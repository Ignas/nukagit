CREATE TABLE users
(
    id         CHAR(36) PRIMARY KEY,
    username   VARCHAR(255) NOT NULL,
    created_on TIMESTAMP         DEFAULT CURRENT_TIMESTAMP,
    deleted_on TIMESTAMP    NULL DEFAULT NULL
);

ALTER TABLE users
    ADD not_archived BOOLEAN
        GENERATED ALWAYS AS (IF(deleted_on IS NULL, 1, NULL)) VIRTUAL;

ALTER TABLE users
    ADD CONSTRAINT UNIQUE (username, not_archived);

CREATE TABLE public_keys
(
    id          CHAR(36) PRIMARY KEY,
    user_id     CHAR(36)  NOT NULL,
    fingerprint CHAR(59)  NOT NULL,
    modulus     VARBINARY(8000)    NOT NULL,
    exponent    VARBINARY(8000)    NOT NULL,
    created_on  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    deleted_on  TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

ALTER TABLE public_keys
    ADD not_archived BOOLEAN
        GENERATED ALWAYS AS (IF(deleted_on IS NULL, 1, NULL)) VIRTUAL;

ALTER TABLE public_keys
    ADD CONSTRAINT UNIQUE (fingerprint, not_archived);