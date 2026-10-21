# Dicionário de dados — TripShare

PostgreSQL 16, migrations V1–V6. Gerado a partir do catálogo real; SQL versionado é a fonte de verdade.

Ver [modelagem, regras, diagrama e requisitos](database.md). `NULL` representa informação opcional ou desconhecida.

Índices de PK/UNIQUE não são repetidos na lista de índices adicionais.

## categories

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `code` | `text` | Não | `—` |
| `name` | `text` | Não | `—` |

Constraints:

```sql
ck_categories_code: CHECK ((code ~ '^[A-Z][A-Z_]*$'::text));
ck_categories_name: CHECK ((btrim(name) <> ''::text));
pk_categories: PRIMARY KEY (code);
```

## content_reports

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `reporter_id` | `uuid` | Não | `—` |
| `itinerary_id` | `uuid` | Sim | `—` |
| `comment_id` | `uuid` | Sim | `—` |
| `reason` | `text` | Não | `—` |
| `details` | `text` | Sim | `—` |
| `status` | `text` | Não | `'PENDING'::text` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_content_reports_details: CHECK (((reason <> 'OTHER'::text) OR ((details IS NOT NULL) AND (btrim(details) <> ''::text))));
ck_content_reports_reason: CHECK ((reason = ANY (ARRAY['INAPPROPRIATE_CONTENT'::text, 'MISLEADING_INFORMATION'::text, 'SPAM'::text, 'OTHER'::text])));
ck_content_reports_status: CHECK ((status = ANY (ARRAY['PENDING'::text, 'DISMISSED'::text, 'ACTIONED'::text])));
ck_content_reports_target: CHECK ((num_nonnulls(itinerary_id, comment_id) = 1));
fk_content_reports_comment: FOREIGN KEY (comment_id) REFERENCES itinerary_comments(id) ON DELETE CASCADE;
fk_content_reports_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_content_reports_reporter: FOREIGN KEY (reporter_id) REFERENCES users(id);
pk_content_reports: PRIMARY KEY (id);
```

Índices adicionais:

```sql
CREATE INDEX idx_content_reports_comment ON public.content_reports USING btree (comment_id);
CREATE INDEX idx_content_reports_itinerary ON public.content_reports USING btree (itinerary_id);
CREATE INDEX idx_content_reports_pending ON public.content_reports USING btree (created_at, id) WHERE (status = 'PENDING'::text);
CREATE INDEX idx_content_reports_reporter ON public.content_reports USING btree (reporter_id);
CREATE UNIQUE INDEX uq_content_reports_pending_comment ON public.content_reports USING btree (reporter_id, comment_id) WHERE ((status = 'PENDING'::text) AND (comment_id IS NOT NULL));
CREATE UNIQUE INDEX uq_content_reports_pending_itinerary ON public.content_reports USING btree (reporter_id, itinerary_id) WHERE ((status = 'PENDING'::text) AND (itinerary_id IS NOT NULL));
```

## destinations

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `name` | `text` | Não | `—` |
| `region` | `text` | Sim | `—` |
| `country_code` | `character(2)` | Não | `—` |
| `timezone` | `text` | Não | `—` |
| `latitude` | `numeric(9,6)` | Sim | `—` |
| `longitude` | `numeric(9,6)` | Sim | `—` |
| `source` | `text` | Não | `—` |
| `external_id` | `text` | Sim | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_destinations_coordinates: CHECK ((((latitude IS NULL) AND (longitude IS NULL)) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL) AND ((latitude >= ('-90'::integer)::numeric) AND (latitude <= (90)::numeric)) AND ((longitude >= ('-180'::integer)::numeric) AND (longitude <= (180)::numeric)))));
ck_destinations_country: CHECK ((country_code ~ '^[A-Z]{2}$'::text));
ck_destinations_name: CHECK ((btrim(name) <> ''::text));
ck_destinations_source: CHECK ((btrim(source) <> ''::text));
ck_destinations_timezone: CHECK ((btrim(timezone) <> ''::text));
pk_destinations: PRIMARY KEY (id);
uq_destinations_external: UNIQUE (source, external_id);
```

Índices adicionais:

```sql
CREATE INDEX idx_destinations_name ON public.destinations USING btree (name);
```

