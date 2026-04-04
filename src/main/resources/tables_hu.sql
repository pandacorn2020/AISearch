CREATE TABLE KGFile (
    name VARCHAR(256) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE KGCommunity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    summary TEXT
);



CREATE TABLE KGEntity (
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64),
    description TEXT,
    file_name VARCHAR(256),
    PRIMARY KEY (name)
);

create text index text_index_kgentity_name on KGEntity (name);
create text index text_idnex_kgentity_description on KGEntity (description);


CREATE TABLE KGSegment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment TEXT,
    file_name VARCHAR(256)
);

create text index text_index_kgsegment_segment on KGSegment (segment);

CREATE TABLE KGRelationship (
    source VARCHAR(128) NOT NULL,
    target VARCHAR(128) NOT NULL,
    relation VARCHAR(32) NOT NULL,
    description TEXT,
    file_name VARCHAR(256),
    PRIMARY KEY (source, target, relation)
);

CREATE TABLE KGIMAGE (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content BLOB NOT NULL,
    description TEXT
);
create text index text_index_kgimage_description on KGIMAGE (description);

