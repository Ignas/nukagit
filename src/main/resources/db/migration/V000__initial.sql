CREATE TABLE repositories (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    push_id CHAR(36),
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_on TIMESTAMP NULL DEFAULT NULL
);

ALTER TABLE repositories
    ADD not_archived BOOLEAN
    GENERATED ALWAYS AS (IF(deleted_on IS NULL, 1, NULL)) VIRTUAL;

ALTER TABLE repositories
    ADD CONSTRAINT UNIQUE (name, not_archived);

CREATE TABLE pushes (
    id CHAR(36) PRIMARY KEY,
    repository_id CHAR(36) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_on TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (repository_id) REFERENCES repositories(id)
);


CREATE TABLE packs (
    push_id CHAR(36) NOT NULL,
    -- name is a uuid + - + source
    name CHAR(128) NOT NULL,
    source varchar(50) NOT NULL,
    ext varchar(50) NOT NULL,
    file_size BIGINT NOT NULL,
    object_count BIGINT NOT NULL,
    max_update_index BIGINT NOT NULL,
    min_update_index BIGINT NOT NULL,
    FOREIGN KEY (push_id) REFERENCES pushes(id),
    PRIMARY KEY (push_id, name, source, ext)
);

ALTER TABLE packs
    ADD ref_pack BOOLEAN
        GENERATED ALWAYS AS (IF(ext = 'ref', 1, NULL)) VIRTUAL;

ALTER TABLE packs
    ADD CONSTRAINT UNIQUE (max_update_index, ref_pack);