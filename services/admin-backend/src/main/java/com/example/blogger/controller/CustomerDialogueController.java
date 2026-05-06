package com.example.blogger.controller;

import com.example.blogger.entity.CustomerDialogue;
import com.example.blogger.entity.Result;
import com.example.blogger.service.CustomerDialogueService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/customer-dialogues")
@CrossOrigin(origins = "*")
public class CustomerDialogueController {

    private final CustomerDialogueService customerDialogueService;

    public CustomerDialogueController(CustomerDialogueService customerDialogueService) {
        this.customerDialogueService = customerDialogueService;
    }

    @GetMapping
    public Result<List<CustomerDialogue>> list(@RequestParam(value = "category", required = false) String category,
                                                @RequestParam(value = "adminId", required = false) String adminId) {
        if (adminId != null && !adminId.isEmpty()) {
            // 运营管理员：只返回自己的话术
            if (category != null && !category.isEmpty()) {
                return Result.ok(customerDialogueService.listByCategoryAndAdminId(category, adminId));
            }
            return Result.ok(customerDialogueService.listByAdminId(adminId));
        }
        // 超级管理员或默认：返回全部
        if (category != null && !category.isEmpty()) {
            return Result.ok(customerDialogueService.listByCategory(category));
        }
        return Result.ok(customerDialogueService.list());
    }

    @GetMapping("/categories")
    public Result<List<String>> categories(@RequestParam(value = "adminId", required = false) String adminId) {
        if (adminId != null && !adminId.isEmpty()) {
            return Result.ok(customerDialogueService.listCategoriesByAdminId(adminId));
        }
        return Result.ok(customerDialogueService.listCategories());
    }

    @GetMapping("/{id}")
    public Result<CustomerDialogue> get(@PathVariable String id) {
        return Result.ok(customerDialogueService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody CustomerDialogue customerDialogue) {
        customerDialogueService.save(customerDialogue);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        customerDialogueService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的记录");
        }
        for (String id : ids) {
            customerDialogueService.delete(id);
        }
        return Result.ok(null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return exportToExcel(customerDialogueService.list());
    }

    @PostMapping("/export-selected")
    public ResponseEntity<byte[]> exportSelected(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return exportToExcel(customerDialogueService.listByIds(ids));
    }

    private ResponseEntity<byte[]> exportToExcel(List<CustomerDialogue> list) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("客服对话");
            String[] headers = {"分类", "提问/场景", "回复内容", "图片URL", "排序"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < list.size(); i++) {
                CustomerDialogue item = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(item.getCategory() != null ? item.getCategory() : "");
                row.createCell(1).setCellValue(item.getQuestion() != null ? item.getQuestion() : "");
                row.createCell(2).setCellValue(item.getReply() != null ? item.getReply() : "");
                row.createCell(3).setCellValue(item.getImageUrl() != null ? item.getImageUrl() : "");
                row.createCell(4).setCellValue(item.getSortOrder() != null ? item.getSortOrder() : 0);
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            sheet.setColumnWidth(2, 60 * 256);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] body = out.toByteArray();

            String fileName = "customer_dialogues_" + System.currentTimeMillis() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请上传文件");
        }
        try (InputStream is = file.getInputStream();
             Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int success = 0;
            int failed = 0;
            List<String> errors = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String category = getCellString(row.getCell(0));
                    String question = getCellString(row.getCell(1));
                    String reply = getCellString(row.getCell(2));
                    String imageUrl = getCellString(row.getCell(3));
                    int sortOrder = 0;
                    Cell sortCell = row.getCell(4);
                    if (sortCell != null) {
                        if (sortCell.getCellType() == CellType.NUMERIC) {
                            sortOrder = (int) sortCell.getNumericCellValue();
                        } else {
                            try {
                                sortOrder = Integer.parseInt(getCellString(sortCell));
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    if (category == null || category.isEmpty() || question == null || question.isEmpty() || reply == null || reply.isEmpty()) {
                        failed++;
                        errors.add("第" + (i + 1) + "行：分类、提问、回复不能为空");
                        continue;
                    }

                    CustomerDialogue cd = new CustomerDialogue();
                    cd.setCategory(category);
                    cd.setQuestion(question);
                    cd.setReply(reply);
                    cd.setImageUrl(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : null);
                    cd.setSortOrder(sortOrder);
                    cd.setCreatedAt(LocalDateTime.now());
                    cd.setUpdatedAt(LocalDateTime.now());
                    customerDialogueService.save(cd);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("第" + (i + 1) + "行：" + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("failed", failed);
            result.put("errors", errors);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String val = cell.getStringCellValue();
                return val != null ? val.trim() : null;
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}
