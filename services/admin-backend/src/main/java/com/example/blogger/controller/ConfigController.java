package com.example.blogger.controller;

import com.example.blogger.entity.Config;
import com.example.blogger.entity.Result;
import com.example.blogger.mapper.ConfigMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configs")
@CrossOrigin(origins = "*")
public class ConfigController {
    private final ConfigMapper configMapper;
    private final DataSource dataSource;

    public ConfigController(ConfigMapper configMapper, DataSource dataSource) {
        this.configMapper = configMapper;
        this.dataSource = dataSource;
    }

    @GetMapping
    public Result<Map<String, String>> list() {
        List<Config> list = configMapper.findAll();
        Map<String, String> map = new HashMap<>();
        for (Config c : list) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return Result.ok(map);
    }

    @PostMapping
    public Result<Void> save(@RequestBody Map<String, String> req) {
        for (Map.Entry<String, String> entry : req.entrySet()) {
            Config c = new Config();
            c.setConfigKey(entry.getKey());
            c.setConfigValue(entry.getValue());
            configMapper.save(c);
        }
        return Result.ok(null);
    }

    @PostMapping("/backup")
    public ResponseEntity<byte[]> backup() {
        try (Connection conn = dataSource.getConnection()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter w = new PrintWriter(out, false, StandardCharsets.UTF_8);

            // 获取所有表名
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet tables = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    // 跳过系统表
                    if (tableName.equals("_schema_version")) continue;

                    // 写入表结构和数据
                    writeTable(w, conn, tableName);
                }
            }

            w.flush();
            byte[] body = out.toByteArray();

            String fileName = "db_backup_" + System.currentTimeMillis() + ".sql";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void writeTable(PrintWriter w, Connection conn, String tableName) throws SQLException {
        // 跳过包含 is_deleted 的逻辑删除表空数据（保留结构）
        w.println("\n-- ----------------------------");
        w.println("-- Table " + tableName);
        w.println("-- ----------------------------");

        // 生成 CREATE TABLE（简化版，只导出数据）
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + escape(tableName))) {

            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();

            // 列名
            StringBuilder cols = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) cols.append(", ");
                cols.append("`").append(rsmd.getColumnLabel(i)).append("`");
            }

            while (rs.next()) {
                StringBuilder vals = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) vals.append(", ");
                    Object v = rs.getObject(i);
                    vals.append(toSqlValue(v));
                }
                w.println("INSERT INTO `" + tableName + "` (" + cols + ") VALUES (" + vals + ");");
            }
        }

        // 空表也保留注释
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + escape(tableName))) {
            rs.next();
            int count = rs.getInt(1);
            if (count == 0) {
                w.println("-- (empty table)");
            } else {
                w.println("-- " + count + " rows");
            }
        }
    }

    private String escape(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    private String toSqlValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number) return v.toString();
        if (v instanceof Boolean) return ((Boolean) v) ? "1" : "0";
        if (v instanceof java.sql.Date || v instanceof Timestamp || v instanceof Time) {
            return "'" + v.toString() + "'";
        }
        if (v instanceof java.util.Date) {
            return "'" + new Timestamp(((java.util.Date) v).getTime()).toString() + "'";
        }
        if (v instanceof byte[]) {
            java.util.Base64.Encoder enc = java.util.Base64.getEncoder();
            return "X'" + enc.encodeToString((byte[]) v) + "'";
        }
        String s = v.toString();
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "'";
    }
}