## itineraries

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `trip_id` | `uuid` | Não | `—` |
| `title` | `text` | Não | `—` |
| `description` | `text` | Sim | `—` |
| `category_code` | `text` | Sim | `—` |
| `origin` | `text` | Não | `'MANUAL'::text` |
| `visibility` | `text` | Não | `'PRIVATE'::text` |
| `hidden_by_moderation` | `boolean` | Não | `false` |
| `published_at` | `timestamp with time zone` | Sim | `—` |
| `source_itinerary_id` | `uuid` | Sim | `—` |
| `source_author_name` | `text` | Sim | `—` |
| `source_title` | `text` | Sim | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `version` | `bigint` | Não | `0` |

Constraints:

```sql
ck_itineraries_attribution: CHECK (((origin <> 'COPIED'::text) OR ((source_author_name IS NOT NULL) AND (btrim(source_author_name) <> ''::text) AND (source_title IS NOT NULL) AND (btrim(source_title) <> ''::text))));
ck_itineraries_origin: CHECK ((origin = ANY (ARRAY['MANUAL'::text, 'AUTOMATIC'::text, 'COPIED'::text])));
ck_itineraries_publication: CHECK (((visibility = 'PRIVATE'::text) OR ((category_code IS NOT NULL) AND (description IS NOT NULL) AND (btrim(description) <> ''::text) AND (published_at IS NOT NULL))));
ck_itineraries_source: CHECK (((source_itinerary_id IS NULL) OR (source_itinerary_id <> id)));
ck_itineraries_title: CHECK ((btrim(title) <> ''::text));
ck_itineraries_visibility: CHECK ((visibility = ANY (ARRAY['PRIVATE'::text, 'PUBLIC'::text])));
fk_itineraries_category: FOREIGN KEY (category_code) REFERENCES categories(code);
fk_itineraries_source: FOREIGN KEY (source_itinerary_id) REFERENCES itineraries(id) ON DELETE SET NULL;
fk_itineraries_trip: FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;
pk_itineraries: PRIMARY KEY (id);
trg_itineraries_public_activity: TRIGGER DEFERRABLE INITIALLY DEFERRED;
```

Índices adicionais:

```sql
CREATE INDEX idx_itineraries_category_publication ON public.itineraries USING btree (category_code, published_at DESC, id);
CREATE INDEX idx_itineraries_publication ON public.itineraries USING btree (published_at DESC, id) WHERE ((visibility = 'PUBLIC'::text) AND (NOT hidden_by_moderation));
CREATE INDEX idx_itineraries_source ON public.itineraries USING btree (source_itinerary_id);
CREATE INDEX idx_itineraries_trip ON public.itineraries USING btree (trip_id);
```

