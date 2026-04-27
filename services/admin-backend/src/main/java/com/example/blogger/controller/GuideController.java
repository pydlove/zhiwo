package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Guide;
import com.example.blogger.service.GuideService;
import com.example.blogger.service.GuideGenerationService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/guides")
@CrossOrigin(origins = "*")
public class GuideController {
    private final GuideService guideService;
    private final GuideGenerationService guideGenerationService;

    public GuideController(GuideService guideService, GuideGenerationService guideGenerationService) {
        this.guideService = guideService;
        this.guideGenerationService = guideGenerationService;
    }

    @GetMapping
    public Result<List<Guide>> list(@RequestParam(required = false) Integer isRecommended) {
        List<Guide> list = guideService.list();
        if (isRecommended != null) {
            list = list.stream()
                .filter(g -> isRecommended.equals(g.getIsRecommended()))
                .toList();
        }
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<Guide> get(@PathVariable String id) {
        return Result.ok(guideService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Guide guide) {
        if (guide.getIsRecommended() == null) {
            guide.setIsRecommended(0);
        }
        guideService.save(guide);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Guide guide) {
        guide.setId(id);
        if (guide.getIsRecommended() == null) {
            guide.setIsRecommended(0);
        }
        guideService.save(guide);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        guideService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/batch-recommended")
    public Result<Void> batchUpdateRecommended(@RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) req.get("ids");
        Object recObj = req.get("isRecommended");
        if (ids == null || ids.isEmpty() || recObj == null) {
            return Result.error("参数错误");
        }
        int isRecommended = Integer.parseInt(recObj.toString());
        guideService.batchUpdateRecommended(ids, isRecommended);
        return Result.ok(null);
    }

    @PostMapping("/generate")
    public Result<List<Guide>> generate(@RequestBody Map<String, Object> req) {
        String category = req.get("category") != null ? req.get("category").toString() : "";
        int count = 1;
        try {
            count = Integer.parseInt(req.get("count").toString());
        } catch (Exception e) {
            // use default
        }
        if (category == null || category.isBlank()) {
            return Result.error("请选择内容类型");
        }
        try {
            List<Guide> guides = guideGenerationService.generateGuides(category, count);
            return Result.ok(guides);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/export")
    public void export(HttpServletResponse response,
            @RequestParam(value = "guideIds", required = false) List<String> guideIds) {
        try {
            List<Guide> guides;
            if (guideIds != null && !guideIds.isEmpty()) {
                guides = new ArrayList<>();
                for (String id : guideIds) {
                    Guide g = guideService.getById(id);
                    if (g != null) guides.add(g);
                }
            } else {
                guides = guideService.list();
            }

            XSSFWorkbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("创作技巧");
            String[] headers = { "ID", "技巧标题", "分类", "描述", "内容", "链接", "排序", "状态", "是否推荐" };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < guides.size(); i++) {
                Guide g = guides.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(g.getId() != null ? g.getId() : "");
                row.createCell(1).setCellValue(g.getTitle() != null ? g.getTitle() : "");
                row.createCell(2).setCellValue(g.getCategory() != null ? g.getCategory() : "");
                row.createCell(3).setCellValue(g.getDescription() != null ? g.getDescription() : "");
                row.createCell(4).setCellValue(g.getContent() != null ? g.getContent() : "");
                row.createCell(5).setCellValue(g.getLink() != null ? g.getLink() : "");
                row.createCell(6).setCellValue(g.getSortOrder() != null ? g.getSortOrder() : 0);
                row.createCell(7).setCellValue(g.getStatus() != null ? g.getStatus() : "");
                row.createCell(8).setCellValue(g.getIsRecommended() != null && g.getIsRecommended() == 1 ? "1" : "0");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "创作技巧导出_" + timestamp + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
            try (OutputStream out = response.getOutputStream()) {
                wb.write(out);
            }
            wb.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("excel") MultipartFile excel) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int created = 0;
        int updated = 0;
        int skip = 0;

        try {
            String originalName = excel.getOriginalFilename() != null ? excel.getOriginalFilename().toLowerCase() : "";
            List<String[]> rows = new ArrayList<>();

            if (originalName.endsWith(".csv")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(excel.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        rows.add(parseCsvLine(line));
                    }
                }
            } else {
                Workbook wb;
                if (originalName.endsWith(".xls")) {
                    wb = new HSSFWorkbook(excel.getInputStream());
                } else {
                    wb = new XSSFWorkbook(excel.getInputStream());
                }
                Sheet sheet = wb.getSheetAt(0);
                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String[] arr = new String[9];
                    for (int c = 0; c < 9; c++) {
                        arr[c] = getCellString(row, c);
                    }
                    rows.add(arr);
                }
                wb.close();
            }

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row == null) continue;

                String id = row.length > 0 ? row[0] : null;
                String title = row.length > 1 ? row[1] : null;
                String category = row.length > 2 ? row[2] : null;
                String description = row.length > 3 ? row[3] : null;
                String content = row.length > 4 ? row[4] : null;
                String link = row.length > 5 ? row[5] : null;
                String sortOrderStr = row.length > 6 ? row[6] : null;
                String status = row.length > 7 ? row[7] : null;
                String isRecommendedStr = row.length > 8 ? row[8] : null;

                if (title == null || title.isEmpty()) {
                    skip++;
                    continue;
                }

                int sortOrder = 0;
                try {
                    if (sortOrderStr != null && !sortOrderStr.isEmpty()) {
                        sortOrder = (int) Double.parseDouble(sortOrderStr);
                    }
                } catch (Exception ignored) {}

                int isRecommended = 0;
                try {
                    if (isRecommendedStr != null && !isRecommendedStr.isEmpty()) {
                        isRecommended = (int) Double.parseDouble(isRecommendedStr);
                    }
                } catch (Exception ignored) {}

                Guide guide = new Guide();
                guide.setTitle(title);
                guide.setCategory(category);
                guide.setDescription(description);
                guide.setContent(content);
                guide.setLink(link);
                guide.setSortOrder(sortOrder);
                guide.setStatus(status != null && !status.isEmpty() ? status : "已上架");
                guide.setIsRecommended(isRecommended);

                if (id != null && !id.isEmpty()) {
                    Guide existing = guideService.getById(id);
                    if (existing != null) {
                        guide.setId(id);
                        guideService.save(guide);
                        updated++;
                        success++;
                        continue;
                    }
                }

                guideService.save(guide);
                created++;
                success++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("created", created);
            result.put("updated", updated);
            result.put("skip", skip);
            result.put("errors", errors);
            return Result.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
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
        return null;
    }
}
