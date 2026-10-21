CREATE FUNCTION touch_updated_at() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

DO $$
DECLARE table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['users', 'destinations', 'places', 'trips', 'itineraries',
        'itinerary_activities', 'itinerary_ratings', 'itinerary_comments', 'content_reports']
    LOOP
        EXECUTE format('CREATE TRIGGER %I BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION touch_updated_at()',
            'trg_' || table_name || '_updated_at', table_name);
    END LOOP;
END;
$$;

CREATE FUNCTION validate_trip_day() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE trip_length integer;
BEGIN
    SELECT duration_days INTO trip_length FROM trips WHERE id = NEW.trip_id FOR SHARE;
    IF NEW.day_number > trip_length THEN
        RAISE EXCEPTION 'Day % is outside the trip period', NEW.day_number USING ERRCODE = '23514';
    END IF;
    IF EXISTS (
        SELECT 1 FROM itinerary_activities a JOIN itineraries i ON i.id = a.itinerary_id
        WHERE a.trip_day_id = NEW.id AND i.trip_id <> NEW.trip_id
    ) THEN
        RAISE EXCEPTION 'Trip day cannot move away from its activities' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_trip_days_period BEFORE INSERT OR UPDATE ON trip_days
    FOR EACH ROW EXECUTE FUNCTION validate_trip_day();

CREATE FUNCTION validate_trip_period() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM trip_days WHERE trip_id = NEW.id AND day_number > NEW.ends_on - NEW.starts_on + 1) THEN
        RAISE EXCEPTION 'Reallocate activities and remove days outside the new period before shortening the trip'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_trips_period BEFORE UPDATE OF starts_on, ends_on ON trips
    FOR EACH ROW EXECUTE FUNCTION validate_trip_period();

CREATE FUNCTION validate_activity_day() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE itinerary_trip uuid; day_trip uuid;
BEGIN
    IF NEW.trip_day_id IS NOT NULL THEN
        SELECT trip_id INTO itinerary_trip FROM itineraries WHERE id = NEW.itinerary_id FOR SHARE;
        SELECT trip_id INTO day_trip FROM trip_days WHERE id = NEW.trip_day_id FOR SHARE;
        IF itinerary_trip IS DISTINCT FROM day_trip THEN
            RAISE EXCEPTION 'Activity day must belong to the itinerary trip' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_itinerary_activities_day BEFORE INSERT OR UPDATE ON itinerary_activities
    FOR EACH ROW EXECUTE FUNCTION validate_activity_day();

CREATE FUNCTION validate_itinerary_trip() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM itinerary_activities a JOIN trip_days d ON d.id = a.trip_day_id
        WHERE a.itinerary_id = NEW.id AND d.trip_id <> NEW.trip_id) THEN
        RAISE EXCEPTION 'Itinerary cannot move away from its allocated days' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_itineraries_trip BEFORE UPDATE OF trip_id ON itineraries
    FOR EACH ROW EXECUTE FUNCTION validate_itinerary_trip();

CREATE FUNCTION validate_leg_activities() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE first_activity record; second_activity record;
BEGIN
    SELECT itinerary_id, trip_day_id, position INTO first_activity FROM itinerary_activities WHERE id = NEW.from_activity_id FOR SHARE;
    SELECT itinerary_id, trip_day_id, position INTO second_activity FROM itinerary_activities WHERE id = NEW.to_activity_id FOR SHARE;
    IF first_activity.itinerary_id IS DISTINCT FROM second_activity.itinerary_id
        OR first_activity.trip_day_id IS NULL
        OR first_activity.trip_day_id IS DISTINCT FROM second_activity.trip_day_id
        OR first_activity.position >= second_activity.position
        OR EXISTS (SELECT 1 FROM itinerary_activities WHERE itinerary_id = first_activity.itinerary_id
            AND trip_day_id = first_activity.trip_day_id
            AND position > first_activity.position AND position < second_activity.position) THEN
        RAISE EXCEPTION 'A leg must join consecutive allocated activities of the same itinerary day' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_itinerary_legs_activities BEFORE INSERT OR UPDATE ON itinerary_legs
    FOR EACH ROW EXECUTE FUNCTION validate_leg_activities();

CREATE FUNCTION invalidate_itinerary_legs() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'INSERT' THEN
        DELETE FROM itinerary_legs WHERE from_activity_id IN (
            SELECT id FROM itinerary_activities WHERE itinerary_id = OLD.itinerary_id);
    END IF;
    IF TG_OP <> 'DELETE' THEN
        DELETE FROM itinerary_legs WHERE from_activity_id IN (
            SELECT id FROM itinerary_activities WHERE itinerary_id = NEW.itinerary_id);
    END IF;
    RETURN NULL;
