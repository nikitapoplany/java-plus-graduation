CREATE TABLE IF NOT EXISTS user_event_interactions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    last_interaction_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_user_event_interactions_user_time
    ON user_event_interactions (user_id, last_interaction_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_event_interactions_event
    ON user_event_interactions (event_id);

CREATE TABLE IF NOT EXISTS event_similarities (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_a, event_b)
);

CREATE INDEX IF NOT EXISTS idx_event_similarities_event_a
    ON event_similarities (event_a);

CREATE INDEX IF NOT EXISTS idx_event_similarities_event_b
    ON event_similarities (event_b);
