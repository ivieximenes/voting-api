package com.sicredi.voting.service;

import com.sicredi.voting.domain.Topic;

public record VotingResult(Topic topic, long yesVotes, long noVotes, boolean sessionClosed) {

    public long total() {
        return yesVotes + noVotes;
    }
}