## itinerary_activities

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `itinerary_id` | `uuid` | Não | `—` |
| `trip_day_id` | `uuid` | Sim | `—` |
| `place_id` | `uuid` | Sim | `—` |
| `name` | `text` | Não | `—` |
| `location` | `text` | Sim | `—` |
| `latitude` | `numeric(9,6)` | Sim | `—` |
| `longitude` | `numeric(9,6)` | Sim | `—` |
| `position` | `integer` | Sim | `—` |
| `starts_at` | `time without time zone` | Sim | `—` |
| `duration_minutes` | `integer` | Sim | `—` |
| `notes` | `text` | Sim | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_itinerary_activities_allocation: CHECK ((((trip_day_id IS NULL) AND ("position" IS NULL) AND (starts_at IS NULL)) OR ((trip_day_id IS NOT NULL) AND ("position" IS NOT NULL) AND ("position" > 0))));
ck_itinerary_activities_coordinates: CHECK ((((latitude IS NULL) AND (longitude IS NULL)) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL) AND ((latitude >= ('-90'::integer)::numeric) AND (latitude <= (90)::numeric)) AND ((longitude >= ('-180'::integer)::numeric) AND (longitude <= (180)::numeric)))));
ck_itinerary_activities_duration: CHECK ((duration_minutes > 0));
ck_itinerary_activities_name: CHECK ((btrim(name) <> ''::text));
fk_itinerary_activities_day: FOREIGN KEY (trip_day_id) REFERENCES trip_days(id) DEFERRABLE INITIALLY DEFERRED;
fk_itinerary_activities_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_itinerary_activities_place: FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE SET NULL;
pk_itinerary_activities: PRIMARY KEY (id);
trg_activities_public_activity: TRIGGER DEFERRABLE INITIALLY DEFERRED;
uq_itinerary_activities_position: UNIQUE (itinerary_id, trip_day_id, "position") DEFERRABLE;
```

Índices adicionais:

```sql
CREATE INDEX idx_itinerary_activities_day ON public.itinerary_activities USING btree (trip_day_id);
CREATE INDEX idx_itinerary_activities_place ON public.itinerary_activities USING btree (place_id);
```

## itinerary_comments

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `user_id` | `uuid` | Não | `—` |
| `itinerary_id` | `uuid` | Não | `—` |
| `body` | `character varying(1000)` | Não | `—` |
| `hidden_by_moderation` | `boolean` | Não | `false` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `edited_at` | `timestamp with time zone` | Sim | `—` |

Constraints:

```sql
ck_itinerary_comments_body: CHECK ((btrim((body)::text) <> ''::text));
fk_itinerary_comments_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_itinerary_comments_user: FOREIGN KEY (user_id) REFERENCES users(id);
pk_itinerary_comments: PRIMARY KEY (id);
```

Índices adicionais:

```sql
CREATE INDEX idx_itinerary_comments_itinerary ON public.itinerary_comments USING btree (itinerary_id, created_at, id);
CREATE INDEX idx_itinerary_comments_user ON public.itinerary_comments USING btree (user_id);
```

## itinerary_favorites

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `user_id` | `uuid` | Não | `—` |
| `itinerary_id` | `uuid` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
fk_itinerary_favorites_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_itinerary_favorites_user: FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
pk_itinerary_favorites: PRIMARY KEY (user_id, itinerary_id);
```

Índices adicionais:

```sql
CREATE INDEX idx_itinerary_favorites_itinerary ON public.itinerary_favorites USING btree (itinerary_id);
```

## itinerary_images

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `itinerary_id` | `uuid` | Não | `—` |
| `object_key` | `text` | Não | `—` |
| `content_type` | `text` | Não | `—` |
| `size_bytes` | `integer` | Não | `—` |
| `caption` | `text` | Sim | `—` |
| `alt_text` | `text` | Não | `—` |
| `position` | `integer` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_itinerary_images_alt: CHECK ((btrim(alt_text) <> ''::text));
ck_itinerary_images_object_key: CHECK ((btrim(object_key) <> ''::text));
ck_itinerary_images_position: CHECK (("position" > 0));
ck_itinerary_images_size: CHECK (((size_bytes >= 1) AND (size_bytes <= 5000000)));
ck_itinerary_images_type: CHECK ((content_type = ANY (ARRAY['image/jpeg'::text, 'image/png'::text])));
fk_itinerary_images_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
pk_itinerary_images: PRIMARY KEY (id);
uq_itinerary_images_object_key: UNIQUE (object_key);
uq_itinerary_images_position: UNIQUE (itinerary_id, "position");
```

## itinerary_legs

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `from_activity_id` | `uuid` | Não | `—` |
| `to_activity_id` | `uuid` | Não | `—` |
| `transport_mode_code` | `text` | Não | `—` |
| `calculation_status` | `text` | Não | `'PENDING'::text` |
| `distance_meters` | `numeric(12,2)` | Sim | `—` |
| `duration_seconds` | `integer` | Sim | `—` |
| `source` | `text` | Sim | `—` |
| `calculated_at` | `timestamp with time zone` | Sim | `—` |
| `route_geometry` | `text` | Sim | `—` |

Constraints:

```sql
ck_itinerary_legs_distinct: CHECK ((from_activity_id <> to_activity_id));
ck_itinerary_legs_estimates: CHECK ((((calculation_status = 'CALCULATED'::text) AND (distance_meters IS NOT NULL) AND (distance_meters >= (0)::numeric) AND (duration_seconds IS NOT NULL) AND (duration_seconds >= 0) AND (calculated_at IS NOT NULL) AND (source IS NOT NULL) AND (btrim(source) <> ''::text)) OR ((calculation_status <> 'CALCULATED'::text) AND (distance_meters IS NULL) AND (duration_seconds IS NULL))));
ck_itinerary_legs_status: CHECK ((calculation_status = ANY (ARRAY['PENDING'::text, 'CALCULATED'::text, 'UNAVAILABLE'::text])));
fk_itinerary_legs_from: FOREIGN KEY (from_activity_id) REFERENCES itinerary_activities(id) ON DELETE CASCADE;
fk_itinerary_legs_to: FOREIGN KEY (to_activity_id) REFERENCES itinerary_activities(id) ON DELETE CASCADE;
fk_itinerary_legs_transport: FOREIGN KEY (transport_mode_code) REFERENCES transport_modes(code);
pk_itinerary_legs: PRIMARY KEY (id);
uq_itinerary_legs_pair: UNIQUE (from_activity_id, to_activity_id, transport_mode_code);
```

Índices adicionais:

```sql
CREATE INDEX idx_itinerary_legs_to ON public.itinerary_legs USING btree (to_activity_id);
CREATE INDEX idx_itinerary_legs_transport ON public.itinerary_legs USING btree (transport_mode_code);
```

## itinerary_likes

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `user_id` | `uuid` | Não | `—` |
| `itinerary_id` | `uuid` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
fk_itinerary_likes_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_itinerary_likes_user: FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
pk_itinerary_likes: PRIMARY KEY (user_id, itinerary_id);
```

