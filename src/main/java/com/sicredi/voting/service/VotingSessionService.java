package com.sicredi.voting.service;

import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.repository.VotingSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

@Service
public class VotingSessionService {

    private final VotingSessionRepository sessionRepository;
    private final TopicService topicService;
    private final Duration defaultDuration;

    public VotingSessionService(
            VotingSessionRepository sessionRepository,
            TopicService topicService,
            @Value("${sicredi.voting.default-session-duration:60s}") Duration defaultDuration) {
        this.sessionRepository = sessionRepository;
        this.topicService = topicService;
        this.defaultDuration = defaultDuration;
    }

    @Transactional
    public VotingSession open(Long topicId, Integer durationSeconds) {
        topicService.findById(topicId);

        if (sessionRepository.existsByTopicId(topicId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sessão já aberta para pauta: id=" + topicId);
        }

        int duration = (durationSeconds != null) ? durationSeconds : (int) defaultDuration.getSeconds();
        if (duration <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A duração da sessão deve ser maior que zero");
        }

        try {
            VotingSession session = new VotingSession(topicId, duration);
            return sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sessão já aberta para pauta: id=" + topicId);
        }
    }

    @Transactional(readOnly = true)
    public VotingSession findByTopicId(Long topicId) {
        return sessionRepository.findByTopicId(topicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sessão não encontrada para pauta: id=" + topicId));
    }

    @Transactional(readOnly = true)
    public Optional<VotingSession> findOptionalByTopicId(Long topicId) {
        return sessionRepository.findByTopicId(topicId);
    }
}