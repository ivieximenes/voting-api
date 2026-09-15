package com.sicredi.voting.repository;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.enums.VoteOption;
import com.sicredi.voting.support.PostgresContainerSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Container Postgres compartilhado herdado de PostgresContainerSupport
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VoteRepositoryTest extends PostgresContainerSupport {

    @Autowired
    private VoteRepository repository;

    @Autowired
    private TopicRepository topicRepository;

    private static final String MEMBER_ID = "12345678900";

    private Long topicId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        topicRepository.deleteAll();
        topicId = topicRepository.save(new Topic("Pauta de teste", null)).getId();
    }

    private Long newTopicId() {
        return topicRepository.save(new Topic("Outra pauta", null)).getId();
    }

    @Test
    void shouldSaveVote() {
        var vote = new Vote(topicId, MEMBER_ID, VoteOption.YES);

        var saved = repository.save(vote);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTopicId()).isEqualTo(topicId);
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getOption()).isEqualTo(VoteOption.YES);
    }

    @Test
    void shouldFindVoteById() {
        var vote = new Vote(topicId, MEMBER_ID, VoteOption.YES);
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
        var vote = new Vote(topicId, MEMBER_ID, VoteOption.YES);
        var saved = repository.save(vote);

        repository.deleteById(saved.getId());

        var found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountTotalVotes() {
        Long otherTopicId = newTopicId();
        repository.save(new Vote(topicId, "11111111111", VoteOption.YES));
        repository.save(new Vote(topicId, "22222222222", VoteOption.NO));
        repository.save(new Vote(otherTopicId, "33333333333", VoteOption.YES));

        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void shouldNotAllowDuplicateVotePerMemberPerTopic() {
        repository.save(new Vote(topicId, MEMBER_ID, VoteOption.YES));

        var duplicate = new Vote(topicId, MEMBER_ID, VoteOption.NO);

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameMemberToVoteOnDifferentTopics() {
        Long otherTopicId = newTopicId();
        repository.save(new Vote(topicId, MEMBER_ID, VoteOption.YES));
        repository.save(new Vote(otherTopicId, MEMBER_ID, VoteOption.NO));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void shouldReturnTrueWhenMemberAlreadyVotedOnTopic() {
        repository.save(new Vote(topicId, MEMBER_ID, VoteOption.YES));

        assertThat(repository.existsByTopicIdAndMemberId(topicId, MEMBER_ID)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenMemberHasNotVotedOnTopic() {
        assertThat(repository.existsByTopicIdAndMemberId(topicId, MEMBER_ID)).isFalse();
    }

    @Test
    void shouldCountVotesByTopicAndOption() {
        Long otherTopicId = newTopicId();
        repository.save(new Vote(topicId, "11111111111", VoteOption.YES));
        repository.save(new Vote(topicId, "22222222222", VoteOption.YES));
        repository.save(new Vote(topicId, "33333333333", VoteOption.NO));
        repository.save(new Vote(otherTopicId, "44444444444", VoteOption.YES));

        assertThat(repository.countByTopicIdAndOption(topicId, VoteOption.YES)).isEqualTo(2);
        assertThat(repository.countByTopicIdAndOption(topicId, VoteOption.NO)).isEqualTo(1);
        assertThat(repository.countByTopicIdAndOption(otherTopicId, VoteOption.YES)).isEqualTo(1);
        assertThat(repository.countByTopicIdAndOption(otherTopicId, VoteOption.NO)).isZero();
    }
}
