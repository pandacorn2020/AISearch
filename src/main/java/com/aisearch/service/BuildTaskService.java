package com.aisearch.service;

import com.aisearch.config.KgProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BuildTaskService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KgProperties kgProperties;

    public String createTask(String schemaName, String fileName, String sourceType,
                             String triggeredBy, String inputPath, String extraInfo) {
        String now = nowString();
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String sql = "INSERT INTO " + Schemas.MANAGER + ".build_task_status " +
            "(task_id, schema_name, file_name, source_type, triggered_by, input_path, status, error_message, " +
            "created_at, started_at, finished_at, updated_at, retry_count, extra_info) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(
                sql,
                taskId,
                schemaName,
                fileName,
                sourceType,
                triggeredBy,
                inputPath,
                "PENDING",
                null,
                now,
                null,
                null,
                now,
                0,
                extraInfo
            );
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (message.contains("duplicate") || message.contains("unique") || message.contains("already exists")) {
                throw new IllegalArgumentException("重复构建任务：schema=" + schemaName + ", file_name=" + fileName);
            }
            throw e;
        }
        return taskId;
    }

    public boolean existsBySchemaAndFileName(String schemaName, String fileName) {
        String sql = "SELECT COUNT(*) FROM " + Schemas.MANAGER +
            ".build_task_status WHERE schema_name = ? AND file_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, schemaName, fileName);
        return count != null && count > 0;
    }

    // Returns null when task is currently running; otherwise returns taskId to execute.
    public String prepareTaskForBuild(String schemaName, String fileName, String sourceType,
                                      String triggeredBy, String inputPath, String extraInfo) {
        String querySql = "SELECT task_id, status, updated_at FROM " + Schemas.MANAGER +
            ".build_task_status WHERE schema_name = ? AND file_name = ? ORDER BY id DESC LIMIT 1";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(querySql, schemaName, fileName);
        if (rows.isEmpty()) {
            return createTask(schemaName, fileName, sourceType, triggeredBy, inputPath, extraInfo);
        }

        Map<String, Object> row = rows.get(0);
        String taskId = String.valueOf(row.get("task_id"));
        String status = row.get("status") == null ? "" : String.valueOf(row.get("status"));
        if ("RUNNING".equalsIgnoreCase(status)) {
            if (isRunningTaskAlive(row.get("updated_at"))) {
                return null;
            }
        } else {
            // Non-RUNNING tasks are not retriggered automatically.
            return null;
        }

        String now = nowString();
        String updateSql = "UPDATE " + Schemas.MANAGER +
            ".build_task_status SET source_type = ?, triggered_by = ?, input_path = ?, status = ?, " +
            "error_message = ?, started_at = ?, finished_at = ?, updated_at = ?, extra_info = ? WHERE task_id = ?";
        jdbcTemplate.update(
            updateSql,
            sourceType,
            triggeredBy,
            inputPath,
            "PENDING",
            null,
            null,
            null,
            now,
            extraInfo,
            taskId
        );
        return taskId;
    }

    public String getLatestStatusBySchemaAndFile(String schemaName, String fileName) {
        String sql = "SELECT status FROM " + Schemas.MANAGER +
            ".build_task_status WHERE schema_name = ? AND file_name = ? ORDER BY id DESC LIMIT 1";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, schemaName, fileName);
        if (rows.isEmpty()) {
            return null;
        }
        Object status = rows.get(0).get("status");
        return status == null ? null : status.toString();
    }

    private boolean isRunningTaskAlive(Object updatedAtObj) {
        LocalDateTime updatedAt = parseDateTime(updatedAtObj);
        if (updatedAt == null) {
            return false;
        }
        long seconds = ChronoUnit.SECONDS.between(updatedAt, LocalDateTime.now());
        long staleThreshold = Math.max(kgProperties.getTaskTimeoutSeconds(), 30);
        return seconds <= staleThreshold;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        String text = value.toString();
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().replace('T', ' ');
        if (normalized.length() >= 19) {
            normalized = normalized.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    public void markRunning(String taskId) {
        String now = nowString();
        String sql = "UPDATE " + Schemas.MANAGER +
            ".build_task_status SET status = ?, started_at = ?, updated_at = ? WHERE task_id = ?";
        jdbcTemplate.update(sql, "RUNNING", now, now, taskId);
    }

    public void markHeartbeat(String taskId) {
        String now = nowString();
        String sql = "UPDATE " + Schemas.MANAGER +
            ".build_task_status SET updated_at = ? WHERE task_id = ?";
        jdbcTemplate.update(sql, now, taskId);
    }

    public void markSuccess(String taskId) {
        String now = nowString();
        String sql = "UPDATE " + Schemas.MANAGER +
            ".build_task_status SET status = ?, finished_at = ?, updated_at = ?, error_message = ? WHERE task_id = ?";
        jdbcTemplate.update(sql, "SUCCESS", now, now, null, taskId);
    }

    public void markFailed(String taskId, String errorMessage) {
        String now = nowString();
        String sql = "UPDATE " + Schemas.MANAGER +
            ".build_task_status SET status = ?, finished_at = ?, updated_at = ?, error_message = ?, retry_count = retry_count + 1 WHERE task_id = ?";
        jdbcTemplate.update(sql, "FAILED", now, now, errorMessage, taskId);
    }

    public List<Map<String, Object>> findBySchema(String schemaName) {
        String sql = "SELECT task_id, schema_name, file_name, source_type, triggered_by, input_path, status, " +
            "error_message, created_at, started_at, finished_at, updated_at, retry_count, extra_info " +
            "FROM " + Schemas.MANAGER + ".build_task_status WHERE schema_name = ? ORDER BY id DESC";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, schemaName);
        formatDateTimeFields(list);
        return list;
    }

    public List<Map<String, Object>> findBySchemaAndFile(String schemaName, String fileName) {
        String sql = "SELECT task_id, schema_name, file_name, source_type, triggered_by, input_path, status, " +
            "error_message, created_at, started_at, finished_at, updated_at, retry_count, extra_info " +
            "FROM " + Schemas.MANAGER + ".build_task_status WHERE schema_name = ? AND file_name = ? ORDER BY id DESC";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, schemaName, fileName);
        formatDateTimeFields(list);
        return list;
    }

    public Map<String, Object> summarizeStatus(List<Map<String, Object>> tasks) {
        Map<String, Object> summary = new HashMap<>();
        int pending = 0;
        int running = 0;
        int success = 0;
        int failed = 0;

        for (Map<String, Object> task : tasks) {
            Object statusObj = task.get("status");
            String status = statusObj == null ? "" : statusObj.toString();
            if ("PENDING".equalsIgnoreCase(status)) {
                pending++;
            } else if ("RUNNING".equalsIgnoreCase(status)) {
                running++;
            } else if ("SUCCESS".equalsIgnoreCase(status)) {
                success++;
            } else if ("FAILED".equalsIgnoreCase(status)) {
                failed++;
            }
        }

        summary.put("pending", pending);
        summary.put("running", running);
        summary.put("success", success);
        summary.put("failed", failed);
        summary.put("total", tasks.size());

        String overallStatus;
        if (pending > 0 || running > 0) {
            overallStatus = "BUILDING";
        } else if (failed > 0) {
            overallStatus = "FAILED";
        } else if (success > 0) {
            overallStatus = "SUCCESS";
        } else {
            overallStatus = "UNKNOWN";
        }
        summary.put("overallStatus", overallStatus);
        return summary;
    }

    private void formatDateTimeFields(List<Map<String, Object>> tasks) {
        for (Map<String, Object> task : tasks) {
            formatDateTimeField(task, "created_at");
            formatDateTimeField(task, "started_at");
            formatDateTimeField(task, "finished_at");
            formatDateTimeField(task, "updated_at");
        }
    }

    private void formatDateTimeField(Map<String, Object> task, String key) {
        Object value = task.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof LocalDateTime localDateTime) {
            task.put(key, DATE_TIME_FORMATTER.format(localDateTime));
            return;
        }
        String text = value.toString();
        if (text.length() >= 19) {
            task.put(key, text.substring(0, 19));
        } else {
            task.put(key, text);
        }
    }

    private String nowString() {
        return DATE_TIME_FORMATTER.format(LocalDateTime.now());
    }
}
