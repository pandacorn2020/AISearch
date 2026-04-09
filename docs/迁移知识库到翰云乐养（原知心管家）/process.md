远程数据库连接信息：
server:124.220.178.83
host:1978
user:system
password:CHANGEME
---
远程数据库的建表语句：
```
create schema IMM_EX;

use IMM_EX;

CREATE TABLE KGFile (
    name VARCHAR(256) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE KGCommunity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    summary TEXT
);

create vector index on KGCommunity (summary) ;

CREATE TABLE KGEntity (
    name VARCHAR(64) NOT NULL,
    type VARCHAR(64),
    description TEXT,
    PRIMARY KEY (name)
);

create index index_KGEntity_name on KGEntity (name);
create vector index on KGEntity (name);
create text index text_index_kgentity_name on KGEntity (name);


CREATE TABLE KGSegment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment TEXT
);

create vector index on KGSegment (segment);
create text index text_index_kgsegment_segment on KGSegment (segment);

CREATE TABLE KGRelationship (
    source VARCHAR(128) NOT NULL,
    target VARCHAR(128) NOT NULL,
    relation VARCHAR(32) NOT NULL,
    description TEXT,
    PRIMARY KEY (source, target, relation)
);

```

---
本地数据库连接信息：
server:127.0.0.1
host:1978
user:system
password:CHANGEME
---

本地数据库的建表语句
```
create schema aiask_test_schema;
use aiask_test_schema;
CREATE TABLE KGFile (
    name VARCHAR(256) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE KGCommunity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    summary TEXT
);

create vector index on KGCommunity (summary) ;

CREATE TABLE KGEntity (
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64),
    description TEXT,
    file_name VARCHAR(256),
    PRIMARY KEY (name)
);

create index index_KGEntity_name on KGEntity (name);
create vector index on KGEntity (name);
create text index text_index_kgentity_name on KGEntity (name);


CREATE TABLE KGSegment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment TEXT,
    file_name VARCHAR(256)
);

create vector index on KGSegment (segment);
create text index text_index_kgsegment_segment on KGSegment (segment);

CREATE TABLE KGRelationship (
    source VARCHAR(128) NOT NULL,
    target VARCHAR(128) NOT NULL,
    relation VARCHAR(32) NOT NULL,
    description TEXT,
    file_name VARCHAR(256),
    PRIMARY KEY (source, target, relation)
);

create index index_KGRelationship_source on KGRelationship (source);
create index index_KGRelationship_target on KGRelationship (target);

CREATE TABLE KGIMAGE (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content BLOB NOT NULL,
    description TEXT
);
create vector index on KGIMAGE (description);
create text index text_index_kgimage_description on KGIMAGE (description);


```

---

编写一个测试用例作为工具，将本地数据库的aiask_test_schema schema数据迁移到远程数据库 IMM_EX schema里头，只迁移IMM_EX有的字段（个别字段只有IMM_EX有，aiask_test_schema里没有）