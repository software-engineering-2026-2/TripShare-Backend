CREATE TABLE itinerary_likes (
    user_id uuid NOT NULL CONSTRAINT fk_itinerary_likes_user REFERENCES users(id) ON DELETE CASCADE,
    itinerary_id uuid NOT NULL CONSTRAINT fk_itinerary_likes_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_itinerary_likes PRIMARY KEY (user_id, itinerary_id)
);

CREATE TABLE itinerary_votes (
    user_id uuid NOT NULL CONSTRAINT fk_itinerary_votes_user REFERENCES users(id) ON DELETE CASCADE,
    itinerary_id uuid NOT NULL CONSTRAINT fk_itinerary_votes_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_itinerary_votes PRIMARY KEY (user_id, itinerary_id)
);

CREATE TABLE itinerary_favorites (
    user_id uuid NOT NULL CONSTRAINT fk_itinerary_favorites_user REFERENCES users(id) ON DELETE CASCADE,
    itinerary_id uuid NOT NULL CONSTRAINT fk_itinerary_favorites_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_itinerary_favorites PRIMARY KEY (user_id, itinerary_id)
);

CREATE TABLE itinerary_ratings (
    user_id uuid NOT NULL CONSTRAINT fk_itinerary_ratings_user REFERENCES users(id) ON DELETE CASCADE,
    itinerary_id uuid NOT NULL CONSTRAINT fk_itinerary_ratings_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    rating smallint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_itinerary_ratings PRIMARY KEY (user_id, itinerary_id),
    CONSTRAINT ck_itinerary_ratings_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE itinerary_comments (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_itinerary_comments PRIMARY KEY,
    user_id uuid NOT NULL CONSTRAINT fk_itinerary_comments_user REFERENCES users(id),
    itinerary_id uuid NOT NULL CONSTRAINT fk_itinerary_comments_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    body varchar(1000) NOT NULL,
    hidden_by_moderation boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at timestamptz,
    CONSTRAINT ck_itinerary_comments_body CHECK (btrim(body) <> '')
);

CREATE TABLE itinerary_images (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_itinerary_images PRIMARY KEY,
    itinerary_id uuid NOT NULL CONSTRAINT fk_itinerary_images_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    object_key text NOT NULL,
    content_type text NOT NULL,
    size_bytes integer NOT NULL,
    caption text,
    alt_text text NOT NULL,
    position integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_itinerary_images_object_key UNIQUE (object_key),
    CONSTRAINT uq_itinerary_images_position UNIQUE (itinerary_id, position),
    CONSTRAINT ck_itinerary_images_object_key CHECK (btrim(object_key) <> ''),
    CONSTRAINT ck_itinerary_images_type CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_itinerary_images_size CHECK (size_bytes BETWEEN 1 AND 5000000),
    CONSTRAINT ck_itinerary_images_alt CHECK (btrim(alt_text) <> ''),
    CONSTRAINT ck_itinerary_images_position CHECK (position > 0)
);

CREATE TABLE user_follows (
    follower_id uuid NOT NULL CONSTRAINT fk_user_follows_follower REFERENCES users(id) ON DELETE CASCADE,
    followed_id uuid NOT NULL CONSTRAINT fk_user_follows_followed REFERENCES users(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_follows PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT ck_user_follows_distinct CHECK (follower_id <> followed_id)
);

CREATE TABLE notifications (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_notifications PRIMARY KEY,
    recipient_id uuid NOT NULL CONSTRAINT fk_notifications_recipient REFERENCES users(id) ON DELETE CASCADE,
    actor_id uuid NOT NULL CONSTRAINT fk_notifications_actor REFERENCES users(id),
    itinerary_id uuid NOT NULL CONSTRAINT fk_notifications_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    comment_id uuid CONSTRAINT fk_notifications_comment REFERENCES itinerary_comments(id) ON DELETE SET NULL,
    type text NOT NULL,
    event_key text NOT NULL CONSTRAINT uq_notifications_event_key UNIQUE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at timestamptz,
    CONSTRAINT ck_notifications_distinct CHECK (recipient_id <> actor_id),
    CONSTRAINT ck_notifications_type CHECK (type IN ('LIKE', 'COMMENT', 'VOTE', 'RATING')),
    CONSTRAINT ck_notifications_event_key CHECK (btrim(event_key) <> ''),
    CONSTRAINT ck_notifications_read CHECK (read_at IS NULL OR read_at >= created_at)
);

CREATE TABLE content_reports (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_content_reports PRIMARY KEY,
    reporter_id uuid NOT NULL CONSTRAINT fk_content_reports_reporter REFERENCES users(id),
    itinerary_id uuid CONSTRAINT fk_content_reports_itinerary REFERENCES itineraries(id) ON DELETE CASCADE,
    comment_id uuid CONSTRAINT fk_content_reports_comment REFERENCES itinerary_comments(id) ON DELETE CASCADE,
    reason text NOT NULL,
    details text,
    status text NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_content_reports_target CHECK (num_nonnulls(itinerary_id, comment_id) = 1),
    CONSTRAINT ck_content_reports_reason CHECK (reason IN ('INAPPROPRIATE_CONTENT', 'MISLEADING_INFORMATION', 'SPAM', 'OTHER')),
    CONSTRAINT ck_content_reports_details CHECK (reason <> 'OTHER' OR (details IS NOT NULL AND btrim(details) <> '')),
    CONSTRAINT ck_content_reports_status CHECK (status IN ('PENDING', 'DISMISSED', 'ACTIONED'))
);

CREATE UNIQUE INDEX uq_content_reports_pending_itinerary
    ON content_reports (reporter_id, itinerary_id) WHERE status = 'PENDING' AND itinerary_id IS NOT NULL;
CREATE UNIQUE INDEX uq_content_reports_pending_comment
    ON content_reports (reporter_id, comment_id) WHERE status = 'PENDING' AND comment_id IS NOT NULL;

-- Target snapshots are historical identifiers, deliberately not live foreign keys.
CREATE TABLE moderation_actions (
    id uuid DEFAULT gen_random_uuid() CONSTRAINT pk_moderation_actions PRIMARY KEY,
    administrator_id uuid NOT NULL CONSTRAINT fk_moderation_actions_admin REFERENCES users(id),
    report_id uuid CONSTRAINT fk_moderation_actions_report REFERENCES content_reports(id) ON DELETE SET NULL,
    previous_action_id uuid CONSTRAINT fk_moderation_actions_previous REFERENCES moderation_actions(id),
    action text NOT NULL,
    target_type text NOT NULL,
    target_id uuid NOT NULL,
    target_snapshot jsonb NOT NULL,
    justification text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_moderation_actions_action CHECK (action IN ('DISMISS', 'HIDE', 'SUSPEND', 'RESTORE')),
    CONSTRAINT ck_moderation_actions_type CHECK (target_type IN ('ITINERARY', 'COMMENT', 'USER')),
    CONSTRAINT ck_moderation_actions_snapshot CHECK (jsonb_typeof(target_snapshot) = 'object'),
    CONSTRAINT ck_moderation_actions_justification CHECK (btrim(justification) <> ''),
    CONSTRAINT ck_moderation_actions_previous CHECK (previous_action_id IS NULL OR previous_action_id <> id)
);
