CREATE TABLE build_task_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    schema_name VARCHAR(128) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    triggered_by VARCHAR(128),
    input_path VARCHAR(1024),
    status VARCHAR(32) NOT NULL,
    error_message TEXT default '',
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    retry_count INT DEFAULT 0,
    extra_info TEXT default '',
    UNIQUE (task_id),
    UNIQUE (schema_name, file_name)
);

create index idx_build_task_schema on build_task_status (schema_name);
create index idx_build_task_status on build_task_status (status);
create index idx_build_task_schema_file on build_task_status (schema_name, file_name);
