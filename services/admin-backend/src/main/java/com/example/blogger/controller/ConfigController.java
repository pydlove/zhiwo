package com.example.blogger.controller;

import com.example.blogger.config.AppProperties;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@RestController
@RequestMapping("/api/configs")
@CrossOrigin(origins = "*")
public class ConfigController {
    private final ConfigMapper configMapper;
    private final DataSource dataSource;
    private final AppProperties appProperties;

    public ConfigController(ConfigMapper configMapper, DataSource dataSource, AppProperties appProperties) {
        this.configMapper = configMapper;
        this.dataSource = dataSource;
        this.appProperties = appProperties;
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

    @GetMapping("/visible-tabs")
    public Result<Map<String, Object>> visibleTabs() {
        Map<String, Object> result = new HashMap<>();
        result.put("processManage", appProperties.getProcessManageVisibleTabs());
        return Result.ok(result);
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

    @PostMapping("/open-account-link")
    public Result<Map<String, String>> generateOpenAccountLink(@RequestBody Map<String, Object> req) {
        String platform = req.get("platform") != null ? req.get("platform").toString() : "";
        Integer count = 3;
        Object countObj = req.get("count");
        if (countObj != null) {
            if (countObj instanceof Number) {
                count = ((Number) countObj).intValue();
            } else {
                try {
                    count = Integer.parseInt(countObj.toString());
                } catch (NumberFormatException e) {
                    count = 3;
                }
            }
        }
        String baseUrl = req.get("baseUrl") != null ? req.get("baseUrl").toString() : "http://www.mmshuo.tech";
        String membershipPlanId = req.get("membershipPlanId") != null ? req.get("membershipPlanId").toString() : "";
        String adminId = req.get("adminId") != null ? req.get("adminId").toString() : "";

        if (count == null || count < 1) count = 3;
        if (count > 20) count = 20;

        String code = generateShortCode(8);
        Map<String, Object> data = new HashMap<>();
        data.put("platform", platform);
        data.put("count", count);
        if (membershipPlanId != null && !membershipPlanId.isEmpty()) {
            data.put("membershipPlanId", membershipPlanId);
        }
        if (adminId != null && !adminId.isEmpty()) {
            data.put("adminId", adminId);
        }
        data.put("createdAt", LocalDateTime.now().toString());

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(data);
            Config c = new Config();
            c.setConfigKey("oa_link_" + code);
            c.setConfigValue(json);
            configMapper.save(c);
        } catch (Exception e) {
            return Result.error("生成链接失败: " + e.getMessage());
        }

        String url = baseUrl + "/open-account?c=" + code;
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("code", code);
        return Result.ok(result);
    }

    @PostMapping("/operator-promo-link")
    public Result<Map<String, String>> generateOperatorPromoLink(@RequestBody Map<String, Object> req) {
        String adminId = req.get("adminId") != null ? req.get("adminId").toString() : "";
        String username = req.get("username") != null ? req.get("username").toString() : "";
        String baseUrl = req.get("baseUrl") != null ? req.get("baseUrl").toString() : "http://www.mmshuo.tech";
        String targetPath = req.get("targetPath") != null ? req.get("targetPath").toString() : "/login";

        if (adminId.isEmpty() || username.isEmpty()) {
            return Result.error("运营者信息不能为空");
        }

        // 检查是否已有该运营者的短链（按 adminId 查），有则复用
        String existingCode = findExistingOperatorPromoCode(adminId);
        if (existingCode != null) {
            String url = baseUrl + "/s/" + existingCode;
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("code", existingCode);
            return Result.ok(result);
        }

        String code = generateShortCode(6);
        Map<String, Object> data = new HashMap<>();
        data.put("adminId", adminId);
        data.put("username", username);
        data.put("baseUrl", baseUrl);
        data.put("targetPath", targetPath);
        data.put("createdAt", LocalDateTime.now().toString());

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(data);
            Config c = new Config();
            c.setConfigKey("op_link_" + code);
            c.setConfigValue(json);
            configMapper.save(c);
        } catch (Exception e) {
            return Result.error("生成短链失败: " + e.getMessage());
        }

        String url = baseUrl + "/s/" + code;
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("code", code);
        return Result.ok(result);
    }

