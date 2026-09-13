package com.sicredi.voting.web.v1;

import com.sicredi.voting.enums.VoteOption;
import com.sicredi.voting.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integracao do contexto "voto": registro de voto e apuracao,
 * incluindo cenarios de sucesso e erro.
 */
class VoteFlowIT extends AbstractIntegrationTest {

    private static final String VALID_CPF_1 = "11144477735";
    private static final String VALID_CPF_2 = "52998224725";

    @Test
    void shouldRegisterVoteWhenSessionIsOpen() throws Exception {
        Long topicId = createTopic("Topic for voting", null);
        openSession(topicId, 60);

        registerVote(topicId, VALID_CPF_1, VoteOption.SIM);

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(1))
                .andExpect(jsonPath("$.noVotes").value(0))
                .andExpect(jsonPath("$.totalVotes").value(1));
    }

    @Test
    void shouldTallyVotesWhenYesIsGreaterThanNo() throws Exception {
        Long topicId = createTopic("Topic with more yes", null);
        openSession(topicId, 60);

        registerVote(topicId, VALID_CPF_1, VoteOption.SIM);
        registerVote(topicId, VALID_CPF_2, VoteOption.SIM);

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(2))
                .andExpect(jsonPath("$.noVotes").value(0))
                .andExpect(jsonPath("$.totalVotes").value(2));
    }

    @Test
    void shouldTallyVotesWhenNoIsGreaterThanYes() throws Exception {
        Long topicId = createTopic("Topic with more no", null);
        openSession(topicId, 60);

        registerVote(topicId, VALID_CPF_1, VoteOption.NAO);
        registerVote(topicId, VALID_CPF_2, VoteOption.NAO);

        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(0))
                .andExpect(jsonPath("$.noVotes").value(2));
    }

    @Test
    void shouldReturn404WhenVotingWithoutOpenSession() throws Exception {
        Long topicId = createTopic("Topic without session", null);

        mockMvc.perform(post("/api/v1/topics/{id}/votes", topicId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","option":"SIM"}
                                """.formatted(VALID_CPF_1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldReturn409WhenMemberVotesTwiceOnSameTopic() throws Exception {
        Long topicId = createTopic("Topic with duplicate vote", null);
        openSession(topicId, 60);
        registerVote(topicId, VALID_CPF_1, VoteOption.SIM);

        mockMvc.perform(post("/api/v1/topics/{id}/votes", topicId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","option":"NAO"}
                                """.formatted(VALID_CPF_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldReturn400WhenMemberIdIsMissing() throws Exception {
        Long topicId = createTopic("Topic with invalid vote", null);
        openSession(topicId, 60);

        mockMvc.perform(post("/api/v1/topics/{id}/votes", topicId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"option":"SIM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400WhenMemberIdHasInvalidLength() throws Exception {
        Long topicId = createTopic("Topic with invalid CPF", null);
        openSession(topicId, 60);

        mockMvc.perform(post("/api/v1/topics/{id}/votes", topicId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"memberId":"123","option":"SIM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn422WhenVotingAfterSessionCloses() throws Exception {
        Long topicId = createTopic("Topic with short session", null);
        openSession(topicId, 1); // 1 segundo

        Thread.sleep(1500); // aguarda a sessao encerrar

        mockMvc.perform(post("/api/v1/topics/{id}/votes", topicId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","option":"SIM"}
                                """.formatted(VALID_CPF_1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").exists());
    }
}