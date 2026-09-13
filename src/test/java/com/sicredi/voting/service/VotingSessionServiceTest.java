package com.sicredi.voting.service;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.repository.VotingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotingSessionServiceTest {

    @Mock
    private VotingSessionRepository sessionRepository;

    @Mock
    private TopicService topicService;

    private VotingSessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new VotingSessionService(sessionRepository, topicService, Duration.ofSeconds(60));
    }

    @Test
    void shouldOpenSessionWithDefaultDurationWhenNotInformed() {
        when(topicService.findById(1L)).thenReturn(new Topic("Title", "Desc"));
        when(sessionRepository.existsByTopicId(1L)).thenReturn(false);
        when(sessionRepository.save(any(VotingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingSession session = sessionService.open(1L, null);

        assertThat(session.getDurationSeconds()).isEqualTo(60);
        assertThat(session.isOpen()).isTrue();
    }

    @Test
    void shouldOpenSessionWithInformedDuration() {
        when(topicService.findById(1L)).thenReturn(new Topic("Title", "Desc"));
        when(sessionRepository.existsByTopicId(1L)).thenReturn(false);
        when(sessionRepository.save(any(VotingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingSession session = sessionService.open(1L, 120);

        assertThat(session.getDurationSeconds()).isEqualTo(120);
    }

    @Test
    void shouldThrowConflictWhenSessionAlreadyOpen() {
        when(topicService.findById(1L)).thenReturn(new Topic("Title", "Desc"));
        when(sessionRepository.existsByTopicId(1L)).thenReturn(true);

        assertThatThrownBy(() -> sessionService.open(1L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void shouldThrowBadRequestWhenDurationIsInvalid() {
        when(topicService.findById(1L)).thenReturn(new Topic("Title", "Desc"));
        when(sessionRepository.existsByTopicId(1L)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.open(1L, -5))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldThrowNotFoundWhenSessionDoesNotExist() {
        when(sessionRepository.findByTopicId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.findByTopicId(1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }   

    @Test
    void shouldUseConfiguredDefaultDurationWhenNotInformed() {
        var customService = new VotingSessionService(
                sessionRepository, topicService, Duration.ofSeconds(120));

        when(topicService.findById(1L)).thenReturn(new Topic("Title", "Desc"));
        when(sessionRepository.existsByTopicId(1L)).thenReturn(false);
        when(sessionRepository.save(any(VotingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingSession session = customService.open(1L, null);

        assertThat(session.getDurationSeconds()).isEqualTo(120);
    }
}