Índices adicionais:

```sql
CREATE INDEX idx_itinerary_likes_itinerary ON public.itinerary_likes USING btree (itinerary_id);
```

## itinerary_ratings

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `user_id` | `uuid` | Não | `—` |
| `itinerary_id` | `uuid` | Não | `—` |
| `rating` | `smallint` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_itinerary_ratings_rating: CHECK (((rating >= 1) AND (rating <= 5)));
fk_itinerary_ratings_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_itinerary_ratings_user: FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
pk_itinerary_ratings: PRIMARY KEY (user_id, itinerary_id);
```

Índices adicionais:

```sql
CREATE INDEX idx_itinerary_ratings_itinerary ON public.itinerary_ratings USING btree (itinerary_id);
```

## itinerary_votes

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `user_id` | `uuid` | Não | `—` |
| `itinerary_id` | `uuid` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
fk_itinerary_votes_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_itinerary_votes_user: FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
pk_itinerary_votes: PRIMARY KEY (user_id, itinerary_id);
```

Índices adicionais:

```sql
CREATE INDEX idx_itinerary_votes_itinerary ON public.itinerary_votes USING btree (itinerary_id);
```

## moderation_actions

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `administrator_id` | `uuid` | Não | `—` |
| `report_id` | `uuid` | Sim | `—` |
| `previous_action_id` | `uuid` | Sim | `—` |
| `action` | `text` | Não | `—` |
| `target_type` | `text` | Não | `—` |
| `target_id` | `uuid` | Não | `—` |
| `target_snapshot` | `jsonb` | Não | `—` |
| `justification` | `text` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_moderation_actions_action: CHECK ((action = ANY (ARRAY['DISMISS'::text, 'HIDE'::text, 'SUSPEND'::text, 'RESTORE'::text])));
ck_moderation_actions_justification: CHECK ((btrim(justification) <> ''::text));
ck_moderation_actions_previous: CHECK (((previous_action_id IS NULL) OR (previous_action_id <> id)));
ck_moderation_actions_snapshot: CHECK ((jsonb_typeof(target_snapshot) = 'object'::text));
ck_moderation_actions_type: CHECK ((target_type = ANY (ARRAY['ITINERARY'::text, 'COMMENT'::text, 'USER'::text])));
fk_moderation_actions_admin: FOREIGN KEY (administrator_id) REFERENCES users(id);
fk_moderation_actions_previous: FOREIGN KEY (previous_action_id) REFERENCES moderation_actions(id);
fk_moderation_actions_report: FOREIGN KEY (report_id) REFERENCES content_reports(id) ON DELETE SET NULL;
pk_moderation_actions: PRIMARY KEY (id);
```

Índices adicionais:

```sql
CREATE INDEX idx_moderation_actions_admin ON public.moderation_actions USING btree (administrator_id);
CREATE INDEX idx_moderation_actions_previous ON public.moderation_actions USING btree (previous_action_id);
CREATE INDEX idx_moderation_actions_report ON public.moderation_actions USING btree (report_id);
CREATE INDEX idx_moderation_actions_target ON public.moderation_actions USING btree (target_type, target_id, created_at);
```

## notifications

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `recipient_id` | `uuid` | Não | `—` |
| `actor_id` | `uuid` | Não | `—` |
| `itinerary_id` | `uuid` | Não | `—` |
| `comment_id` | `uuid` | Sim | `—` |
| `type` | `text` | Não | `—` |
| `event_key` | `text` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `read_at` | `timestamp with time zone` | Sim | `—` |

Constraints:

```sql
ck_notifications_distinct: CHECK ((recipient_id <> actor_id));
ck_notifications_event_key: CHECK ((btrim(event_key) <> ''::text));
ck_notifications_read: CHECK (((read_at IS NULL) OR (read_at >= created_at)));
ck_notifications_type: CHECK ((type = ANY (ARRAY['LIKE'::text, 'COMMENT'::text, 'VOTE'::text, 'RATING'::text])));
fk_notifications_actor: FOREIGN KEY (actor_id) REFERENCES users(id);
fk_notifications_comment: FOREIGN KEY (comment_id) REFERENCES itinerary_comments(id) ON DELETE SET NULL;
fk_notifications_itinerary: FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE;
fk_notifications_recipient: FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE;
pk_notifications: PRIMARY KEY (id);
uq_notifications_event_key: UNIQUE (event_key);
```

Índices adicionais:

```sql
CREATE INDEX idx_notifications_actor ON public.notifications USING btree (actor_id);
CREATE INDEX idx_notifications_comment ON public.notifications USING btree (comment_id);
CREATE INDEX idx_notifications_itinerary ON public.notifications USING btree (itinerary_id);
CREATE INDEX idx_notifications_recipient ON public.notifications USING btree (recipient_id, created_at DESC, id);
```

## password_reset_tokens

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `user_id` | `uuid` | Não | `—` |
| `token_hash` | `character varying(64)` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `expires_at` | `timestamp with time zone` | Não | `(CURRENT_TIMESTAMP + '00:30:00'::interval)` |
| `used_at` | `timestamp with time zone` | Sim | `—` |

Constraints:

```sql
ck_password_reset_tokens_expiry: CHECK (((expires_at > created_at) AND (expires_at <= (created_at + '00:30:00'::interval))));
ck_password_reset_tokens_hash: CHECK (((token_hash)::text ~ '^[0-9a-f]{64}$'::text));
ck_password_reset_tokens_used: CHECK (((used_at IS NULL) OR (used_at >= created_at)));
fk_password_reset_tokens_user: FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
pk_password_reset_tokens: PRIMARY KEY (id);
uq_password_reset_tokens_hash: UNIQUE (token_hash);
```

Índices adicionais:

```sql
CREATE INDEX idx_password_reset_tokens_user ON public.password_reset_tokens USING btree (user_id, expires_at);
```

## place_categories

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `place_id` | `uuid` | Não | `—` |
| `category_code` | `text` | Não | `—` |

Constraints:

```sql
fk_place_categories_category: FOREIGN KEY (category_code) REFERENCES categories(code);
fk_place_categories_place: FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
pk_place_categories: PRIMARY KEY (place_id, category_code);
```

Índices adicionais:

```sql
CREATE INDEX idx_place_categories_category ON public.place_categories USING btree (category_code, place_id);
```

## place_images

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `place_id` | `uuid` | Não | `—` |
| `url` | `text` | Não | `—` |
| `alt_text` | `text` | Sim | `—` |
| `source` | `text` | Não | `—` |
| `position` | `integer` | Não | `—` |

Constraints:

```sql
ck_place_images_position: CHECK (("position" > 0));
ck_place_images_source: CHECK ((btrim(source) <> ''::text));
ck_place_images_url: CHECK ((btrim(url) <> ''::text));
fk_place_images_place: FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
pk_place_images: PRIMARY KEY (id);
uq_place_images_position: UNIQUE (place_id, "position");
```

## place_opening_hours

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `place_id` | `uuid` | Não | `—` |
| `weekday` | `smallint` | Não | `—` |
| `opens_at` | `time without time zone` | Não | `—` |
| `closes_at` | `time without time zone` | Não | `—` |
| `closes_next_day` | `boolean` | Não | `false` |

Constraints:

```sql
ck_place_opening_hours_interval: CHECK ((closes_next_day OR (opens_at < closes_at)));
ck_place_opening_hours_weekday: CHECK (((weekday >= 1) AND (weekday <= 7)));
fk_place_opening_hours_place: FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
pk_place_opening_hours: PRIMARY KEY (id);
uq_place_opening_hours_period: UNIQUE (place_id, weekday, opens_at);
```

## places

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `destination_id` | `uuid` | Não | `—` |
| `name` | `text` | Não | `—` |
| `description` | `text` | Sim | `—` |
| `address` | `text` | Sim | `—` |
| `neighborhood` | `text` | Sim | `—` |
| `latitude` | `numeric(9,6)` | Sim | `—` |
| `longitude` | `numeric(9,6)` | Sim | `—` |
| `estimated_visit_minutes` | `integer` | Sim | `—` |
| `estimated_cost` | `numeric(12,2)` | Sim | `—` |
| `currency` | `character(3)` | Sim | `—` |
| `source` | `text` | Não | `—` |
| `external_id` | `text` | Sim | `—` |
| `source_url` | `text` | Sim | `—` |
| `source_updated_at` | `timestamp with time zone` | Sim | `—` |
| `removed_at` | `timestamp with time zone` | Sim | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_places_coordinates: CHECK ((((latitude IS NULL) AND (longitude IS NULL)) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL) AND ((latitude >= ('-90'::integer)::numeric) AND (latitude <= (90)::numeric)) AND ((longitude >= ('-180'::integer)::numeric) AND (longitude <= (180)::numeric)))));
ck_places_cost: CHECK ((((estimated_cost IS NULL) AND (currency IS NULL)) OR ((estimated_cost IS NOT NULL) AND (estimated_cost >= (0)::numeric) AND (currency IS NOT NULL) AND (currency ~ '^[A-Z]{3}$'::text))));
ck_places_duration: CHECK ((estimated_visit_minutes > 0));
ck_places_name: CHECK ((btrim(name) <> ''::text));
ck_places_source: CHECK ((btrim(source) <> ''::text));
fk_places_destination: FOREIGN KEY (destination_id) REFERENCES destinations(id);
pk_places: PRIMARY KEY (id);
uq_places_external: UNIQUE (source, external_id);
```

