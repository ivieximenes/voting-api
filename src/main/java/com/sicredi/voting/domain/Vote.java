package com.sicredi.voting.domain;

import com.sicredi.voting.enums.VoteOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "vote",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vote_topic_member",
                columnNames = {"topic_id", "member_id"}
        )
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "member_id", nullable = false, length = 11)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_option", nullable = false, length = 10)
    private VoteOption option;

    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt;

    public Vote(Long topicId, String memberId, VoteOption option) {
        this.topicId = topicId;
        this.memberId = memberId;
        this.option = option;
        this.votedAt = Instant.now();
    }
}