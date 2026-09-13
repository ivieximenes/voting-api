CREATE TABLE IF NOT EXISTS topic (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS voting_session (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closes_at TIMESTAMP NOT NULL,
    duration_seconds INTEGER NOT NULL,
    CONSTRAINT uk_voting_session_topic UNIQUE (topic_id)
);

CREATE TABLE IF NOT EXISTS vote (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    member_id VARCHAR(11) NOT NULL,
    vote_option VARCHAR(10) NOT NULL,
    voted_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_vote_topic_member UNIQUE (topic_id, member_id)
);