    private String findExistingOperatorPromoCode(String adminId) {
        List<Config> all = configMapper.findAll();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (Config cfg : all) {
            if (cfg.getConfigKey() == null || !cfg.getConfigKey().startsWith("op_link_")) continue;
            try {
                Map<String, Object> data = mapper.readValue(cfg.getConfigValue(), Map.class);
                if (adminId.equals(data.get("adminId"))) {
                    return cfg.getConfigKey().substring("op_link_".length());
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    private String generateShortCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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
        w.println("\n-- ----------------------------");
        w.println("-- Table " + tableName);
        w.println("-- ----------------------------");

        // 1. 导出表结构
        writeCreateTable(w, conn, tableName);

        // 2. 导出数据
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + escape(tableName))) {

            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();

            StringBuilder cols = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) cols.append(", ");
                cols.append("`").append(rsmd.getColumnLabel(i)).append("`");
            }

            int rowCount = 0;
            while (rs.next()) {
                StringBuilder vals = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) vals.append(", ");
                    Object v = rs.getObject(i);
                    vals.append(toSqlValue(v));
                }
                w.println("INSERT INTO `" + tableName + "` (" + cols + ") VALUES (" + vals + ");");
                rowCount++;
            }
            if (rowCount == 0) {
                w.println("-- (empty table)");
            } else {
                w.println("-- " + rowCount + " rows");
            }
        }
    }

    private void writeCreateTable(PrintWriter w, Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();

        // 收集主键列
        Set<String> pkCols = new HashSet<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
            while (pkRs.next()) {
                pkCols.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DROP TABLE IF EXISTS `").append(tableName).append("`;\n");
        sb.append("CREATE TABLE `").append(tableName).append("` (\n");

        boolean first = true;
        try (ResultSet colRs = meta.getColumns(conn.getCatalog(), null, tableName, "%")) {
            while (colRs.next()) {
                if (!first) sb.append(",\n");
                first = false;

                String colName = colRs.getString("COLUMN_NAME");
                String typeName = colRs.getString("TYPE_NAME");
                int dataType = colRs.getInt("DATA_TYPE");
                int colSize = colRs.getInt("COLUMN_SIZE");
                int decimalDigits = colRs.getInt("DECIMAL_DIGITS");
                int nullable = colRs.getInt("NULLABLE");
                String defaultVal = colRs.getString("COLUMN_DEF");
                String remarks = colRs.getString("REMARKS");
                boolean isAutoInc = "YES".equalsIgnoreCase(colRs.getString("IS_AUTOINCREMENT"));

                sb.append("  `").append(colName).append("` ");

                // 类型映射
                sb.append(mapColumnType(dataType, typeName, colSize, decimalDigits));

                if (nullable == DatabaseMetaData.columnNoNulls) {
                    sb.append(" NOT NULL");
                }
                if (isAutoInc) {
                    sb.append(" AUTO_INCREMENT");
                }
                if (defaultVal != null) {
                    sb.append(" DEFAULT ").append(toSqlDefault(defaultVal, dataType));
                }
                if (remarks != null && !remarks.isEmpty()) {
                    sb.append(" COMMENT '").append(remarks.replace("'", "\\'")).append("'");
                }
            }
        }

        // 主键
        if (!pkCols.isEmpty()) {
            sb.append(",\n  PRIMARY KEY (");
            boolean firstPk = true;
            for (String pk : pkCols) {
                if (!firstPk) sb.append(", ");
                firstPk = false;
                sb.append("`").append(pk).append("`");
            }
            sb.append(")");
        }

        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n");
        w.println(sb.toString());
    }

    private String mapColumnType(int dataType, String typeName, int colSize, int decimalDigits) {
        switch (dataType) {
            case Types.BIT:
            case Types.BOOLEAN:
                return "tinyint(1)";
            case Types.TINYINT:
                return "tinyint" + (colSize > 0 ? "(" + colSize + ")" : "");
            case Types.SMALLINT:
                return "smallint" + (colSize > 0 ? "(" + colSize + ")" : "");
            case Types.INTEGER:
                return "int" + (colSize > 0 ? "(" + colSize + ")" : "");
            case Types.BIGINT:
                return "bigint" + (colSize > 0 ? "(" + colSize + ")" : "");
            case Types.FLOAT:
            case Types.REAL:
                return "float";
            case Types.DOUBLE:
                return "double";
            case Types.NUMERIC:
            case Types.DECIMAL:
                if (decimalDigits > 0) {
                    return "decimal(" + colSize + "," + decimalDigits + ")";
                }
                return "decimal" + (colSize > 0 ? "(" + colSize + ")" : "");
            case Types.CHAR:
                return "char(" + colSize + ")";
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
                if (colSize >= 65535 || colSize <= 0) {
                    return "text";
                }
                return "varchar(" + colSize + ")";
            case Types.DATE:
                return "date";
            case Types.TIME:
                return "time";
            case Types.TIMESTAMP:
                return "datetime";
            case Types.BLOB:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return "blob";
            case Types.CLOB:
            case Types.NCLOB:
            case Types.LONGNVARCHAR:
                return "longtext";
            case Types.OTHER:
                if ("json".equalsIgnoreCase(typeName)) {
                    return "json";
                }
                return typeName != null ? typeName.toLowerCase() : "text";
            default:
                if (typeName != null) {
                    String tn = typeName.toLowerCase();
                    if (tn.contains("datetime") || tn.contains("timestamp")) {
                        return tn;
                    }
                    if (tn.contains("text") || tn.contains("blob")) {
                        return tn;
                    }
                    if (colSize > 0) {
                        return tn + "(" + colSize + ")";
                    }
                    return tn;
                }
                return "text";
        }
    }

    private String toSqlDefault(String defaultVal, int dataType) {
        if (defaultVal == null) return "NULL";
        // 处理 MySQL 的特殊默认值，如 CURRENT_TIMESTAMP
        String upper = defaultVal.toUpperCase();
        if (upper.contains("CURRENT_TIMESTAMP")) {
            return defaultVal;
        }
        if (upper.equals("NULL")) {
            return "NULL";
        }
        if (dataType == Types.BIT || dataType == Types.BOOLEAN ||
            dataType == Types.TINYINT || dataType == Types.SMALLINT ||
            dataType == Types.INTEGER || dataType == Types.BIGINT ||
            dataType == Types.FLOAT || dataType == Types.DOUBLE ||
            dataType == Types.NUMERIC || dataType == Types.DECIMAL) {
            return defaultVal;
        }
        return "'" + defaultVal.replace("'", "\\'") + "'";
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
