package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Blogger;
import com.example.blogger.entity.Track;
import com.example.blogger.service.BloggerService;
import com.example.blogger.mapper.TrackMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/bloggers")
@CrossOrigin(origins = "*")
public class BloggerController {
    private final BloggerService bloggerService;
    private final TrackMapper trackMapper;
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    public BloggerController(BloggerService bloggerService, TrackMapper trackMapper) {
        this.bloggerService = bloggerService;
        this.trackMapper = trackMapper;
    }

    @GetMapping
    public Result<List<Blogger>> list(@RequestParam(required = false) String trackId) {
        if (trackId != null && !trackId.isEmpty()) {
            return Result.ok(bloggerService.listByTrack(trackId));
        }
        return Result.ok(bloggerService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Blogger> get(@PathVariable String id) {
        return Result.ok(bloggerService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Blogger blogger) {
        bloggerService.save(blogger);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Blogger blogger) {
        blogger.setId(id);
        bloggerService.save(blogger);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        bloggerService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importBloggers(
            @RequestParam("excel") MultipartFile excel,
            @RequestParam(value = "zip", required = false) MultipartFile zip) {
        Path tempDir = null;
        Map<String, String> imageMap = new HashMap<>();

        try {
            // 1. 解压 ZIP 到临时目录
            if (zip != null && !zip.isEmpty()) {
                tempDir = Files.createTempDirectory("blogger_import_");
                Path zipPath = tempDir.resolve("images.zip");
                Files.copy(zip.getInputStream(), zipPath);
                unzip(zipPath, tempDir);
                // 收集所有图片文件
                Files.walk(tempDir).forEach(p -> {
                    if (Files.isRegularFile(p)) {
                        String name = p.getFileName().toString().toLowerCase();
                        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                            imageMap.put(p.getFileName().toString(), p.toString());
                        }
                    }
                });
            }

            // 2. 读取 Excel
            Workbook wb = new XSSFWorkbook(excel.getInputStream());
            Sheet sheet = wb.getSheetAt(0);
            List<String> errors = new ArrayList<>();
            int success = 0;
            int skip = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellString(row, 0);
                String tagline = getCellString(row, 1);
                String platform = getCellString(row, 2);
                String trackName = getCellString(row, 3);
                String link = getCellString(row, 4);
                String avatarFileName = getCellString(row, 5);

                if (name == null || name.isEmpty()) {
                    skip++;
                    continue;
                }

                // 查找赛道
                Track track = trackMapper.findByName(trackName);
                if (track == null) {
                    errors.add("第" + (i + 1) + "行：赛道「" + trackName + "」不存在");
                    continue;
                }

                // 处理头像
                String avatarUrl = null;
                if (avatarFileName != null && !avatarFileName.isEmpty()) {
                    if (avatarFileName.startsWith("http://") || avatarFileName.startsWith("https://") || avatarFileName.startsWith("data:")) {
                        avatarUrl = avatarFileName;
                    } else {
                        String imagePath = imageMap.get(avatarFileName);
                        if (imagePath == null) {
                            imagePath = imageMap.get(avatarFileName.toLowerCase());
                        }
                        if (imagePath != null) {
                            avatarUrl = copyToUploads(Paths.get(imagePath));
                        }
                    }
                }

                Blogger blogger = new Blogger();
                blogger.setName(name);
                blogger.setTagline(tagline);
                blogger.setPlatform(platform);
                blogger.setTrackId(track.getId());
                blogger.setLink(link);
                if (avatarUrl != null) {
                    blogger.setAvatar(avatarUrl);
                }
                bloggerService.save(blogger);
                success++;
            }
            wb.close();

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("skip", skip);
            result.put("errors", errors);
            return Result.ok(result);

        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        } finally {
            if (tempDir != null) {
                try {
                    deleteDir(tempDir.toFile());
                } catch (Exception ignored) {}
            }
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            String v = cell.getStringCellValue();
            return v != null ? v.trim() : null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }

    private String copyToUploads(Path source) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String ext = "";
        String fileName = source.getFileName().toString();
        if (fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf("."));
        }
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = uploadPath.resolve(newName);
        Files.copy(source, target);
        return "/uploads/" + newName;
    }

    private void unzip(Path zipPath, Path destDir) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zipPath))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                Path entryPath = destDir.resolve(entry.getName());
                Files.createDirectories(entryPath.getParent());
                Files.copy(zis, entryPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
        }
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
