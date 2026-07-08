package com.aisearch.migration;

import java.sql.*;
import java.util.*;

/**
 * 数据库迁移工具：将远程数据库中的 aiask* 和 aisearch_manager schema 迁移到本地数据库。
 * 建表使用预定义 DDL（用户提供），数据逐行 INSERT。
 */
public class DatabaseMigration {

    // ======================== 连接配置 ========================
    private static final String REMOTE_URL = "jdbc:mysql://111.231.63.161:3308?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
    private static final String REMOTE_USER = "system";
    private static final String REMOTE_PWD = "CHANGEME";

    private static final String LOCAL_URL = "jdbc:mysql://127.0.0.1:3307?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
    private static final String LOCAL_USER = "system";
    private static final String LOCAL_PWD = "CHANGEME";

    // ======================== aiask* schema 建表 DDL ========================
    private static final String[] AIASK_CREATE_TABLE_DDLS = {
        "CREATE TABLE KGFILE (name VARCHAR(256) NOT NULL, PRIMARY KEY (name))",

        "CREATE TABLE KGCOMMUNITY (" +
        "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "    name VARCHAR(128) NOT NULL," +
        "    summary TEXT" +
        ")",

        "CREATE TABLE KGENTITY (" +
        "    name VARCHAR(128) NOT NULL," +
        "    type VARCHAR(64)," +
        "    description TEXT," +
        "    file_name VARCHAR(256)," +
        "    PRIMARY KEY (name)" +
        ")",

        "CREATE TABLE KGSEGMENT (" +
        "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "    segment TEXT," +
        "    file_name VARCHAR(256)" +
        ")",

        "CREATE TABLE KGRELATIONSHIP (" +
        "    source VARCHAR(128) NOT NULL," +
        "    target VARCHAR(128) NOT NULL," +
        "    relation VARCHAR(32) NOT NULL," +
        "    description TEXT," +
        "    file_name VARCHAR(256)," +
        "    PRIMARY KEY (source, target, relation)" +
        ")",

        "CREATE TABLE KGIMAGE (" +
        "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "    content BLOB NOT NULL," +
        "    description TEXT" +
        ")"
    };

    private static final String[] AIASK_TABLES = {
        "KGFILE", "KGCOMMUNITY", "KGENTITY", "KGSEGMENT", "KGRELATIONSHIP", "KGIMAGE"
    };

    // ======================== aisearch_manager schema 建表 DDL ========================
    private static final String[] MANAGER_CREATE_TABLE_DDLS = {
        "CREATE TABLE build_task_status (" +
        "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "    task_id VARCHAR(64) NOT NULL," +
        "    schema_name VARCHAR(128) NOT NULL," +
        "    file_name VARCHAR(512) NOT NULL," +
        "    source_type VARCHAR(32) NOT NULL," +
        "    triggered_by VARCHAR(128)," +
        "    input_path VARCHAR(1024)," +
        "    status VARCHAR(32) NOT NULL," +
        "    error_message TEXT default ''," +
        "    created_at TIMESTAMP NOT NULL," +
        "    started_at TIMESTAMP," +
        "    finished_at TIMESTAMP," +
        "    updated_at TIMESTAMP NOT NULL," +
        "    retry_count INT DEFAULT 0," +
        "    extra_info TEXT default ''," +
        "    UNIQUE (task_id)," +
        "    UNIQUE (schema_name, file_name)" +
        ")"
    };

    private static final String[] MANAGER_CREATE_INDEX_DDLS = {
        "create index idx_build_task_schema on build_task_status (schema_name)",
        "create index idx_build_task_status on build_task_status (status)",
        "create index idx_build_task_schema_file on build_task_status (schema_name, file_name)"
    };

    private static final String[] MANAGER_TABLES = { "build_task_status" };

