package com.sicredi.voting.service;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.enums.VoteOption;
import com.sicredi.voting.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    private static final String VALID_CPF = "11144477735";

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private TopicService topicService;

    @Mock
    private VotingSessionService sessionService;

    private VoteService voteService;

    @BeforeEach
    void setUp() {
        voteService = new VoteService(voteRepository, topicService, sessionService);
    }

    @Test
    void shouldRegisterVoteWhenSessionIsOpen() {
        Topic topic = new Topic("Title", "Desc");
        VotingSession session = new VotingSession(1L, 60);
        when(topicService.findById(1L)).thenReturn(topic);
        when(sessionService.findByTopicId(1L)).thenReturn(session);
        when(voteRepository.save(any(Vote.class))).thenAnswer(inv -> inv.getArgument(0));

        Vote vote = voteService.vote(1L, VALID_CPF, VoteOption.SIM);

        assertThat(vote.getMemberId()).isEqualTo(VALID_CPF);
        assertThat(vote.getOption()).isEqualTo(VoteOption.SIM);
    }

    @Test
    void shouldThrowUnprocessableEntityWhenSessionIsAlreadyClosed() {
        Topic topic = new Topic("Title", "Desc");
        VotingSession session = new VotingSession(1L, 60, Instant.now().minusSeconds(120));
        when(topicService.findById(1L)).thenReturn(topic);
        when(sessionService.findByTopicId(1L)).thenReturn(session);

        assertThatThrownBy(() -> voteService.vote(1L, VALID_CPF, VoteOption.SIM))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void shouldThrowConflictWhenVoteIsDuplicated() {
        Topic topic = new Topic("Title", "Desc");
        VotingSession session = new VotingSession(1L, 60);
        when(topicService.findById(1L)).thenReturn(topic);
        when(sessionService.findByTopicId(1L)).thenReturn(session);
        when(voteRepository.save(any(Vote.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> voteService.vote(1L, VALID_CPF, VoteOption.SIM))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void shouldTallyVotesWhenCountsAreEqual() {
        Topic topic = new Topic("Title", "Desc");
        when(topicService.findById(1L)).thenReturn(topic);
        when(sessionService.findOptionalByTopicId(1L)).thenReturn(Optional.empty());
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.SIM)).thenReturn(3L);
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.NAO)).thenReturn(3L);

        VotingResult result = voteService.tally(1L);

        assertThat(result.yesVotes()).isEqualTo(3L);
        assertThat(result.noVotes()).isEqualTo(3L);
        assertThat(result.total()).isEqualTo(6L);
    }

    @Test
    void shouldTallyVotesWhenYesIsGreaterThanNo() {
        Topic topic = new Topic("Title", "Desc");
        when(topicService.findById(1L)).thenReturn(topic);
        when(sessionService.findOptionalByTopicId(1L)).thenReturn(Optional.empty());
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.SIM)).thenReturn(5L);
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.NAO)).thenReturn(2L);

        VotingResult result = voteService.tally(1L);

        assertThat(result.yesVotes()).isEqualTo(5L);
        assertThat(result.noVotes()).isEqualTo(2L);
        assertThat(result.total()).isEqualTo(7L);
    }

    @Test
    void shouldTallyVotesWhenNoIsGreaterThanYes() {
        Topic topic = new Topic("Title", "Desc");
        when(topicService.findById(1L)).thenReturn(topic);
        when(sessionService.findOptionalByTopicId(1L)).thenReturn(Optional.empty());
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.SIM)).thenReturn(2L);
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.NAO)).thenReturn(5L);

        VotingResult result = voteService.tally(1L);

        assertThat(result.yesVotes()).isEqualTo(2L);
        assertThat(result.noVotes()).isEqualTo(5L);
        assertThat(result.total()).isEqualTo(7L);
    }

    @Test
    void shouldTallyVotesWhenNoVotesExist() {
        Topic topic = new Topic("Title", "Desc");
        when(topicService.findById(1L)).thenReturn(topic);
        when(sessionService.findOptionalByTopicId(1L)).thenReturn(Optional.empty());
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.SIM)).thenReturn(0L);
        when(voteRepository.countByTopicIdAndOption(1L, VoteOption.NAO)).thenReturn(0L);

        VotingResult result = voteService.tally(1L);

        assertThat(result.yesVotes()).isZero();
        assertThat(result.noVotes()).isZero();
        assertThat(result.total()).isZero();
    }
}