Índices adicionais:

```sql
CREATE INDEX idx_places_destination_name ON public.places USING btree (destination_id, name);
```

## transport_modes

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `code` | `text` | Não | `—` |
| `name` | `text` | Não | `—` |

Constraints:

```sql
ck_transport_modes_name: CHECK ((btrim(name) <> ''::text));
pk_transport_modes: PRIMARY KEY (code);
```

## travel_paces

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `code` | `text` | Não | `—` |
| `name` | `text` | Não | `—` |

Constraints:

```sql
ck_travel_paces_name: CHECK ((btrim(name) <> ''::text));
pk_travel_paces: PRIMARY KEY (code);
```

## trip_days

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `trip_id` | `uuid` | Não | `—` |
| `day_number` | `integer` | Não | `—` |
| `available_from` | `time without time zone` | Sim | `—` |
| `available_until` | `time without time zone` | Sim | `—` |

Constraints:

```sql
ck_trip_days_interval: CHECK ((((available_from IS NULL) AND (available_until IS NULL)) OR ((available_from IS NOT NULL) AND (available_until IS NOT NULL) AND (available_from < available_until))));
ck_trip_days_number: CHECK ((day_number > 0));
fk_trip_days_trip: FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;
pk_trip_days: PRIMARY KEY (id);
uq_trip_days_number: UNIQUE (trip_id, day_number);
```

