package com.sicredi.voting.web.screen;

import com.sicredi.voting.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScreenControllerIT extends AbstractIntegrationTest {

    @Value("${sicredi.app.base-url}")
    private String baseUrl;

    @Test
    void shouldReturnFormScreenForNewTopic() throws Exception {
        mockMvc.perform(get("/api/v1/screens/topics/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.titulo").value("Nova pauta"))
                .andExpect(jsonPath("$.itens[0].tipo").value("INPUT_TEXTO"))
                .andExpect(jsonPath("$.itens[0].id").value("title"))
                .andExpect(jsonPath("$.itens[1].id").value("description"))
                .andExpect(jsonPath("$.botaoOk.texto").value("Cadastrar"))
                .andExpect(jsonPath("$.botaoOk.url").value(baseUrl + "/api/v1/topics"))
                .andExpect(jsonPath("$.botaoCancelar.texto").value("Cancelar"))
                .andExpect(jsonPath("$.botaoCancelar.url").value(baseUrl + "/api/v1/screens/topics"));
    }

    @Test
    void shouldReturnSelectionScreenListingTopics() throws Exception {
        Long topicId = createTopic("Balance sheet approval", "Vote on the fiscal year balance sheet");

        mockMvc.perform(get("/api/v1/screens/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Balance sheet approval')]").exists())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Balance sheet approval')].url")
                        .value(baseUrl + "/api/v1/topics/" + topicId + "/result"));
    }

    @Test
    void shouldPointToVoteScreenWhenSessionIsOpen() throws Exception {
        Long topicId = createTopic("Open topic", null);
        openSession(topicId, 3600);  // 1 hora

        mockMvc.perform(get("/api/v1/screens/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Open topic')].url")
                        .value(baseUrl + "/api/v1/screens/topics/" + topicId + "/vote"));
    }

    @Test
    void shouldPointToResultScreenWhenSessionIsClosed() throws Exception {
        Long topicId = createTopic("Closed topic", null);
        openSession(topicId, 1);  // 1 segundo

        Thread.sleep(1500);  // aguarda a sessão encerrar

        mockMvc.perform(get("/api/v1/screens/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Closed topic')].url")
                        .value(baseUrl + "/api/v1/topics/" + topicId + "/result"));
    }

    @Test
    void shouldReturnSelectionScreenWithYesAndNoOptions() throws Exception {
        Long topicId = createTopic("Budget approval", null);

        mockMvc.perform(get("/api/v1/screens/topics/{topicId}/vote", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.titulo").value("Budget approval"))
                .andExpect(jsonPath("$.itens[0].texto").value("Sim"))
                .andExpect(jsonPath("$.itens[0].url")
                        .value(baseUrl + "/api/v1/topics/" + topicId + "/votes"))
                .andExpect(jsonPath("$.itens[0].body.option").value("SIM"))
                .andExpect(jsonPath("$.itens[1].texto").value("Não"))
                .andExpect(jsonPath("$.itens[1].body.option").value("NAO"));
    }

    @Test
    void shouldReturn404WhenTopicDoesNotExistForVoteScreen() throws Exception {
        mockMvc.perform(get("/api/v1/screens/topics/{topicId}/vote", 999999L))
                .andExpect(status().isNotFound());
    }
}