package com.example.blogger.controller;

import com.example.blogger.entity.ImageLibrary;
import com.example.blogger.entity.Result;
import com.example.blogger.mapper.ImageLibraryMapper;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageLibraryController {

    private static final Logger log = LoggerFactory.getLogger(ImageLibraryController.class);
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/images/";

    private final ImageLibraryMapper imageLibraryMapper;

    public ImageLibraryController(ImageLibraryMapper imageLibraryMapper) {
        this.imageLibraryMapper = imageLibraryMapper;
    }

    @GetMapping
    public Result<List<ImageLibrary>> list(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword) {
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
        return Result.ok(imageLibraryMapper.findAll(categoryList, keyword));
    }

    @PostMapping("/upload")
    public Result<List<ImageLibrary>> upload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "categories", required = false) String categories) {
        if (files == null || files.length == 0) {
            return Result.error("请选择文件");
        }
        List<ImageLibrary> results = new java.util.ArrayList<>();
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
                String ext = "";
                if (originalName != null && originalName.contains(".")) {
                    ext = originalName.substring(originalName.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
                Path filePath = uploadPath.resolve(fileName);
                byte[] compressedBytes = compressImage(file);
                Files.write(filePath, compressedBytes);

                ImageLibrary image = new ImageLibrary();
                image.setId(UUID.randomUUID().toString().replace("-", ""));
                image.setName(originalName != null ? originalName : fileName);
                image.setUrl("/uploads/images/" + fileName);
                image.setCategories(categories);
                imageLibraryMapper.insert(image);
                results.add(image);
                log.info("[ImageLibrary] 上传图片: id={}, name={}", image.getId(), image.getName());
            } catch (IOException e) {
                log.error("[ImageLibrary] 上传失败: {}", e.getMessage(), e);
            }
        }
        if (results.isEmpty()) {
            return Result.error("没有成功上传的图片文件");
        }
        return Result.ok(results);
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            return file.getBytes();
        }

        int maxWidth = 1920;
        int maxHeight = 1080;
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        double scale = 1.0;
        if (width > maxWidth || height > maxHeight) {
            double scaleX = (double) maxWidth / width;
            double scaleY = (double) maxHeight / height;
            scale = Math.min(scaleX, scaleY);
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
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();
        }

        String contentType = file.getContentType();
        boolean isJpeg = contentType != null && (contentType.contains("jpeg") || contentType.contains("jpg"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (isJpeg) {
            compressJpeg(targetImage, baos, 0.85f);
        } else {
            String formatName = contentType != null && contentType.contains("png") ? "png" : "png";
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
}
