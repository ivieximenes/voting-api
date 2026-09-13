package com.sicredi.voting.web.dto;

import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.enums.VoteOption;

import java.time.Instant;

public record VoteResponse(
        Long id,
        Long topicId,
        String memberId,
        VoteOption option,
        Instant votedAt
) {

    public static VoteResponse of(Vote vote) {
        return new VoteResponse(
                vote.getId(),
                vote.getTopicId(),
                vote.getMemberId(),
                vote.getOption(),
                vote.getVotedAt()
        );
    }
}
