package com.sicredi.voting.web.v1;

import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.service.VoteService;
import com.sicredi.voting.service.VotingResult;
import com.sicredi.voting.web.dto.RegisterVoteRequest;
import com.sicredi.voting.web.dto.VoteResponse;
import com.sicredi.voting.web.dto.VotingResultResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/topics/{topicId}")
@Tag(name = "Votes", description = "Vote registration and result tally for a topic")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/votes")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable Long topicId,
            @Valid @RequestBody RegisterVoteRequest request) {
        Vote vote = voteService.vote(topicId, request.memberId(), request.option());
        return ResponseEntity.status(HttpStatus.CREATED).body(VoteResponse.of(vote));
    }

    @GetMapping("/result")
    public VotingResultResponse result(@PathVariable Long topicId) {
        VotingResult result = voteService.tally(topicId);
        return VotingResultResponse.of(result);
    }
}