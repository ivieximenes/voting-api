package com.sicredi.voting.web.v1;

import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.service.VotingSessionService;
import com.sicredi.voting.web.dto.OpenSessionRequest;
import com.sicredi.voting.web.dto.VotingSessionResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/topics/{topicId}/sessions")
@Tag(name = "Voting Sessions", description = "Voting session lifecycle for a topic")
public class VotingSessionController {

    private final VotingSessionService sessionService;

    public VotingSessionController(VotingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<VotingSessionResponse> open(
            @PathVariable Long topicId,
            @Valid @RequestBody(required = false) OpenSessionRequest request) {

        Integer durationSeconds = (request != null) ? request.durationSeconds() : null;
        VotingSession session = sessionService.open(topicId, durationSeconds);

        URI location = URI.create("/api/v1/topics/" + topicId + "/sessions/" + session.getId());
        return ResponseEntity.created(location).body(VotingSessionResponse.of(session));
    }

    @GetMapping
    public VotingSessionResponse find(@PathVariable Long topicId) {
        return VotingSessionResponse.of(sessionService.findByTopicId(topicId));
    }
}
