package com.sicredi.voting.web.v1;

import com.sicredi.voting.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integracao do contexto "sessao de votacao": abertura e consulta,
 * incluindo cenarios de sucesso e erro.
 */
class VotingSessionFlowIT extends AbstractIntegrationTest {

    @Test
    void shouldOpenSessionWithDefaultDuration() throws Exception {
        Long topicId = createTopic("Topic with default session", null);

        openSession(topicId, null);

        mockMvc.perform(get("/api/v1/topics/{id}/sessions", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(topicId))
                .andExpect(jsonPath("$.durationSeconds").value(60))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.openedAt").exists())
                .andExpect(jsonPath("$.closesAt").exists());
    }

    @Test
    void shouldOpenSessionWithInformedDuration() throws Exception {
        Long topicId = createTopic("Topic with custom session", null);

        openSession(topicId, 120);

        mockMvc.perform(get("/api/v1/topics/{id}/sessions", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationSeconds").value(120));
    }

    @Test
    void shouldReturn404WhenOpeningSessionForNonExistentTopic() throws Exception {
        mockMvc.perform(post("/api/v1/topics/{id}/sessions", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldReturn404WhenFindingSessionForTopicWithoutSession() throws Exception {
        Long topicId = createTopic("Topic without session", null);

        mockMvc.perform(get("/api/v1/topics/{id}/sessions", topicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn409WhenOpeningSessionTwiceForSameTopic() throws Exception {
        Long topicId = createTopic("Topic with duplicate session", null);
        openSession(topicId, null);

        mockMvc.perform(post("/api/v1/topics/{id}/sessions", topicId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldReturn400WhenDurationIsNegative() throws Exception {
        Long topicId = createTopic("Topic with invalid duration", null);

        mockMvc.perform(post("/api/v1/topics/{id}/sessions", topicId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"durationSeconds":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}