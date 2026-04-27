package com.example.blogger.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageDownloadService {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
    private static final String POLLINATIONS_URL = "https://image.pollinations.ai/prompt/";

    private final RestClient restClient;

    public ImageDownloadService() {
        this.restClient = RestClient.builder()
                .build();
    }

    /**
     * 根据图片描述生成/下载 16:9 配图
     *
     * @param description 图片描述（中文或英文）
     * @return 图片访问 URL，如 /uploads/xxx.jpg
     */
    public String downloadImage(String description) {
        if (description == null || description.isBlank()) {
            description = "beautiful scenery";
        }

        try {
            String encodedPrompt = URLEncoder.encode(description, StandardCharsets.UTF_8);
            int seed = (int) (Math.random() * 100000);
            String imageUrl = POLLINATIONS_URL + encodedPrompt
                    + "?width=1600&height=900&nologo=true&seed=" + seed;

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = "guide_img_" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
            Path filePath = uploadPath.resolve(fileName);

            byte[] imageBytes = restClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .body(byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("下载图片失败: 空响应");
            }

            Files.write(filePath, imageBytes);
            return "/uploads/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("保存图片失败", e);
        } catch (Exception e) {
            throw new RuntimeException("下载图片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量下载图片
     */
    public java.util.List<String> downloadImages(java.util.List<String> descriptions) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (String desc : descriptions) {
            try {
                String url = downloadImage(desc);
                urls.add(url);
                // 稍微延迟，避免请求过快
                Thread.sleep(800);
            } catch (Exception e) {
                // 单张图片失败不影响整体
                System.err.println("图片下载失败: " + desc + " - " + e.getMessage());
            }
        }
        return urls;
    }
}
