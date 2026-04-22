package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Blogger;
import com.example.blogger.entity.Track;
import com.example.blogger.service.BloggerService;
import com.example.blogger.mapper.TrackMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

                // 检查重复：平台 + 名称 + 赛道
                Blogger existing = bloggerService.findByNamePlatformTrack(name, platform, track.getId());

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
                if (existing != null) {
                    blogger.setId(existing.getId());
                    blogger.setRankNum(existing.getRankNum());
                    if (avatarUrl == null) {
                        blogger.setAvatar(existing.getAvatar());
                    }
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

    @PostMapping("/parse-export")
    public ResponseEntity<byte[]> parseAndExport(
            @RequestParam String text,
            @RequestParam String platform,
            @RequestParam String trackId,
            @RequestParam(required = false) String avatarUrls) {
        try {
            Track track = trackMapper.findById(trackId);
            String trackName = track != null ? track.getName() : "";

            List<Map<String, String>> items = parseBloggerText(text);
            List<String> avatarList = parseAvatarUrls(avatarUrls);

            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("博主导入模板");

            Row header = sheet.createRow(0);
            String[] cols = {"name", "tagline", "platform", "track", "link", "avatarFileName"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
            }

            for (int i = 0; i < items.size(); i++) {
                Map<String, String> item = items.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(item.get("name"));
                row.createCell(1).setCellValue(item.get("tagline"));
                row.createCell(2).setCellValue(platform);
                row.createCell(3).setCellValue(trackName);
                row.createCell(4).setCellValue("");
                String avatar = i < avatarList.size() ? avatarList.get(i) : "";
                row.createCell(5).setCellValue(avatar);
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close();

            String fileName = "blogger_export_" + System.currentTimeMillis() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(out.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<String> parseAvatarUrls(String avatarUrls) {
        List<String> list = new ArrayList<>();
        if (avatarUrls == null || avatarUrls.isEmpty()) {
            return list;
        }
        String[] lines = avatarUrls.split("\\r?\\n");
        java.util.regex.Pattern mdImgPattern = java.util.regex.Pattern.compile("!\\[.*?\\]\\((.*?)(?:\\s+\".*?\")?\\)");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            java.util.regex.Matcher m = mdImgPattern.matcher(line);
            if (m.find()) {
                list.add(m.group(1).trim());
            } else {
                list.add(line);
            }
        }
        return list;
    }

    private List<Map<String, String>> parseBloggerText(String text) {
        List<Map<String, String>> list = new ArrayList<>();
        // 先尝试按空行分隔段落（名称一行 + 描述一行或多行）
        String[] paragraphs = text.split("\\n\\s*\\n");
        if (paragraphs.length > 1) {
            for (String para : paragraphs) {
                para = para.trim();
                if (para.isEmpty()) continue;
                String[] lines = para.split("\\r?\\n");
                if (lines.length >= 2) {
                    String name = lines[0].trim();
                    StringBuilder desc = new StringBuilder();
                    for (int i = 1; i < lines.length; i++) {
                        if (desc.length() > 0) desc.append(" ");
                        desc.append(lines[i].trim());
                    }
                    Map<String, String> map = new HashMap<>();
                    map.put("name", name);
                    map.put("tagline", desc.toString());
                    list.add(map);
                } else if (lines.length == 1) {
                    parseSingleLine(lines[0].trim(), list);
                }
            }
            return list;
        }
        // 回退：按行处理，每行内部找分隔符
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            parseSingleLine(line.trim(), list);
        }
        return list;
    }

    private void parseSingleLine(String line, List<Map<String, String>> list) {
        if (line == null || line.isEmpty()) return;
        String name = line;
        String desc = "";
        String[] delimiters = {"：", ":", "\t"};
        for (String delim : delimiters) {
            int idx = line.indexOf(delim);
            if (idx > 0 && idx < line.length() - 1) {
                name = line.substring(0, idx).trim();
                desc = line.substring(idx + delim.length()).trim();
                break;
            }
        }
        if (desc.isEmpty()) {
            int spaceIdx = line.indexOf(' ');
            if (spaceIdx > 0 && spaceIdx < 20 && spaceIdx < line.length() - 1) {
                name = line.substring(0, spaceIdx).trim();
                desc = line.substring(spaceIdx + 1).trim();
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("tagline", desc);
        list.add(map);
    }
}
