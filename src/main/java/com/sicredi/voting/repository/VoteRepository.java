package com.sicredi.voting.repository;

import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.enums.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByTopicIdAndMemberId(Long topicId, String memberId);

    long countByTopicIdAndOption(Long topicId, VoteOption option);
}