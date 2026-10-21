CREATE TABLE users (
    uid      VARCHAR(128) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    phone    VARCHAR(255),
    CONSTRAINT pk_users PRIMARY KEY (uid),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
);