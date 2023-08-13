CREATE TABLE repositories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_on TIMESTAMP NULL DEFAULT NULL
);

ALTER TABLE repositories
    ADD not_archived BOOLEAN
    GENERATED ALWAYS AS (IF(deleted_on IS NULL, 1, NULL)) VIRTUAL;

ALTER TABLE repositories
    ADD CONSTRAINT UNIQUE (name, not_archived);
