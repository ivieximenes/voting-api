package com.sicredi.voting.web.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.service.VotingSessionService;
import com.sicredi.voting.web.dto.OpenSessionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test da camada web do contexto "sessao de votacao".
 * Sobe apenas o controller + MockMvc; o VotingSessionService e mockado.
 */
@WebMvcTest(VotingSessionController.class)
class VotingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VotingSessionService sessionService;

    @Test
    void shouldOpenSessionWithDefaultDuration() throws Exception {
        var session = new VotingSession(1L, 60);
        when(sessionService.open(eq(1L), isNull())).thenReturn(session);

        mockMvc.perform(post("/api/v1/topics/{topicId}/sessions", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicId").value(1L))
                .andExpect(jsonPath("$.durationSeconds").value(60))
                .andExpect(jsonPath("$.open").value(true));
    }

    @Test
    void shouldOpenSessionWithInformedDuration() throws Exception {
        var request = new OpenSessionRequest(120);
        var session = new VotingSession(1L, 120);
        when(sessionService.open(eq(1L), eq(120))).thenReturn(session);

        mockMvc.perform(post("/api/v1/topics/{topicId}/sessions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationSeconds").value(120));
    }

    @Test
    void shouldReturn400WhenDurationIsNegative() throws Exception {
        var request = new OpenSessionRequest(-5);

        mockMvc.perform(post("/api/v1/topics/{topicId}/sessions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenSessionAlreadyOpen() throws Exception {
        when(sessionService.open(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Sessão já aberta"));

        mockMvc.perform(post("/api/v1/topics/{topicId}/sessions", 1L))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn404WhenTopicDoesNotExist() throws Exception {
        when(sessionService.open(eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada"));

        mockMvc.perform(post("/api/v1/topics/{topicId}/sessions", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindSessionByTopicId() throws Exception {
        var session = new VotingSession(1L, 60);
        when(sessionService.findByTopicId(1L)).thenReturn(session);

        mockMvc.perform(get("/api/v1/topics/{topicId}/sessions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(1L))
                .andExpect(jsonPath("$.durationSeconds").value(60));
    }

    @Test
    void shouldReturn404WhenSessionDoesNotExist() throws Exception {
        when(sessionService.findByTopicId(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));

        mockMvc.perform(get("/api/v1/topics/{topicId}/sessions", 99L))
                .andExpect(status().isNotFound());
    }
}