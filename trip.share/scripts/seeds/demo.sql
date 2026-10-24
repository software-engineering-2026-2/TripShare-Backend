-- Development fixtures only. No real destinations, venues or personal data.
BEGIN;
SELECT pg_advisory_xact_lock(782349102);

-- These UIDs are fixtures, not Firebase accounts and do not enable real login.
INSERT INTO auth (id, uuid) VALUES
    ('01000000-0000-0000-0000-000000000001', 'demo-only-alex'),
    ('01000000-0000-0000-0000-000000000002', 'demo-only-sam'),
    ('01000000-0000-0000-0000-000000000003', 'demo-only-admin')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, auth_id, username, name, email, role) VALUES
    ('10000000-0000-0000-0000-000000000001', '01000000-0000-0000-0000-000000000001',
     'demo_alex', 'Demo Alex', 'alex@example.invalid', 'USER'),
    ('10000000-0000-0000-0000-000000000002', '01000000-0000-0000-0000-000000000002',
     'demo_sam', 'Demo Sam', 'sam@example.invalid', 'USER'),
    ('10000000-0000-0000-0000-000000000003', '01000000-0000-0000-0000-000000000003',
     'demo_admin', 'Demo Administrator', 'admin@example.invalid', 'ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO destinations (id, name, region, country_code, timezone, source, external_id) VALUES
    ('20000000-0000-0000-0000-000000000001', 'Demo City', 'Fictional destination', 'BR',
     'America/Fortaleza', 'DEMO', 'demo-city') ON CONFLICT DO NOTHING;

INSERT INTO places (id, destination_id, name, description, source, external_id, estimated_visit_minutes) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'Demo Museum', 'Fictional venue for development only. Location and costs are unknown.', 'DEMO', 'demo-museum', 60),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'Demo Landmark', 'Fictional venue for development only. Location and costs are unknown.', 'DEMO', 'demo-landmark', 45)
ON CONFLICT DO NOTHING;

INSERT INTO place_categories (place_id, category_code) VALUES
    ('30000000-0000-0000-0000-000000000001', 'MUSEUM'),
    ('30000000-0000-0000-0000-000000000002', 'LANDMARK') ON CONFLICT DO NOTHING;

INSERT INTO trips (id, user_id, destination_id, title, starts_on, ends_on, pace_code, transport_mode_code) VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', 'Demo museum trip', '2030-01-10', '2030-01-12', 'RELAXED', 'WALKING'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000001', 'Demo copied trip', '2030-02-10', '2030-02-12', 'MODERATE', 'WALKING')
ON CONFLICT DO NOTHING;

INSERT INTO trip_interests (trip_id, category_code) VALUES
    ('40000000-0000-0000-0000-000000000001', 'MUSEUM'),
    ('40000000-0000-0000-0000-000000000002', 'MUSEUM') ON CONFLICT DO NOTHING;

INSERT INTO trip_days (id, trip_id, day_number, available_from, available_until) VALUES
    ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 1, '09:00', '18:00'),
    ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 2, '09:00', '18:00'),
    ('50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 3, '09:00', '18:00'),
    ('50000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000002', 1, '09:00', '18:00')
ON CONFLICT DO NOTHING;

INSERT INTO itineraries (id, trip_id, title, description, category_code, visibility, published_at) VALUES
    ('60000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001',
     'Demo public museum route', 'A fictional itinerary for development.', 'MUSEUM', 'PUBLIC', '2026-01-01T12:00:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO itineraries (id, trip_id, title, description, category_code, origin,
    source_itinerary_id, source_author_name, source_title) VALUES
    ('60000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002',
     'Demo private copy', 'An independent private copy.', 'MUSEUM', 'COPIED',
     '60000000-0000-0000-0000-000000000001', 'Demo Alex', 'Demo public museum route') ON CONFLICT DO NOTHING;

INSERT INTO itinerary_activities (id, itinerary_id, trip_day_id, place_id, name, position, starts_at, duration_minutes, notes) VALUES
    ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001',
     '50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'Demo Museum', 1, '10:00', 60,
     'Demo data. Coordinates and travel estimates are pending.'),
    ('70000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001',
     '50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 'Demo Landmark', 2, '12:00', 45,
     'Demo data. Coordinates and travel estimates are pending.'),
    ('70000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000002',
     '50000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', 'Demo Museum', 1, '10:00', 60,
     'Private independent copy.') ON CONFLICT (id) DO NOTHING;

INSERT INTO itinerary_likes (user_id, itinerary_id) VALUES
    ('10000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;
INSERT INTO itinerary_votes (user_id, itinerary_id) VALUES
    ('10000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;
INSERT INTO itinerary_ratings (user_id, itinerary_id, rating) VALUES
    ('10000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', 5) ON CONFLICT DO NOTHING;
INSERT INTO itinerary_favorites (user_id, itinerary_id) VALUES
    ('10000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;
INSERT INTO user_follows (follower_id, followed_id) VALUES
    ('10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO itinerary_comments (id, user_id, itinerary_id, body) VALUES
    ('80000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002',
     '60000000-0000-0000-0000-000000000001', 'Demo comment for development.') ON CONFLICT DO NOTHING;

INSERT INTO notifications (id, recipient_id, actor_id, itinerary_id, comment_id, type, event_key) VALUES
    ('90000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001',
     '80000000-0000-0000-0000-000000000001', 'COMMENT', 'demo:comment:1') ON CONFLICT DO NOTHING;

INSERT INTO content_reports (id, reporter_id, itinerary_id, reason, details, status) VALUES
    ('a0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002',
     '60000000-0000-0000-0000-000000000001', 'OTHER', 'Demo moderation workflow.', 'DISMISSED') ON CONFLICT DO NOTHING;

INSERT INTO moderation_actions (id, administrator_id, report_id, action, target_type, target_id, target_snapshot, justification) VALUES
    ('b0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003',
     'a0000000-0000-0000-0000-000000000001', 'DISMISS', 'ITINERARY', '60000000-0000-0000-0000-000000000001',
     '{"title":"Demo public museum route"}', 'Demonstration only; no real report.') ON CONFLICT DO NOTHING;
COMMIT;