## trip_interests

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `trip_id` | `uuid` | Não | `—` |
| `category_code` | `text` | Não | `—` |

Constraints:

```sql
fk_trip_interests_category: FOREIGN KEY (category_code) REFERENCES categories(code);
fk_trip_interests_trip: FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;
pk_trip_interests: PRIMARY KEY (trip_id, category_code);
```

Índices adicionais:

```sql
CREATE INDEX idx_trip_interests_category ON public.trip_interests USING btree (category_code, trip_id);
```

## trips

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `user_id` | `uuid` | Não | `—` |
| `destination_id` | `uuid` | Não | `—` |
| `title` | `text` | Não | `—` |
| `starts_on` | `date` | Não | `—` |
| `ends_on` | `date` | Não | `—` |
| `duration_days` | `integer` | Sim | `((ends_on - starts_on) + 1)` |
| `pace_code` | `text` | Sim | `—` |
| `transport_mode_code` | `text` | Sim | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `version` | `bigint` | Não | `0` |

Constraints:

```sql
ck_trips_period: CHECK ((ends_on >= starts_on));
ck_trips_title: CHECK ((btrim(title) <> ''::text));
fk_trips_destination: FOREIGN KEY (destination_id) REFERENCES destinations(id);
fk_trips_pace: FOREIGN KEY (pace_code) REFERENCES travel_paces(code);
fk_trips_transport: FOREIGN KEY (transport_mode_code) REFERENCES transport_modes(code);
fk_trips_user: FOREIGN KEY (user_id) REFERENCES users(id);
pk_trips: PRIMARY KEY (id);
```

