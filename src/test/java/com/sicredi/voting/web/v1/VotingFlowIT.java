package com.sicredi.voting.web.v1;

import com.sicredi.voting.enums.VoteOption;
import com.sicredi.voting.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo de ponta a ponta descrito no desafio:
 * cadastrar pauta -> abrir sessao -> registrar votos -> apurar resultado.
 *
 * <p>Um unico teste, para provar que todas as camadas funcionam juntas.
 * Cenarios isolados de sucesso e erro de cada contexto ficam em
 * {@link TopicFlowIT}, {@link VotingSessionFlowIT} e {@link VoteFlowIT}.</p>
 */
class VotingFlowIT extends AbstractIntegrationTest {

    private static final String VALID_CPF_1 = "11144477735";
    private static final String VALID_CPF_2 = "52998224725";

    @Test
    void shouldRunFullVotingFlow() throws Exception {
        // 1. Cadastra pauta
        Long topicId = createTopic(
                "Balance sheet approval",
                "Vote on the fiscal year balance sheet");

        // 2. Abre sessao
        openSession(topicId, 5);

        // 3. Registra votos
        registerVote(topicId, VALID_CPF_1, VoteOption.YES);
        registerVote(topicId, VALID_CPF_2, VoteOption.NO);

        // 4. Apura resultado
        mockMvc.perform(get("/api/v1/topics/{id}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(topicId))
                .andExpect(jsonPath("$.yesVotes").value(1))
                .andExpect(jsonPath("$.noVotes").value(1))
                .andExpect(jsonPath("$.totalVotes").value(2));
    }
}