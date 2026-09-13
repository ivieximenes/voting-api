package com.sicredi.voting.web.dto;

import com.sicredi.voting.service.VotingResult;

public record VotingResultResponse(
        Long topicId,
        String title,
        long yesVotes,
        long noVotes,
        long totalVotes,
        boolean sessionClosed
) {

    public static VotingResultResponse of(VotingResult result) {
        return new VotingResultResponse(
                result.topic().getId(),
                result.topic().getTitle(),
                result.yesVotes(),
                result.noVotes(),
                result.total(),
                result.sessionClosed()
        );
    }
}