Índices adicionais:

```sql
CREATE INDEX idx_trips_destination ON public.trips USING btree (destination_id);
CREATE INDEX idx_trips_pace ON public.trips USING btree (pace_code);
CREATE INDEX idx_trips_transport ON public.trips USING btree (transport_mode_code);
CREATE INDEX idx_trips_user_period ON public.trips USING btree (user_id, starts_on DESC, id);
```

## user_follows

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `follower_id` | `uuid` | Não | `—` |
| `followed_id` | `uuid` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

Constraints:

```sql
ck_user_follows_distinct: CHECK ((follower_id <> followed_id));
fk_user_follows_followed: FOREIGN KEY (followed_id) REFERENCES users(id) ON DELETE CASCADE;
fk_user_follows_follower: FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE;
pk_user_follows: PRIMARY KEY (follower_id, followed_id);
```

Índices adicionais:

```sql
CREATE INDEX idx_user_follows_followed ON public.user_follows USING btree (followed_id, follower_id);
```

## user_sessions

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `user_id` | `uuid` | Não | `—` |
| `token_hash` | `character varying(64)` | Não | `—` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `expires_at` | `timestamp with time zone` | Não | `—` |
| `revoked_at` | `timestamp with time zone` | Sim | `—` |

Constraints:

```sql
ck_user_sessions_expiry: CHECK ((expires_at > created_at));
ck_user_sessions_revoked: CHECK (((revoked_at IS NULL) OR (revoked_at >= created_at)));
ck_user_sessions_token_hash: CHECK (((token_hash)::text ~ '^[0-9a-f]{64}$'::text));
fk_user_sessions_user: FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
pk_user_sessions: PRIMARY KEY (id);
uq_user_sessions_token_hash: UNIQUE (token_hash);
```

Índices adicionais:

```sql
CREATE INDEX idx_user_sessions_user ON public.user_sessions USING btree (user_id, expires_at);
```

## users

| Coluna | Tipo | Aceita NULL | Padrão / expressão gerada |
|---|---|---|---|
| `id` | `uuid` | Não | `gen_random_uuid()` |
| `username` | `character varying(50)` | Não | `—` |
| `name` | `character varying(120)` | Não | `—` |
| `email` | `character varying(254)` | Não | `—` |
| `phone` | `character varying(32)` | Sim | `—` |
| `password_hash` | `text` | Não | `—` |
| `bio` | `text` | Sim | `—` |
| `hometown` | `text` | Sim | `—` |
| `role` | `text` | Não | `'USER'::text` |
| `status` | `text` | Não | `'ACTIVE'::text` |
| `created_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `version` | `bigint` | Não | `0` |

Constraints:

```sql
ck_users_email: CHECK (((btrim((email)::text) <> ''::text) AND ((email)::text = btrim((email)::text))));
ck_users_name: CHECK ((btrim((name)::text) <> ''::text));
ck_users_password_hash: CHECK ((length(password_hash) >= 60));
ck_users_role: CHECK ((role = ANY (ARRAY['USER'::text, 'ADMIN'::text])));
ck_users_status: CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'SUSPENDED'::text])));
ck_users_username: CHECK ((btrim((username)::text) <> ''::text));
pk_users: PRIMARY KEY (id);
```

Índices adicionais:

```sql
CREATE UNIQUE INDEX uq_users_email ON public.users USING btree (lower((email)::text));
CREATE UNIQUE INDEX uq_users_username ON public.users USING btree (lower((username)::text));
```