    // ======================== MAIN ========================
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  数据库迁移工具");
        System.out.println("========================================");
        System.out.println("远程: " + REMOTE_URL);
        System.out.println("本地: " + LOCAL_URL);
        System.out.println();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection remoteConn = DriverManager.getConnection(REMOTE_URL, REMOTE_USER, REMOTE_PWD);
                 Connection localConn = DriverManager.getConnection(LOCAL_URL, LOCAL_USER, LOCAL_PWD)) {

                System.out.println("[OK] 远程数据库连接成功");
                System.out.println("[OK] 本地数据库连接成功");
                System.out.println();

                // === 1. 通过 INFORMATION_SCHEMA 发现目标 schema ===
                List<String> aiaskSchemas = new ArrayList<>();
                boolean hasManager = false;

                String q = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME LIKE 'aiask%' ORDER BY SCHEMA_NAME";
                try (Statement s = remoteConn.createStatement(); ResultSet r = s.executeQuery(q)) {
                    while (r.next()) aiaskSchemas.add(r.getString("SCHEMA_NAME"));
                }

                try (Statement s = remoteConn.createStatement();
                     ResultSet r = s.executeQuery(
                         "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'aisearch_manager'")) {
                    hasManager = r.next();
                }

                System.out.println("发现 " + aiaskSchemas.size() + " 个 aiask* schema");
                if (hasManager) System.out.println("发现 aisearch_manager schema");
                System.out.println();

                int totalTables = 0;
                long totalRows = 0;

                // === 2. 迁移 aiask* schema ===
                for (String schema : aiaskSchemas) {
                    MigrationStats s = migrateAiask(remoteConn, localConn, schema);
                    totalTables += s.tableCount;
                    totalRows += s.rowCount;
                }

                // === 3. 迁移 aisearch_manager ===
                if (hasManager) {
                    MigrationStats s = migrateManager(remoteConn, localConn);
                    totalTables += s.tableCount;
                    totalRows += s.rowCount;
                }

                System.out.println("========================================");
                System.out.println("  迁移完成!");
                System.out.println("  schema: " + (aiaskSchemas.size() + (hasManager ? 1 : 0)));
                System.out.println("  表: " + totalTables);
                System.out.println("  行: " + totalRows);
                System.out.println("========================================");
            }
        } catch (Exception e) {
            System.err.println("[错误] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ======================== 迁移 aiask* schema ========================
    private static MigrationStats migrateAiask(Connection remoteConn, Connection localConn, String schema)
            throws SQLException {
        MigrationStats stats = new MigrationStats();
        System.out.println("--- " + schema + " ---");

        // 创建 schema
        try (Statement s = localConn.createStatement()) {
            s.executeUpdate("CREATE SCHEMA IF NOT EXISTS `" + schema + "`");
            s.executeUpdate("USE `" + schema + "`");
        }

        // 找出远程库中实际存在的表
        Set<String> existing = existingTables(remoteConn, schema, AIASK_TABLES);

        // 建表（先删后建，兼容残留）
        for (String ddl : AIASK_CREATE_TABLE_DDLS) {
            String tn = extractTableName(ddl);
            if (existing.contains(tn)) {
                try (Statement s = localConn.createStatement()) {
                    s.executeUpdate("USE `" + schema + "`");
                    try { s.executeUpdate("DROP TABLE `" + tn + "`"); } catch (SQLException ignored) {}
                    s.executeUpdate(ddl);
                }
            }
        }

        // 逐表迁移数据
        for (String table : AIASK_TABLES) {
            if (!existing.contains(table)) continue;
            try {
                long rows = copyData(remoteConn, localConn, schema, table);
                stats.tableCount++;
                stats.rowCount += rows;
                System.out.println("  " + table + ": " + rows + " 行");
            } catch (SQLException e) {
                System.err.println("  [ERROR] " + table + ": " + e.getMessage());
            }
        }

        System.out.println("  => " + stats.tableCount + " 表, " + stats.rowCount + " 行");
        System.out.println();
        return stats;
    }

    // ======================== 迁移 aisearch_manager ========================
    private static MigrationStats migrateManager(Connection remoteConn, Connection localConn)
            throws SQLException {
        MigrationStats stats = new MigrationStats();
        final String schema = "aisearch_manager";
        System.out.println("--- " + schema + " ---");

        try (Statement s = localConn.createStatement()) {
            s.executeUpdate("CREATE SCHEMA IF NOT EXISTS `" + schema + "`");
            s.executeUpdate("USE `" + schema + "`");
        }

        // 建表（先删后建，兼容残留）
        for (String ddl : MANAGER_CREATE_TABLE_DDLS) {
            String tn = extractTableName(ddl);
            try (Statement s = localConn.createStatement()) {
                s.executeUpdate("USE `" + schema + "`");
                try { s.executeUpdate("DROP TABLE `" + tn + "`"); } catch (SQLException ignored) {}
                s.executeUpdate(ddl);
            }
        }
        // 建索引
        for (String idx : MANAGER_CREATE_INDEX_DDLS) {
            try (Statement s = localConn.createStatement()) {
                s.executeUpdate("USE `" + schema + "`");
                s.executeUpdate(idx);
            } catch (SQLException e) {
                System.out.println("  [WARN] 索引: " + e.getMessage());
            }
        }

        // 迁移数据
        for (String table : MANAGER_TABLES) {
            long rows = copyData(remoteConn, localConn, schema, table);
            stats.tableCount++;
            stats.rowCount += rows;
            System.out.println("  " + table + ": " + rows + " 行");
        }

        System.out.println("  => " + stats.tableCount + " 表, " + stats.rowCount + " 行");
        System.out.println();
        return stats;
    }

    // ======================== 数据复制（逐行 INSERT） ========================
    private static long copyData(Connection src, Connection dst, String schema, String table)
            throws SQLException {
        List<String> cols = columnNames(src, schema, table);
        Set<String> auto = autoIncrCols(src, schema, table);

        // 构建 INSERT 列（跳过自增列）
        List<String> insertCols = new ArrayList<>();
        List<Integer> idxMap = new ArrayList<>();
        for (int i = 0; i < cols.size(); i++) {
            if (!auto.contains(cols.get(i))) {
                insertCols.add(cols.get(i));
                idxMap.add(i);
            }
        }

        StringBuilder sb = new StringBuilder("INSERT INTO `").append(table).append("` (");
        for (int i = 0; i < insertCols.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("`").append(insertCols.get(i)).append("`");
        }
        sb.append(") VALUES (");
        for (int i = 0; i < insertCols.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")");

        long cnt = 0, skipped = 0;
        String sel = "SELECT * FROM `" + schema + "`.`" + table + "`";
        try (Statement rsStmt = src.createStatement();
             ResultSet rs = rsStmt.executeQuery(sel);
             PreparedStatement ps = dst.prepareStatement(sb.toString())) {

            while (rs.next()) {
                try {
                    for (int i = 0; i < insertCols.size(); i++) {
                        ps.setObject(i + 1, rs.getObject(idxMap.get(i) + 1));
                    }
                    ps.executeUpdate();
                    cnt++;
                } catch (SQLException e) {
                    skipped++;
                }
                if (cnt % 5000 == 0) {
                    System.out.println("    progress: " + cnt + " rows...");
                }
            }
        }
        if (skipped > 0) System.out.println("    (skipped " + skipped + " bad rows)");
        return cnt;
    }

    // ======================== 辅助方法 ========================
    private static String extractTableName(String ddl) {
        String u = ddl.toUpperCase();
        int s = u.indexOf("CREATE TABLE ") + 13;
        int e = u.indexOf("(", s);
        if (e < 0) e = u.indexOf(" (", s);
        return ddl.substring(s, e).trim().replace("`", "");
    }

    private static Set<String> existingTables(Connection conn, String schema, String[] candidates)
            throws SQLException {
        // 先查远程库中该 schema 的所有表，再在 Java 中用 equalsIgnoreCase 匹配
        Set<String> existingUpper = new LinkedHashSet<>();
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) existingUpper.add(rs.getString("TABLE_NAME"));
            }
        }
        // 用忽略大小写匹配
        Set<String> result = new LinkedHashSet<>();
        for (String c : candidates) {
            for (String e : existingUpper) {
                if (c.equalsIgnoreCase(e)) {
                    result.add(e); // 返回远程库中的真实表名
                    break;
                }
            }
        }
        return result;
    }

    private static Set<String> autoIncrCols(Connection conn, String schema, String table)
            throws SQLException {
        Set<String> set = new LinkedHashSet<>();
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND EXTRA LIKE '%auto_increment%'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString("COLUMN_NAME"));
            }
        }
        return set;
    }

    private static List<String> columnNames(Connection conn, String schema, String table)
            throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("COLUMN_NAME"));
            }
        }
        return list;
    }

    private static class MigrationStats {
        int tableCount;
        long rowCount;
    }
}
