package com.sicredi.voting.service;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.enums.VoteOption;
import com.sicredi.voting.repository.VoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final TopicService topicService;
    private final VotingSessionService sessionService;
    private final MemberValidationService memberValidationService;

    public VoteService(
            VoteRepository voteRepository,
            TopicService topicService,
            VotingSessionService sessionService,
            MemberValidationService memberValidationService) {
        this.voteRepository = voteRepository;
        this.topicService = topicService;
        this.sessionService = sessionService;
        this.memberValidationService = memberValidationService;
    }

    @Transactional
    public Vote vote(Long topicId, String memberId, VoteOption option) {
        topicService.findById(topicId);

        VotingSession session = sessionService.findByTopicId(topicId);
        if (!session.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Sessão de votação encerrada para pauta: id=" + topicId);
        }

        memberValidationService.validate(memberId);

        try {
            Vote vote = new Vote(topicId, memberId, option);
            return voteRepository.save(vote);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Associado já votou nesta pauta: topicId=" + topicId + ", memberId=" + memberId);
        }
    }

    @Transactional(readOnly = true)
    public VotingResult tally(Long topicId) {
        Topic topic = topicService.findById(topicId);
        Optional<VotingSession> session = sessionService.findOptionalByTopicId(topicId);

        long yesVotes = voteRepository.countByTopicIdAndOption(topicId, VoteOption.SIM);
        long noVotes = voteRepository.countByTopicIdAndOption(topicId, VoteOption.NAO);

        Instant now = Instant.now();
        boolean sessionClosed = session.isPresent() && !session.get().isOpen(now);

        return new VotingResult(topic, yesVotes, noVotes, sessionClosed);
    }
}