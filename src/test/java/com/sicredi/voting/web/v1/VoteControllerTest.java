package com.sicredi.voting.web.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.Vote;
import com.sicredi.voting.enums.VoteOption;
import com.sicredi.voting.service.VoteService;
import com.sicredi.voting.service.VotingResult;
import com.sicredi.voting.web.dto.RegisterVoteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoteController.class)
class VoteControllerTest {

    private static final String VALID_CPF = "11144477735";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoteService voteService;

    @Test
    void shouldRegisterVote() throws Exception {
        var request = new RegisterVoteRequest(VALID_CPF, VoteOption.YES);
        var vote = new Vote(1L, VALID_CPF, VoteOption.YES);
        when(voteService.vote(eq(1L), eq(VALID_CPF), eq(VoteOption.YES))).thenReturn(vote);

        mockMvc.perform(post("/api/v1/topics/{topicId}/votes", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicId").value(1L))
                .andExpect(jsonPath("$.memberId").value(VALID_CPF))
                .andExpect(jsonPath("$.option").value("YES"))
                .andExpect(jsonPath("$.votedAt").exists());
    }

    @Test
    void shouldReturn400WhenMemberIdIsBlank() throws Exception {
        var request = new RegisterVoteRequest("", VoteOption.YES);

        mockMvc.perform(post("/api/v1/topics/{topicId}/votes", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenMemberIdHasInvalidLength() throws Exception {
        var request = new RegisterVoteRequest("123", VoteOption.YES);

        mockMvc.perform(post("/api/v1/topics/{topicId}/votes", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenOptionIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/topics/{topicId}/votes", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s"}
                                """.formatted(VALID_CPF)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenTopicDoesNotExist() throws Exception {
        var request = new RegisterVoteRequest(VALID_CPF, VoteOption.YES);
        when(voteService.vote(eq(99L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada"));

        mockMvc.perform(post("/api/v1/topics/{topicId}/votes", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenVoteIsDuplicated() throws Exception {
        var request = new RegisterVoteRequest(VALID_CPF, VoteOption.YES);
        when(voteService.vote(eq(1L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Voto duplicado"));

        mockMvc.perform(post("/api/v1/topics/{topicId}/votes", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn422WhenSessionIsClosed() throws Exception {
        var request = new RegisterVoteRequest(VALID_CPF, VoteOption.YES);
        when(voteService.vote(eq(1L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Sessão encerrada"));

        mockMvc.perform(post("/api/v1/topics/{topicId}/votes", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturnResult() throws Exception {
        var topic = new Topic("Pauta", "Desc");
        var result = new VotingResult(topic, 5L, 2L, false);
        when(voteService.tally(1L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/topics/{topicId}/result", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(topic.getId()))
                .andExpect(jsonPath("$.title").value("Pauta"))
                .andExpect(jsonPath("$.yesVotes").value(5))
                .andExpect(jsonPath("$.noVotes").value(2))
                .andExpect(jsonPath("$.totalVotes").value(7));
    }

    @Test
    void shouldReturn404WhenTallyingNonExistentTopic() throws Exception {
        when(voteService.tally(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada"));

        mockMvc.perform(get("/api/v1/topics/{topicId}/result", 99L))
                .andExpect(status().isNotFound());
    }
}