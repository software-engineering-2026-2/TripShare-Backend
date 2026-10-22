ALTER TABLE users
    ADD COLUMN firebase_uid varchar(128),
    ADD CONSTRAINT uq_users_firebase_uid UNIQUE (firebase_uid);