END;
$$;
CREATE TRIGGER trg_itinerary_activities_invalidate_legs
    AFTER INSERT OR DELETE OR UPDATE OF itinerary_id, trip_day_id, position, starts_at,
        duration_minutes, location, latitude, longitude, place_id ON itinerary_activities
    FOR EACH ROW EXECUTE FUNCTION invalidate_itinerary_legs();

CREATE FUNCTION validate_public_itinerary() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE itinerary_id_to_check uuid;
BEGIN
    IF TG_TABLE_NAME = 'itineraries' THEN
        itinerary_id_to_check := NEW.id;
    ELSE
        IF TG_OP <> 'INSERT' THEN
            IF EXISTS (SELECT 1 FROM itineraries WHERE id = OLD.itinerary_id AND visibility = 'PUBLIC')
                AND NOT EXISTS (SELECT 1 FROM itinerary_activities WHERE itinerary_id = OLD.itinerary_id
                    AND (place_id IS NOT NULL OR nullif(btrim(location), '') IS NOT NULL OR latitude IS NOT NULL)) THEN
                RAISE EXCEPTION 'A public itinerary needs an activity with an identified location' USING ERRCODE = '23514';
            END IF;
        END IF;
        IF TG_OP = 'DELETE' THEN RETURN NULL; END IF;
        itinerary_id_to_check := NEW.itinerary_id;
    END IF;
    IF EXISTS (SELECT 1 FROM itineraries WHERE id = itinerary_id_to_check AND visibility = 'PUBLIC')
        AND NOT EXISTS (SELECT 1 FROM itinerary_activities WHERE itinerary_id = itinerary_id_to_check
            AND (place_id IS NOT NULL OR nullif(btrim(location), '') IS NOT NULL OR latitude IS NOT NULL)) THEN
        RAISE EXCEPTION 'A public itinerary needs an activity with an identified location' USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$;
-- Deferred so publication and activity edits can be saved atomically in either order.
CREATE CONSTRAINT TRIGGER trg_itineraries_public_activity AFTER INSERT OR UPDATE ON itineraries
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION validate_public_itinerary();
CREATE CONSTRAINT TRIGGER trg_activities_public_activity AFTER INSERT OR UPDATE OR DELETE ON itinerary_activities
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION validate_public_itinerary();

CREATE FUNCTION mark_comment_edited() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.body IS DISTINCT FROM OLD.body THEN NEW.edited_at := CURRENT_TIMESTAMP; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_itinerary_comments_edited BEFORE UPDATE OF body ON itinerary_comments
    FOR EACH ROW EXECUTE FUNCTION mark_comment_edited();

CREATE INDEX idx_user_sessions_user ON user_sessions (user_id, expires_at);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id, expires_at);
CREATE INDEX idx_destinations_name ON destinations (name);
CREATE INDEX idx_places_destination_name ON places (destination_id, name);
CREATE INDEX idx_place_categories_category ON place_categories (category_code, place_id);
CREATE INDEX idx_trips_user_period ON trips (user_id, starts_on DESC, id);
CREATE INDEX idx_trips_destination ON trips (destination_id);
CREATE INDEX idx_trips_pace ON trips (pace_code);
CREATE INDEX idx_trips_transport ON trips (transport_mode_code);
CREATE INDEX idx_trip_interests_category ON trip_interests (category_code, trip_id);
CREATE INDEX idx_itineraries_trip ON itineraries (trip_id);
CREATE INDEX idx_itineraries_category_publication ON itineraries (category_code, published_at DESC, id);
CREATE INDEX idx_itineraries_publication ON itineraries (published_at DESC, id) WHERE visibility = 'PUBLIC' AND NOT hidden_by_moderation;
CREATE INDEX idx_itineraries_source ON itineraries (source_itinerary_id);
CREATE INDEX idx_itinerary_activities_day ON itinerary_activities (trip_day_id);
CREATE INDEX idx_itinerary_activities_place ON itinerary_activities (place_id);
CREATE INDEX idx_itinerary_legs_to ON itinerary_legs (to_activity_id);
CREATE INDEX idx_itinerary_legs_transport ON itinerary_legs (transport_mode_code);
CREATE INDEX idx_itinerary_likes_itinerary ON itinerary_likes (itinerary_id);
CREATE INDEX idx_itinerary_votes_itinerary ON itinerary_votes (itinerary_id);
CREATE INDEX idx_itinerary_favorites_itinerary ON itinerary_favorites (itinerary_id);
CREATE INDEX idx_itinerary_ratings_itinerary ON itinerary_ratings (itinerary_id);
CREATE INDEX idx_itinerary_comments_user ON itinerary_comments (user_id);
CREATE INDEX idx_itinerary_comments_itinerary ON itinerary_comments (itinerary_id, created_at, id);
CREATE INDEX idx_user_follows_followed ON user_follows (followed_id, follower_id);
CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at DESC, id);
CREATE INDEX idx_notifications_actor ON notifications (actor_id);
CREATE INDEX idx_notifications_itinerary ON notifications (itinerary_id);
CREATE INDEX idx_notifications_comment ON notifications (comment_id);
CREATE INDEX idx_content_reports_reporter ON content_reports (reporter_id);
CREATE INDEX idx_content_reports_itinerary ON content_reports (itinerary_id);
CREATE INDEX idx_content_reports_comment ON content_reports (comment_id);
CREATE INDEX idx_content_reports_pending ON content_reports (created_at, id) WHERE status = 'PENDING';
CREATE INDEX idx_moderation_actions_admin ON moderation_actions (administrator_id);
CREATE INDEX idx_moderation_actions_report ON moderation_actions (report_id);
CREATE INDEX idx_moderation_actions_previous ON moderation_actions (previous_action_id);
CREATE INDEX idx_moderation_actions_target ON moderation_actions (target_type, target_id, created_at);

