ALTER TABLE voting_session
    ADD CONSTRAINT fk_voting_session_topic
    FOREIGN KEY (topic_id) REFERENCES topic (id);

ALTER TABLE vote
    ADD CONSTRAINT fk_vote_topic
    FOREIGN KEY (topic_id) REFERENCES topic (id);
