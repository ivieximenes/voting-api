package com.sicredi.voting.web.dto;

import com.sicredi.voting.domain.Topic;

import java.time.Instant;

public record TopicResponse(
        Long id,
        String title,
        String description,
        Instant createdAt
) {
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getCreatedAt()
        );
    }
}