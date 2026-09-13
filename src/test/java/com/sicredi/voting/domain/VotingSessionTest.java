package com.sicredi.voting.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class VotingSessionTest {

    private static final Instant OPENED_AT = Instant.parse("2024-01-01T10:00:00Z");

    @Test
    void shouldCreateSessionWithTopicAndDuration() {
        var topicId = 1L;
        var durationSeconds = 60;

        var session = new VotingSession(topicId, durationSeconds, OPENED_AT);

        assertThat(session.getId()).isNull();
        assertThat(session.getTopicId()).isEqualTo(topicId);
        assertThat(session.getDurationSeconds()).isEqualTo(durationSeconds);
        assertThat(session.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(session.getClosesAt()).isEqualTo(OPENED_AT.plusSeconds(durationSeconds));
    }

    @Test
    void shouldCalculateClosesAtAsOpenedAtPlusDuration() {
        var durationSeconds = 120;
        var session = new VotingSession(1L, durationSeconds, OPENED_AT);

        assertThat(session.getClosesAt()).isEqualTo(OPENED_AT.plusSeconds(durationSeconds));
    }

    @Test
    void shouldBeOpenImmediatelyAfterCreation() {
        var session = new VotingSession(1L, 3600, OPENED_AT);

        assertThat(session.isOpen(OPENED_AT)).isTrue();
    }

    @Test
    void shouldBeOpenBeforeClosesAt() {
        var session = new VotingSession(1L, 3600, OPENED_AT);
        var beforeClose = session.getClosesAt().minusSeconds(1);

        assertThat(session.isOpen(beforeClose)).isTrue();
    }

    @Test
    void shouldBeClosedAfterClosesAt() {
        var session = new VotingSession(1L, 60, OPENED_AT);
        var afterClose = session.getClosesAt().plusSeconds(1);

        assertThat(session.isOpen(afterClose)).isFalse();
    }

    @Test
    void shouldNotBeOpenAtExactCloseTime() {
        var session = new VotingSession(1L, 60, OPENED_AT);

        assertThat(session.isOpen(session.getClosesAt())).isFalse();
    }

    @Test
    void shouldAcceptLongDurationInSeconds() {
        var longDuration = 86400; // 24 horas
        var session = new VotingSession(1L, longDuration, OPENED_AT);

        assertThat(session.getDurationSeconds()).isEqualTo(longDuration);
        assertThat(session.getClosesAt()).isEqualTo(OPENED_AT.plusSeconds(longDuration));
    }

    @Test
    void shouldAcceptMinimalDurationInSeconds() {
        var minimalDuration = 1;
        var session = new VotingSession(1L, minimalDuration, OPENED_AT);

        assertThat(session.getDurationSeconds()).isEqualTo(minimalDuration);
    }
}