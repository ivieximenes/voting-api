package com.sicredi.voting.repository;

import com.sicredi.voting.domain.VotingSession;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VotingSessionRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private VotingSessionRepository repository;

    private static final Long TOPIC_ID = 1L;
    private static final int DURATION_SECONDS = 60;
    private static final Instant OPENED_AT = Instant.parse("2024-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveVotingSession() {
        var session = new VotingSession(TOPIC_ID, DURATION_SECONDS, OPENED_AT);

        var saved = repository.save(session);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTopicId()).isEqualTo(TOPIC_ID);
        assertThat(saved.getDurationSeconds()).isEqualTo(DURATION_SECONDS);
    }

    @Test
    void shouldFindSessionById() {
        var session = new VotingSession(TOPIC_ID, DURATION_SECONDS, OPENED_AT);
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
        var session = new VotingSession(TOPIC_ID, DURATION_SECONDS, OPENED_AT);
        repository.save(session);

        var found = repository.findByTopicId(TOPIC_ID);

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
        repository.save(new VotingSession(TOPIC_ID, DURATION_SECONDS, OPENED_AT));

        var duplicate = new VotingSession(TOPIC_ID, DURATION_SECONDS, OPENED_AT);

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteSession() {
        var session = new VotingSession(TOPIC_ID, DURATION_SECONDS, OPENED_AT);
        var saved = repository.save(session);

        repository.deleteById(saved.getId());

        var found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountSessions() {
        repository.save(new VotingSession(1L, 60, OPENED_AT));
        repository.save(new VotingSession(2L, 120, OPENED_AT));
        repository.save(new VotingSession(3L, 300, OPENED_AT));

        var count = repository.count();

        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldSaveSessionWithLongDuration() {
        var longDuration = 86400; // 24 horas
        var session = new VotingSession(TOPIC_ID, longDuration, OPENED_AT);

        var saved = repository.save(session);

        assertThat(saved.getDurationSeconds()).isEqualTo(longDuration);
    }

    @Test
    void shouldPreserveOpenedAtAndClosesAt() {
        var session = new VotingSession(TOPIC_ID, DURATION_SECONDS, OPENED_AT);
        var saved = repository.save(session);

        var found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(found.getClosesAt()).isEqualTo(OPENED_AT.plusSeconds(DURATION_SECONDS));
    }
}