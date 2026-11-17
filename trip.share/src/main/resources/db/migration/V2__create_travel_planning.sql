CREATE TABLE trips (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_trips PRIMARY KEY,
    user_id uuid NOT NULL CONSTRAINT fk_trips_user REFERENCES users(id),
    destination_id uuid NOT NULL CONSTRAINT fk_trips_destination REFERENCES destinations(id),
    title text NOT NULL,
    starts_on date NOT NULL,
    ends_on date NOT NULL,
    duration_days integer GENERATED ALWAYS AS (ends_on - starts_on + 1) STORED,
    pace_code text CONSTRAINT fk_trips_pace REFERENCES travel_paces(code),
    transport_mode_code text CONSTRAINT fk_trips_transport REFERENCES transport_modes(code),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_trips_title CHECK (btrim(title) <> ''),
    CONSTRAINT ck_trips_period CHECK (ends_on >= starts_on)
);

CREATE TABLE trip_interests (
    trip_id uuid NOT NULL CONSTRAINT fk_trip_interests_trip REFERENCES trips(id) ON DELETE CASCADE,
    category_code text NOT NULL CONSTRAINT fk_trip_interests_category REFERENCES categories(code),
    CONSTRAINT pk_trip_interests PRIMARY KEY (trip_id, category_code)
);

CREATE TABLE trip_days (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_trip_days PRIMARY KEY,
    trip_id uuid NOT NULL CONSTRAINT fk_trip_days_trip REFERENCES trips(id) ON DELETE CASCADE,
    day_number integer NOT NULL,
    available_from time,
    available_until time,
    CONSTRAINT uq_trip_days_number UNIQUE (trip_id, day_number),
    CONSTRAINT ck_trip_days_number CHECK (day_number > 0),
    CONSTRAINT ck_trip_days_interval CHECK (
        (available_from IS NULL AND available_until IS NULL) OR
        (available_from IS NOT NULL AND available_until IS NOT NULL AND available_from < available_until)
    )
);

CREATE TABLE itineraries (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_itineraries PRIMARY KEY,
    trip_id uuid NOT NULL CONSTRAINT fk_itineraries_trip REFERENCES trips(id) ON DELETE CASCADE,
    title text NOT NULL,
    description text,
    category_code text CONSTRAINT fk_itineraries_category REFERENCES categories(code),
    origin text NOT NULL DEFAULT 'MANUAL',
    visibility text NOT NULL DEFAULT 'PRIVATE',
    hidden_by_moderation boolean NOT NULL DEFAULT false,
    published_at timestamptz,
    source_itinerary_id uuid CONSTRAINT fk_itineraries_source REFERENCES itineraries(id) ON DELETE SET NULL,
    source_author_name text,
    source_title text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_itineraries_title CHECK (btrim(title) <> ''),
    CONSTRAINT ck_itineraries_origin CHECK (origin IN ('MANUAL', 'AUTOMATIC', 'COPIED')),
    CONSTRAINT ck_itineraries_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    CONSTRAINT ck_itineraries_source CHECK (source_itinerary_id IS NULL OR source_itinerary_id <> id),
    CONSTRAINT ck_itineraries_attribution CHECK (
        origin <> 'COPIED' OR (source_author_name IS NOT NULL AND btrim(source_author_name) <> ''
        AND source_title IS NOT NULL AND btrim(source_title) <> '')
    ),
    CONSTRAINT ck_itineraries_publication CHECK (
        visibility = 'PRIVATE' OR (category_code IS NOT NULL AND description IS NOT NULL
        AND btrim(description) <> '' AND published_at IS NOT NULL)
    )
);

CREATE TABLE itinerary_activities (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_itinerary_activities PRIMARY KEY,
    itinerary_id uuid NOT NULL CONSTRAINT fk_itinerary_activities_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    trip_day_id uuid CONSTRAINT fk_itinerary_activities_day REFERENCES trip_days(id),
    place_id uuid CONSTRAINT fk_itinerary_activities_place REFERENCES places(id) ON DELETE SET NULL,
    name text NOT NULL,
    location text,
    latitude numeric(9,6),
    longitude numeric(9,6),
    position integer,
    starts_at time,
    duration_minutes integer,
    notes text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_itinerary_activities_position UNIQUE (itinerary_id, trip_day_id, position) DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT ck_itinerary_activities_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_itinerary_activities_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_itinerary_activities_allocation CHECK (
        (trip_day_id IS NULL AND position IS NULL AND starts_at IS NULL) OR
        (trip_day_id IS NOT NULL AND position IS NOT NULL AND position > 0)
    ),
    CONSTRAINT ck_itinerary_activities_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL AND
         latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    )
);

CREATE TABLE itinerary_legs (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_itinerary_legs PRIMARY KEY,
    from_activity_id uuid NOT NULL CONSTRAINT fk_itinerary_legs_from REFERENCES itinerary_activities(id) ON DELETE CASCADE,
    to_activity_id uuid NOT NULL CONSTRAINT fk_itinerary_legs_to REFERENCES itinerary_activities(id) ON DELETE CASCADE,
    transport_mode_code text NOT NULL CONSTRAINT fk_itinerary_legs_transport REFERENCES transport_modes(code),
    calculation_status text NOT NULL DEFAULT 'PENDING',
    distance_meters numeric(12,2),
    duration_seconds integer,
    source text,
    calculated_at timestamptz,
    route_geometry text,
    CONSTRAINT uq_itinerary_legs_pair UNIQUE (from_activity_id, to_activity_id, transport_mode_code),
    CONSTRAINT ck_itinerary_legs_distinct CHECK (from_activity_id <> to_activity_id),
    CONSTRAINT ck_itinerary_legs_status CHECK (calculation_status IN ('PENDING', 'CALCULATED', 'UNAVAILABLE')),
    CONSTRAINT ck_itinerary_legs_estimates CHECK (
        (calculation_status = 'CALCULATED' AND distance_meters IS NOT NULL AND distance_meters >= 0
         AND duration_seconds IS NOT NULL AND duration_seconds >= 0 AND calculated_at IS NOT NULL
         AND source IS NOT NULL AND btrim(source) <> '') OR
        (calculation_status <> 'CALCULATED' AND distance_meters IS NULL AND duration_seconds IS NULL)
    )
);
