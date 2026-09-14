package com.sicredi.voting.repository;

import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.enums.VoteOption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VoteRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private VoteRepository repository;

    private static final Long TOPIC_ID = 1L;
    private static final String MEMBER_ID = "12345678900";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveVote() {
        var vote = new Vote(TOPIC_ID, MEMBER_ID, VoteOption.YES);

        var saved = repository.save(vote);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTopicId()).isEqualTo(TOPIC_ID);
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getOption()).isEqualTo(VoteOption.YES);
    }

    @Test
    void shouldFindVoteById() {
        var vote = new Vote(TOPIC_ID, MEMBER_ID, VoteOption.YES);
        var saved = repository.save(vote);

        var found = repository.findById(saved.getId());

        assertThat(found)
                .isPresent()
                .contains(saved);
    }

    @Test
    void shouldReturnEmptyWhenVoteNotFound() {
        var found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldDeleteVote() {
        var vote = new Vote(TOPIC_ID, MEMBER_ID, VoteOption.YES);
        var saved = repository.save(vote);

        repository.deleteById(saved.getId());

        var found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountTotalVotes() {
        repository.save(new Vote(TOPIC_ID, "11111111111", VoteOption.YES));
        repository.save(new Vote(TOPIC_ID, "22222222222", VoteOption.NO));
        repository.save(new Vote(2L, "33333333333", VoteOption.YES));

        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void shouldNotAllowDuplicateVotePerMemberPerTopic() {
        repository.save(new Vote(TOPIC_ID, MEMBER_ID, VoteOption.YES));

        var duplicate = new Vote(TOPIC_ID, MEMBER_ID, VoteOption.NO);

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameMemberToVoteOnDifferentTopics() {
        repository.save(new Vote(1L, MEMBER_ID, VoteOption.YES));
        repository.save(new Vote(2L, MEMBER_ID, VoteOption.NO));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void shouldReturnTrueWhenMemberAlreadyVotedOnTopic() {
        repository.save(new Vote(TOPIC_ID, MEMBER_ID, VoteOption.YES));

        assertThat(repository.existsByTopicIdAndMemberId(TOPIC_ID, MEMBER_ID)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenMemberHasNotVotedOnTopic() {
        assertThat(repository.existsByTopicIdAndMemberId(TOPIC_ID, MEMBER_ID)).isFalse();
    }

    @Test
    void shouldCountVotesByTopicAndOption() {
        repository.save(new Vote(TOPIC_ID, "11111111111", VoteOption.YES));
        repository.save(new Vote(TOPIC_ID, "22222222222", VoteOption.YES));
        repository.save(new Vote(TOPIC_ID, "33333333333", VoteOption.NO));
        repository.save(new Vote(2L, "44444444444", VoteOption.YES));

        assertThat(repository.countByTopicIdAndOption(TOPIC_ID, VoteOption.YES)).isEqualTo(2);
        assertThat(repository.countByTopicIdAndOption(TOPIC_ID, VoteOption.NO)).isEqualTo(1);
        assertThat(repository.countByTopicIdAndOption(2L, VoteOption.YES)).isEqualTo(1);
        assertThat(repository.countByTopicIdAndOption(2L, VoteOption.NO)).isZero();
    }
}