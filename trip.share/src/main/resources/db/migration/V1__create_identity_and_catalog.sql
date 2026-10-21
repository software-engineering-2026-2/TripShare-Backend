CREATE TABLE users (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_users PRIMARY KEY,
    username varchar(50) NOT NULL,
    name varchar(120) NOT NULL,
    email varchar(254) NOT NULL,
    phone varchar(32),
    password_hash text NOT NULL,
    bio text,
    hometown text,
    role text NOT NULL DEFAULT 'USER',
    status text NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_users_username CHECK (btrim(username) <> ''),
    CONSTRAINT ck_users_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_users_email CHECK (btrim(email) <> '' AND email = btrim(email)),
    CONSTRAINT ck_users_password_hash CHECK (length(password_hash) >= 60),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE UNIQUE INDEX uq_users_username ON users (lower(username));
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

CREATE TABLE user_sessions (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_user_sessions PRIMARY KEY,
    user_id uuid NOT NULL CONSTRAINT fk_user_sessions_user REFERENCES users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL CONSTRAINT uq_user_sessions_token_hash UNIQUE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CONSTRAINT ck_user_sessions_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_user_sessions_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_user_sessions_revoked CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE TABLE password_reset_tokens (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_password_reset_tokens PRIMARY KEY,
    user_id uuid NOT NULL CONSTRAINT fk_password_reset_tokens_user REFERENCES users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL CONSTRAINT uq_password_reset_tokens_hash UNIQUE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 minutes'),
    used_at timestamptz,
    CONSTRAINT ck_password_reset_tokens_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_password_reset_tokens_expiry CHECK (
        expires_at > created_at AND expires_at <= created_at + INTERVAL '30 minutes'
    ),
    CONSTRAINT ck_password_reset_tokens_used CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE TABLE categories (
    code text CONSTRAINT pk_categories PRIMARY KEY,
    name text NOT NULL,
    CONSTRAINT ck_categories_code CHECK (code ~ '^[A-Z][A-Z_]*$'),
    CONSTRAINT ck_categories_name CHECK (btrim(name) <> '')
);

CREATE TABLE travel_paces (
    code text CONSTRAINT pk_travel_paces PRIMARY KEY,
    name text NOT NULL,
    CONSTRAINT ck_travel_paces_name CHECK (btrim(name) <> '')
);

CREATE TABLE transport_modes (
    code text CONSTRAINT pk_transport_modes PRIMARY KEY,
    name text NOT NULL,
    CONSTRAINT ck_transport_modes_name CHECK (btrim(name) <> '')
);

CREATE TABLE destinations (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_destinations PRIMARY KEY,
    name text NOT NULL,
    region text,
    country_code char(2) NOT NULL,
    timezone text NOT NULL,
    latitude numeric(9,6),
    longitude numeric(9,6),
    source text NOT NULL,
    external_id text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_destinations_external UNIQUE (source, external_id),
    CONSTRAINT ck_destinations_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_destinations_country CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_destinations_timezone CHECK (btrim(timezone) <> ''),
    CONSTRAINT ck_destinations_source CHECK (btrim(source) <> ''),
    CONSTRAINT ck_destinations_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL AND
         latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    )
);

CREATE TABLE places (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_places PRIMARY KEY,
    destination_id uuid NOT NULL CONSTRAINT fk_places_destination REFERENCES destinations(id),
    name text NOT NULL,
    description text,
    address text,
    neighborhood text,
    latitude numeric(9,6),
    longitude numeric(9,6),
    estimated_visit_minutes integer,
    estimated_cost numeric(12,2),
    currency char(3),
    source text NOT NULL,
    external_id text,
    source_url text,
    source_updated_at timestamptz,
    removed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_places_external UNIQUE (source, external_id),
    CONSTRAINT ck_places_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_places_source CHECK (btrim(source) <> ''),
    CONSTRAINT ck_places_duration CHECK (estimated_visit_minutes > 0),
    CONSTRAINT ck_places_cost CHECK (
        (estimated_cost IS NULL AND currency IS NULL) OR
        (estimated_cost IS NOT NULL AND estimated_cost >= 0 AND currency IS NOT NULL AND currency ~ '^[A-Z]{3}$')
    ),
    CONSTRAINT ck_places_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL AND
         latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    )
);

CREATE TABLE place_categories (
    place_id uuid NOT NULL CONSTRAINT fk_place_categories_place REFERENCES places(id) ON DELETE CASCADE,
    category_code text NOT NULL CONSTRAINT fk_place_categories_category REFERENCES categories(code),
    CONSTRAINT pk_place_categories PRIMARY KEY (place_id, category_code)
);

-- No row means unknown hours, not a closed venue. Multiple periods per weekday are allowed.
CREATE TABLE place_opening_hours (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_place_opening_hours PRIMARY KEY,
    place_id uuid NOT NULL CONSTRAINT fk_place_opening_hours_place REFERENCES places(id) ON DELETE CASCADE,
    weekday smallint NOT NULL,
    opens_at time NOT NULL,
    closes_at time NOT NULL,
    closes_next_day boolean NOT NULL DEFAULT false,
    CONSTRAINT uq_place_opening_hours_period UNIQUE (place_id, weekday, opens_at),
    CONSTRAINT ck_place_opening_hours_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT ck_place_opening_hours_interval CHECK (closes_next_day OR opens_at < closes_at)
);

CREATE TABLE place_images (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_place_images PRIMARY KEY,
    place_id uuid NOT NULL CONSTRAINT fk_place_images_place REFERENCES places(id) ON DELETE CASCADE,
    url text NOT NULL,
    alt_text text,
    source text NOT NULL,
    position integer NOT NULL,
    CONSTRAINT uq_place_images_position UNIQUE (place_id, position),
    CONSTRAINT ck_place_images_position CHECK (position > 0),
    CONSTRAINT ck_place_images_url CHECK (btrim(url) <> ''),
    CONSTRAINT ck_place_images_source CHECK (btrim(source) <> '')
);
