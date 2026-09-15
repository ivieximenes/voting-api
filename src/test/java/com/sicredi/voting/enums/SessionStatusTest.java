package com.sicredi.voting.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionStatusTest {

    @Test
    void shouldHaveThreeStatuses() {
        assertThat(SessionStatus.values())
                .containsExactly(SessionStatus.NOT_STARTED, SessionStatus.OPEN, SessionStatus.CLOSED);
    }

    @Test
    void shouldResolveByName() {
        assertThat(SessionStatus.valueOf("OPEN")).isEqualTo(SessionStatus.OPEN);
        assertThat(SessionStatus.valueOf("CLOSED")).isEqualTo(SessionStatus.CLOSED);
        assertThat(SessionStatus.valueOf("NOT_STARTED")).isEqualTo(SessionStatus.NOT_STARTED);
    }
}
