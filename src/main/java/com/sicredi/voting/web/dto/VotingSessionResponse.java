package com.sicredi.voting.web.dto;

import com.sicredi.voting.domain.VotingSession;

import java.time.Instant;

public record VotingSessionResponse(
        Long id,
        Long topicId,
        Instant openedAt,
        Instant closesAt,
        int durationSeconds,
        boolean open
) {

    public static VotingSessionResponse of(VotingSession session) {
        return new VotingSessionResponse(
                session.getId(),
                session.getTopicId(),
                session.getOpenedAt(),
                session.getClosesAt(),
                session.getDurationSeconds(),
                session.isOpen()
        );
    }
}
