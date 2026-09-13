package com.sicredi.voting.repository;

import com.sicredi.voting.domain.VotingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {

    Optional<VotingSession> findByTopicId(Long topicId);

    boolean existsByTopicId(Long topicId);
}