CREATE VIEW public_profiles AS
    SELECT id, username, name, bio, hometown FROM users WHERE status = 'ACTIVE';

CREATE VIEW itinerary_metrics AS
SELECT i.id AS itinerary_id,
    coalesce(l.like_count, 0) AS like_count,
    coalesce(v.vote_count, 0) AS vote_count,
    r.average_rating,
    coalesce(r.rating_count, 0) AS rating_count
FROM itineraries i
LEFT JOIN (SELECT itinerary_id, count(*) AS like_count FROM itinerary_likes GROUP BY itinerary_id) l ON l.itinerary_id = i.id
LEFT JOIN (SELECT itinerary_id, count(*) AS vote_count FROM itinerary_votes GROUP BY itinerary_id) v ON v.itinerary_id = i.id
LEFT JOIN (SELECT itinerary_id, avg(rating) AS average_rating, count(*) AS rating_count
    FROM itinerary_ratings GROUP BY itinerary_id) r ON r.itinerary_id = i.id;

CREATE VIEW public_itineraries AS
SELECT i.id, i.title, i.description, i.category_code, i.origin, i.published_at,
    i.source_itinerary_id, i.source_author_name, i.source_title,
    u.id AS author_id, u.username AS author_username, u.name AS author_name,
    d.id AS destination_id, d.name AS destination_name, d.region, d.country_code,
    t.duration_days,
    m.like_count, m.vote_count, m.average_rating, m.rating_count
FROM itineraries i
JOIN trips t ON t.id = i.trip_id
JOIN users u ON u.id = t.user_id
JOIN destinations d ON d.id = t.destination_id
JOIN itinerary_metrics m ON m.itinerary_id = i.id
WHERE i.visibility = 'PUBLIC' AND NOT i.hidden_by_moderation AND u.status = 'ACTIVE';

CREATE VIEW public_itinerary_activities AS
SELECT a.id, a.itinerary_id, d.day_number, a.position, a.name, a.location,
    a.latitude, a.longitude, a.starts_at, a.duration_minutes, a.notes
FROM itinerary_activities a
JOIN public_itineraries i ON i.id = a.itinerary_id
LEFT JOIN trip_days d ON d.id = a.trip_day_id;

CREATE VIEW public_itinerary_comments AS
SELECT c.id, c.itinerary_id, u.id AS author_id, u.username AS author_username,
    u.name AS author_name, c.body, c.created_at, c.edited_at
FROM itinerary_comments c
JOIN public_itineraries i ON i.id = c.itinerary_id
JOIN users u ON u.id = c.user_id
WHERE NOT c.hidden_by_moderation AND u.status = 'ACTIVE';

CREATE VIEW public_itinerary_images AS
SELECT im.id, im.itinerary_id, im.object_key, im.content_type, im.size_bytes,
    im.caption, im.alt_text, im.position
FROM itinerary_images im JOIN public_itineraries i ON i.id = im.itinerary_id;

CREATE FUNCTION itinerary_ranking(filter_destination_id uuid DEFAULT NULL, filter_category_code text DEFAULT NULL)
RETURNS TABLE (ranking_position bigint, itinerary_id uuid, title text, destination_id uuid,
    category_code text, vote_count bigint, average_rating numeric, rating_count bigint)
LANGUAGE sql STABLE AS $$
    SELECT row_number() OVER (ORDER BY p.vote_count DESC, coalesce(p.average_rating, 0) DESC,
        p.published_at ASC, p.id ASC), p.id, p.title, p.destination_id,
        p.category_code, p.vote_count, p.average_rating, p.rating_count
    FROM public_itineraries p
    WHERE (filter_destination_id IS NULL OR p.destination_id = filter_destination_id)
        AND (filter_category_code IS NULL OR p.category_code = filter_category_code)
    ORDER BY p.vote_count DESC, coalesce(p.average_rating, 0) DESC, p.published_at ASC, p.id ASC;
$$;
