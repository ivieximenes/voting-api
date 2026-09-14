package com.sicredi.voting.web.v1;

import com.sicredi.voting.enums.VoteOption;
import com.sicredi.voting.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integracao do contexto "apuracao de resultado": consulta do
 * resultado da votacao em diferentes cenarios, incluindo pauta sem sessao,
 * sessao sem votos, empate, maioria de SIM e maioria de NAO.
 */
class VotingResultFlowIT extends AbstractIntegrationTest {

    private static final String VALID_CPF_1 = "11144477735";
    private static final String VALID_CPF_2 = "52998224725";
    private static final String VALID_CPF_3 = "12345678909";

    @Test
    void shouldReturnZeroVotesWhenNoVotesWereRegistered() throws Exception {
        Long topicId = createTopic("Topic without votes", null);
        openSession(topicId, 60);

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(topicId))
                .andExpect(jsonPath("$.title").value("Topic without votes"))
                .andExpect(jsonPath("$.yesVotes").value(0))
                .andExpect(jsonPath("$.noVotes").value(0))
                .andExpect(jsonPath("$.totalVotes").value(0));
    }

    @Test
    void shouldTallyWhenYesIsGreaterThanNo() throws Exception {
        Long topicId = createTopic("Topic with more yes", null);
        openSession(topicId, 60);

        registerVote(topicId, VALID_CPF_1, VoteOption.YES);
        registerVote(topicId, VALID_CPF_2, VoteOption.YES);
        registerVote(topicId, VALID_CPF_3, VoteOption.NO);

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(2))
                .andExpect(jsonPath("$.noVotes").value(1))
                .andExpect(jsonPath("$.totalVotes").value(3));
    }

    @Test
    void shouldTallyWhenNoIsGreaterThanYes() throws Exception {
        Long topicId = createTopic("Topic with more no", null);
        openSession(topicId, 60);

        registerVote(topicId, VALID_CPF_1, VoteOption.NO);
        registerVote(topicId, VALID_CPF_2, VoteOption.NO);
        registerVote(topicId, VALID_CPF_3, VoteOption.YES);

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(1))
                .andExpect(jsonPath("$.noVotes").value(2))
                .andExpect(jsonPath("$.totalVotes").value(3));
    }

    @Test
    void shouldTallyWhenVotesAreTied() throws Exception {
        Long topicId = createTopic("Topic with tie", null);
        openSession(topicId, 60);

        registerVote(topicId, VALID_CPF_1, VoteOption.YES);
        registerVote(topicId, VALID_CPF_2, VoteOption.NO);

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(1))
                .andExpect(jsonPath("$.noVotes").value(1))
                .andExpect(jsonPath("$.totalVotes").value(2));
    }

    @Test
    void shouldReturnResultWhenSessionHasClosed() throws Exception {
        Long topicId = createTopic("Topic with closed session", null);
        openSession(topicId, 1); // 1 segundo

        registerVote(topicId, VALID_CPF_1, VoteOption.YES);

        Thread.sleep(1500); // aguarda a sessao encerrar

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(1))
                .andExpect(jsonPath("$.noVotes").value(0))
                .andExpect(jsonPath("$.sessionClosed").value(true));
    }

    @Test
    void shouldReturn404WhenTopicDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/topics/{id}/result", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }
}