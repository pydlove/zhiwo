package com.example.blogger.controller;

import com.example.blogger.entity.ImageLibrary;
import com.example.blogger.entity.Result;
import com.example.blogger.entity.Track;
import com.example.blogger.mapper.ImageLibraryMapper;
import com.example.blogger.mapper.TrackMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageLibraryController {

    private static final Logger log = LoggerFactory.getLogger(ImageLibraryController.class);
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/images/";

    private final ImageLibraryMapper imageLibraryMapper;
    private final TrackMapper trackMapper;

    public ImageLibraryController(ImageLibraryMapper imageLibraryMapper, TrackMapper trackMapper) {
        this.imageLibraryMapper = imageLibraryMapper;
        this.trackMapper = trackMapper;
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        List<String> categoryList = null;
        if (category != null && !category.isEmpty()) {
            try {
                if (category.trim().startsWith("[")) {
                    categoryList = new ObjectMapper().readValue(category, new TypeReference<List<String>>() {});
                } else {
                    categoryList = Collections.singletonList(category);
                }
            } catch (Exception e) {
                categoryList = Collections.singletonList(category);
            }
        }
        int offset = Math.max(0, page - 1) * pageSize;
        List<ImageLibrary> list = imageLibraryMapper.findAll(categoryList, keyword, pageSize, offset);
        int total = imageLibraryMapper.countAll(categoryList, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return Result.ok(result);
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "categories", required = false) String categories) {
        if (files == null || files.length == 0) {
            return Result.error("请选择文件");
        }
        List<ImageLibrary> results = new java.util.ArrayList<>();
        List<String> skippedNames = new java.util.ArrayList<>();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            log.error("[ImageLibrary] 创建上传目录失败: {}", e.getMessage(), e);
            return Result.error("创建上传目录失败: " + e.getMessage());
        }
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("[ImageLibrary] 跳过非图片文件: {}", file.getOriginalFilename());
                continue;
            }
            try {
                String originalName = file.getOriginalFilename();
                String name = originalName != null ? originalName : "";
                // 根据文件名去重：已存在则跳过
                ImageLibrary existing = imageLibraryMapper.findByName(name);
                if (existing != null) {
                    skippedNames.add(name);
                    log.info("[ImageLibrary] 文件名已存在，跳过: {}", name);
                    continue;
                }
                String ext = "";
                if (name.contains(".")) {
                    ext = name.substring(name.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
                Path filePath = uploadPath.resolve(fileName);
                byte[] compressedBytes = compressImage(file);
                Files.write(filePath, compressedBytes);

                ImageLibrary image = new ImageLibrary();
                image.setId(UUID.randomUUID().toString().replace("-", ""));
                image.setName(name);
                image.setUrl("/uploads/images/" + fileName);
                image.setCategories(categories);
                imageLibraryMapper.insert(image);
                results.add(image);
                log.info("[ImageLibrary] 上传图片: id={}, name={}", image.getId(), image.getName());
            } catch (IOException e) {
                log.error("[ImageLibrary] 上传失败: {}", e.getMessage(), e);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", results);
        result.put("uploadedCount", results.size());
        result.put("skippedCount", skippedNames.size());
        result.put("skippedNames", skippedNames);
        if (results.isEmpty() && skippedNames.isEmpty()) {
            return Result.error("没有成功上传的图片文件");
        }
        return Result.ok(result);
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        boolean isJpeg = contentType != null && (contentType.contains("jpeg") || contentType.contains("jpg"));
        boolean isPng = contentType != null && contentType.contains("png");

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            return file.getBytes();
        }

        int maxWidth = 2560;
        int maxHeight = 4096;
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        double scale = 1.0;
        if (width > maxWidth || height > maxHeight) {
            double scaleX = (double) maxWidth / width;
            double scaleY = (double) maxHeight / height;
            scale = Math.min(scaleX, scaleY);
        }

        // PNG 截图如果不需要缩放，直接返回原文件，避免解码/编码损失锐度
        if (scale >= 1.0 && isPng) {
            return file.getBytes();
        }

        BufferedImage targetImage = originalImage;
        if (scale < 1.0) {
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);
            int imageType = originalImage.getType();
            if (imageType == BufferedImage.TYPE_CUSTOM) {
                imageType = originalImage.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB;
            }
            targetImage = new BufferedImage(newWidth, newHeight, imageType);
            Graphics2D g = targetImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (isJpeg) {
            compressJpeg(targetImage, baos, 0.92f);
        } else {
            String formatName = isPng ? "png" : "png";
            ImageIO.write(targetImage, formatName, baos);
        }
        return baos.toByteArray();
    }

    private void compressJpeg(BufferedImage image, OutputStream out, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpeg", out);
            return;
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    @PostMapping("/{id}/update")
    public Result<ImageLibrary> updateImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categories", required = false) String categories) {
        ImageLibrary existing = imageLibraryMapper.findById(id);
        if (existing == null) {
            return Result.error("图片不存在");
        }
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持上传图片文件");
        }
        try {
            // 删除旧文件
            if (existing.getUrl() != null) {
                String relativePath = existing.getUrl().replaceFirst("^/uploads/", "");
                Path oldFilePath = Paths.get(System.getProperty("user.dir") + "/uploads/" + relativePath);
                Files.deleteIfExists(oldFilePath);
            }

            // 保存新文件（带压缩）
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path filePath = uploadPath.resolve(fileName);
            byte[] compressedBytes = compressImage(file);
            Files.write(filePath, compressedBytes);

            existing.setName(originalName != null ? originalName : fileName);
            existing.setUrl("/uploads/images/" + fileName);
            if (categories != null) {
                existing.setCategories(categories);
            }
            imageLibraryMapper.update(existing);
            log.info("[ImageLibrary] 更新图片: id={}, name={}", existing.getId(), existing.getName());
            return Result.ok(existing);
        } catch (IOException e) {
            log.error("[ImageLibrary] 更新图片失败: {}", e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        ImageLibrary image = imageLibraryMapper.findById(id);
        if (image != null && image.getUrl() != null) {
            try {
                String relativePath = image.getUrl().replaceFirst("^/uploads/", "");
                Path filePath = Paths.get(System.getProperty("user.dir") + "/uploads/" + relativePath);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("[ImageLibrary] 删除物理文件失败: {}", e.getMessage());
            }
        }
        imageLibraryMapper.delete(id);
        log.info("[ImageLibrary] 删除图片: id={}", id);
        return Result.ok(null);
    }

    @PostMapping("/download")
    public Result<Map<String, Object>> downloadImages(
            @RequestParam("source") String source,
            @RequestParam("count") int count,
            @RequestParam("trackId") String trackId) {
        Track track = trackMapper.findById(trackId);
        if (track == null) {
            return Result.error("赛道不存在");
        }

        List<String> validSources = Arrays.asList("picsum", "unsplash", "mixed", "bing", "baidu");
        if (!validSources.contains(source)) {
            return Result.error("不支持的图片来源");
        }
        if (count <= 0 || count > 50) {
            return Result.error("数量必须在 1-50 之间");
        }

        String category = track.getName();
        String keyword = track.getPlatforms() != null
                ? track.getName() + " " + track.getPlatforms()
                : track.getName();

        String tempDirName = UUID.randomUUID().toString().replace("-", "");
        Path tempOutputDir = Paths.get(System.getProperty("user.dir"), "uploads", "temp_downloads", tempDirName);
        try {
            Files.createDirectories(tempOutputDir);
        } catch (IOException e) {
            log.error("[ImageLibrary] 创建临时下载目录失败: {}", e.getMessage(), e);
            return Result.error("创建下载目录失败");
        }

        String scriptPath = resolveScriptPath();
        if (scriptPath == null) {
            return Result.error("找不到下载脚本");
        }

        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add(scriptPath);
        command.add(tempOutputDir.toString());
        command.add("--count");
        command.add(String.valueOf(count));
        command.add("--source");
        command.add(source);
        command.add("--category");
        command.add(category);
        command.add("--keyword");
        command.add(keyword);

        log.info("[ImageLibrary] 执行下载脚本: {}", String.join(" ", command));

        int exitCode;
        String stdout;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Result.error("下载超时");
            }
            exitCode = process.exitValue();
            stdout = output.toString();

            if (exitCode != 0) {
                log.error("[ImageLibrary] 下载脚本失败，exitCode={}, stdout={}", exitCode, stdout);
                String errMsg = stdout.length() > 200 ? stdout.substring(0, 200) : stdout;
                return Result.error("下载脚本执行失败: " + errMsg);
            }
        } catch (Exception e) {
            log.error("[ImageLibrary] 执行下载脚本异常: {}", e.getMessage(), e);
            return Result.error("下载失败: " + e.getMessage());
        }

        Path categoryDir = tempOutputDir.resolve(category);
        List<ImageLibrary> imported = new ArrayList<>();
        int failCount = 0;

        if (Files.exists(categoryDir) && Files.isDirectory(categoryDir)) {
            try (Stream<Path> paths = Files.list(categoryDir)) {
                List<Path> imageFiles = paths.filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".jpeg")
                            || name.endsWith(".png") || name.endsWith(".webp");
                }).collect(Collectors.toList());

                for (Path file : imageFiles) {
                    String originalName = file.getFileName().toString();
                    try {
                        String ext = "";
                        if (originalName.contains(".")) {
                            ext = originalName.substring(originalName.lastIndexOf("."));
                        } else {
                            ext = ".jpg";
                        }
                        // 生成新名称：来源-时间戳+四位随机数
                        String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
                        int randomNum = (int) (Math.random() * 10000);
                        String newName = source + "-" + timestamp + String.format("%04d", randomNum) + ext;
                        // 极小概率重名检查
                        ImageLibrary existing = imageLibraryMapper.findByName(newName);
                        if (existing != null) {
                            newName = source + "-" + timestamp + String.format("%04d", (int) (Math.random() * 10000)) + ext;
                        }
                        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
                        Path targetPath = Paths.get(UPLOAD_DIR, fileName);
                        Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);

                        ImageLibrary image = new ImageLibrary();
                        image.setId(UUID.randomUUID().toString().replace("-", ""));
                        image.setName(newName);
                        image.setUrl("/uploads/images/" + fileName);
                        image.setCategories(new ObjectMapper().writeValueAsString(
                                Collections.singletonList(trackId)));
                        imageLibraryMapper.insert(image);
                        imported.add(image);
                    } catch (Exception e) {
                        log.error("[ImageLibrary] 导入图片失败: {}", e.getMessage(), e);
                        failCount++;
                    }
                }
            } catch (IOException e) {
                log.error("[ImageLibrary] 扫描下载目录失败: {}", e.getMessage(), e);
            }
        }

        // 清理临时目录
        try {
            deleteDirectory(tempOutputDir);
        } catch (Exception e) {
            log.warn("[ImageLibrary] 清理临时目录失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", imported.size());
        result.put("failedCount", failCount);
        result.put("trackName", category);
        result.put("source", source);

        log.info("[ImageLibrary] 下载导入完成: track={}, source={}, imported={}, failed={}",
                category, source, imported.size(), failCount);
        return Result.ok(result);
    }

    private String resolveScriptPath() {
        Path directPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "py", "download_category_images.py");
        if (Files.exists(directPath)) {
            return directPath.toString();
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("py/download_category_images.py")) {
            if (is != null) {
                Path tempScript = Files.createTempFile("download_category_images", ".py");
                Files.copy(is, tempScript, StandardCopyOption.REPLACE_EXISTING);
                tempScript.toFile().deleteOnExit();
                return tempScript.toString();
            }
        } catch (IOException e) {
            log.error("[ImageLibrary] 从 classpath 读取脚本失败: {}", e.getMessage());
        }
        return null;
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("[ImageLibrary] 删除文件失败: {}", e.getMessage());
                }
            });
        }
    }
}
