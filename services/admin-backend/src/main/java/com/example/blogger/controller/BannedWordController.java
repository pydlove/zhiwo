package com.example.blogger.controller;

import com.example.blogger.entity.BannedWord;
import com.example.blogger.entity.Result;
import com.example.blogger.service.BannedWordService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/banned-words")
@CrossOrigin(origins = "*")
public class BannedWordController {

    private final BannedWordService bannedWordService;

    public BannedWordController(BannedWordService bannedWordService) {
        this.bannedWordService = bannedWordService;
    }

    @GetMapping
    public Result<List<BannedWord>> list() {
        return Result.ok(bannedWordService.list());
    }

    @GetMapping("/{id}")
    public Result<BannedWord> get(@PathVariable String id) {
        return Result.ok(bannedWordService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BannedWord bannedWord) {
        bannedWordService.save(bannedWord);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        bannedWordService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        try {
            List<BannedWord> list = bannedWordService.list();
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("违禁词库");

            Row header = sheet.createRow(0);
            String[] cols = {"违禁词", "替换词", "分类", "等级"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
            }

            for (int i = 0; i < list.size(); i++) {
                BannedWord bw = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(bw.getWord() != null ? bw.getWord() : "");
                row.createCell(1).setCellValue(bw.getReplacement() != null ? bw.getReplacement() : "");
                row.createCell(2).setCellValue(bw.getCategory() != null ? bw.getCategory() : "");
                row.createCell(3).setCellValue("block".equals(bw.getSeverity()) ? "严禁" : "慎用");
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close();

            String fileName = "banned_words_export_" + System.currentTimeMillis() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importWords(@RequestParam("excel") MultipartFile excel) {
        try {
            Workbook wb = new XSSFWorkbook(excel.getInputStream());
            Sheet sheet = wb.getSheetAt(0);
            int success = 0;
            int skip = 0;
            List<String> errors = new ArrayList<>();

            // Build existing word map for deduplication
            List<BannedWord> existingList = bannedWordService.list();
            Map<String, BannedWord> wordMap = new HashMap<>();
            for (BannedWord bw : existingList) {
                if (bw.getWord() != null) {
                    wordMap.put(bw.getWord(), bw);
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String word = getCellString(row, 0);
                String replacement = getCellString(row, 1);
                String category = getCellString(row, 2);
                String severityStr = getCellString(row, 3);

                if (word == null || word.isEmpty()) {
                    skip++;
                    continue;
                }

                BannedWord bw = wordMap.get(word);
                if (bw == null) {
                    bw = new BannedWord();
                    bw.setWord(word);
                }
                bw.setReplacement(replacement != null ? replacement : "");
                bw.setCategory(category != null && !category.isEmpty() ? category : "其他");
                String severity = "严禁".equals(severityStr) || "block".equals(severityStr) ? "block" : "caution";
                bw.setSeverity(severity);

                bannedWordService.save(bw);
                wordMap.put(word, bw);
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
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
            }
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }
}
