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
