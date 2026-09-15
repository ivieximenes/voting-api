package com.sicredi.voting.repository;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.support.PostgresContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Container Postgres compartilhado herdado de PostgresContainerSupport
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VotingSessionRepositoryTest extends PostgresContainerSupport {

    @Autowired
    private VotingSessionRepository repository;

    @Autowired
    private TopicRepository topicRepository;

    private static final int DURATION_SECONDS = 60;
    private static final Instant OPENED_AT = Instant.parse("2024-01-01T10:00:00Z");

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
    void shouldSaveVotingSession() {
        var session = new VotingSession(topicId, DURATION_SECONDS, OPENED_AT);

        var saved = repository.save(session);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTopicId()).isEqualTo(topicId);
        assertThat(saved.getDurationSeconds()).isEqualTo(DURATION_SECONDS);
    }

    @Test
    void shouldFindSessionById() {
        var session = new VotingSession(topicId, DURATION_SECONDS, OPENED_AT);
        var saved = repository.save(session);

        var found = repository.findById(saved.getId());

        assertThat(found)
                .isPresent()
                .contains(saved);
    }

    @Test
    void shouldReturnEmptyWhenSessionNotFound() {
        var found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindSessionByTopicId() {
        var session = new VotingSession(topicId, DURATION_SECONDS, OPENED_AT);
        repository.save(session);

        var found = repository.findByTopicId(topicId);

        assertThat(found)
                .isPresent()
                .contains(session);
    }

    @Test
    void shouldReturnEmptyForNonExistentTopic() {
        var found = repository.findByTopicId(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldNotAllowDuplicateSessionPerTopic() {
        repository.save(new VotingSession(topicId, DURATION_SECONDS, OPENED_AT));

        var duplicate = new VotingSession(topicId, DURATION_SECONDS, OPENED_AT);

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteSession() {
        var session = new VotingSession(topicId, DURATION_SECONDS, OPENED_AT);
        var saved = repository.save(session);

        repository.deleteById(saved.getId());

        var found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountSessions() {
        repository.save(new VotingSession(topicId, 60, OPENED_AT));
        repository.save(new VotingSession(newTopicId(), 120, OPENED_AT));
        repository.save(new VotingSession(newTopicId(), 300, OPENED_AT));

        var count = repository.count();

        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldSaveSessionWithLongDuration() {
        var longDuration = 86400; // 24 horas
        var session = new VotingSession(topicId, longDuration, OPENED_AT);

        var saved = repository.save(session);

        assertThat(saved.getDurationSeconds()).isEqualTo(longDuration);
    }

    @Test
    void shouldPreserveOpenedAtAndClosesAt() {
        var session = new VotingSession(topicId, DURATION_SECONDS, OPENED_AT);
        var saved = repository.save(session);

        var found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(found.getClosesAt()).isEqualTo(OPENED_AT.plusSeconds(DURATION_SECONDS));
    }
}
