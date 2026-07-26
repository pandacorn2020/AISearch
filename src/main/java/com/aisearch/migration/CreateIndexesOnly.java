package com.aisearch.migration;

import java.sql.*;
import java.util.*;

/**
 * 仅创建索引工具：在所有 aiask* schema 和 aisearch_manager 下创建索引。
 * 前提：表和数据已存在。
 */
public class CreateIndexesOnly {

    private static final String LOCAL_URL = "jdbc:mysql://127.0.0.1:3307?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
    private static final String LOCAL_USER = "system";
    private static final String LOCAL_PWD = "CHANGEME";

    // ======================== aiask* 索引 ========================
    private static final String[] AIASK_INDEXES = {
        "create vector index on KGCOMMUNITY (summary)",
        "create index index_KGEntity_name on KGENTITY (name)",
        "create vector index on KGENTITY (name)",
        "create text index text_index_kgentity_name on KGENTITY (name)",
        "create vector index on KGSEGMENT (segment)",
        "create text index text_index_kgsegment_segment on KGSEGMENT (segment)",
        "create index index_KGRelationship_source on KGRELATIONSHIP (source)",
        "create index index_KGRelationship_target on KGRELATIONSHIP (target)",
        "create vector index on KGIMAGE (description)",
        "create text index text_index_kgimage_description on KGIMAGE (description)"
    };

    // ======================== aisearch_manager 索引 ========================
    private static final String[] MANAGER_INDEXES = {
        "create index idx_build_task_schema on build_task_status (schema_name)",
        "create index idx_build_task_status on build_task_status (status)",
        "create index idx_build_task_schema_file on build_task_status (schema_name, file_name)"
    };

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(LOCAL_URL, LOCAL_USER, LOCAL_PWD)) {
            System.out.println("[OK] 本地数据库连接成功");
            System.out.println();

            // 1. 发现所有 aiask* schema
            List<String> aiaskSchemas = new ArrayList<>();
            try (Statement s = conn.createStatement();
                 ResultSet r = s.executeQuery(
                     "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME LIKE 'aiask%' ORDER BY SCHEMA_NAME")) {
                while (r.next()) aiaskSchemas.add(r.getString("SCHEMA_NAME"));
            }

            // 2. 检查 aisearch_manager
            boolean hasManager = false;
            try (Statement s = conn.createStatement();
                 ResultSet r = s.executeQuery(
                     "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'aisearch_manager'")) {
                hasManager = r.next();
            }

            System.out.println("发现 " + aiaskSchemas.size() + " 个 aiask* schema");
            if (hasManager) System.out.println("发现 aisearch_manager");
            System.out.println();

            int ok = 0, warn = 0;

            // 3. 为每个 aiask* schema 建索引
            for (String schema : aiaskSchemas) {
                System.out.println("--- " + schema + " ---");
                try (Statement s = conn.createStatement()) {
                    s.executeUpdate("USE `" + schema + "`");
                }
                for (String idx : AIASK_INDEXES) {
                    try (Statement s = conn.createStatement()) {
                        s.executeUpdate(idx);
                        ok++;
                    } catch (SQLException e) {
                        System.out.println("  [WARN] " + e.getMessage());
                        warn++;
                    }
                }
            }

            // 4. aisearch_manager 索引
            if (hasManager) {
                System.out.println("--- aisearch_manager ---");
                try (Statement s = conn.createStatement()) {
                    s.executeUpdate("USE `aisearch_manager`");
                }
                for (String idx : MANAGER_INDEXES) {
                    try (Statement s = conn.createStatement()) {
                        s.executeUpdate(idx);
                        ok++;
                    } catch (SQLException e) {
                        System.out.println("  [WARN] " + e.getMessage());
                        warn++;
                    }
                }
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println("  完成! 成功: " + ok + ", 跳过: " + warn);
            System.out.println("========================================");
        }
    }
}
