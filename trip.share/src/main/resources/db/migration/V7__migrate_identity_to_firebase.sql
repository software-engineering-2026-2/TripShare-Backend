-- Authorized development reset: legacy users have no trustworthy Firebase UID.
-- CASCADE clears their dependent domain data; catalogs and Flyway history remain.
TRUNCATE TABLE users CASCADE;

DROP TABLE user_sessions;
DROP TABLE password_reset_tokens;
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;

CREATE TABLE auth (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_auth PRIMARY KEY,
    uuid varchar(128) NOT NULL CONSTRAINT uq_auth_uuid UNIQUE,
    CONSTRAINT ck_auth_uuid CHECK (btrim(uuid) <> '')
);

ALTER TABLE users
    ADD COLUMN auth_id uuid NOT NULL,
    ADD CONSTRAINT uq_users_auth UNIQUE (auth_id),
    ADD CONSTRAINT fk_users_auth FOREIGN KEY (auth_id) REFERENCES auth(id);
