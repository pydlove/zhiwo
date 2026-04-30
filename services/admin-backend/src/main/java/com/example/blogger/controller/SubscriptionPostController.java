package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.SubscriptionPost;
import com.example.blogger.service.SubscriptionPostService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription-posts")
@CrossOrigin(origins = "*")
public class SubscriptionPostController {
    private final SubscriptionPostService service;

    public SubscriptionPostController(SubscriptionPostService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<SubscriptionPost>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String trackId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("userId", userId);
        params.put("trackId", trackId);
        params.put("status", status);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        return Result.ok(service.listByCondition(params));
    }

    @GetMapping("/{id}")
    public Result<SubscriptionPost> get(@PathVariable String id) {
        return Result.ok(service.getById(id));
    }

    @GetMapping("/user/{userId}")
    public Result<List<SubscriptionPost>> listByUser(@PathVariable String userId) {
        return Result.ok(service.listByUserId(userId));
    }

    @GetMapping("/latest")
    public Result<SubscriptionPost> getLatest(
            @RequestParam String userId,
            @RequestParam String trackId) {
        return Result.ok(service.getLatestByUserAndTrack(userId, trackId));
    }

    @PostMapping
    public Result<Void> save(@RequestBody SubscriptionPost p) {
        service.save(p);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody SubscriptionPost p) {
        p.setId(id);
        service.save(p);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.ok(null);
    }

    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".md")) return "text/markdown";
        return "application/octet-stream";
    }

    @GetMapping("/{id}/file")
    public void downloadFile(@PathVariable String id,
                             @RequestParam(value = "download", required = false) Boolean download,
                             HttpServletResponse response) throws IOException {
        SubscriptionPost post = service.getById(id);
        if (post == null || post.getFileUrl() == null || post.getFileUrl().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        String fileUrl = post.getFileUrl();
        String filePath = fileUrl.startsWith("/")
                ? System.getProperty("user.dir") + fileUrl
                : fileUrl;

        File file = new File(filePath);
        if (!file.exists()) {
            // Try resolving from uploads directory
            String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
            file = new File(articlesDir + File.separator + post.getFileName());
        }

        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        // Determine content type by extension (more reliable than Files.probeContentType on Linux)
        String fileName = post.getFileName();
        String contentType = getContentType(fileName);
        response.setContentType(contentType);

        // Set disposition
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        if (Boolean.TRUE.equals(download)) {
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        } else {
            response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedName);
        }

        // Stream file content
        try (InputStream is = new BufferedInputStream(new FileInputStream(file));
             OutputStream os = new BufferedOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }
    }
}
