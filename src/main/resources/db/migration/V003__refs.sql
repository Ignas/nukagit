CREATE TABLE refs
(
    repository_id    CHAR(36)   NOT NULL,
    name             VARCHAR(255) NOT NULL,
    object_id        TEXT,
    target           TEXT,
    storage          VARCHAR(255),
    peeled_ref       TEXT,
    symbolic         BOOLEAN,
    peeled           BOOLEAN,

    FOREIGN KEY (repository_id) REFERENCES repositories (id),
    PRIMARY KEY (repository_id, name)
);