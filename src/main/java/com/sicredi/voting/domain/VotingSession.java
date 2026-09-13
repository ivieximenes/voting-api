package com.sicredi.voting.domain;

import com.sicredi.voting.enums.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "voting_session",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_voting_session_topic",
                columnNames = "topic_id"
        )
)
public class VotingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closes_at", nullable = false, updatable = false)
    private Instant closesAt;

    @Column(name = "duration_seconds", nullable = false, updatable = false)
    private int durationSeconds;

    public VotingSession(Long topicId, int durationSeconds) {
        this(topicId, durationSeconds, Instant.now());
    }

    public VotingSession(Long topicId, int durationSeconds, Instant openedAt) {
        this.topicId = topicId;
        this.durationSeconds = durationSeconds;
        this.openedAt = openedAt;
        this.closesAt = openedAt.plusSeconds(durationSeconds);
    }

    public boolean isOpen(Instant now) {
        return now.isBefore(closesAt);
    }

    public boolean isOpen() {
        return isOpen(Instant.now());
    }

    public SessionStatus status(Instant now) {
        return isOpen(now) ? SessionStatus.OPEN : SessionStatus.CLOSED;
    }

    public SessionStatus status() {
        return status(Instant.now());
    }
}