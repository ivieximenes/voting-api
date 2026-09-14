CREATE TABLE IF NOT EXISTS topic (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS voting_session (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closes_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_seconds INTEGER NOT NULL,
    CONSTRAINT uk_voting_session_topic UNIQUE (topic_id)
);

CREATE TABLE IF NOT EXISTS vote (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    member_id VARCHAR(11) NOT NULL,
    vote_option VARCHAR(10) NOT NULL CHECK (vote_option IN ('SIM', 'NAO')),
    voted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_vote_topic_member UNIQUE (topic_id, member_id)
);

CREATE INDEX IF NOT EXISTS idx_vote_topic_option
    ON vote (topic_id, vote_option);