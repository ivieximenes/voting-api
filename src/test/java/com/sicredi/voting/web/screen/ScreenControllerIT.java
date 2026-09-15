package com.sicredi.voting.web.screen;

import com.sicredi.voting.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integracao da camada de screens (Anexo 1).
 *
 * <p>Cobre o contrato de tela FORMULARIO/SELECAO exposto em
 * {@code /api/v1/screens}: formulario de cadastro de pauta, listagem de
 * pautas, detalhe da pauta (com acoes contextuais), formulario de abertura
 * de sessao, tela de voto e tela de resultado.</p>
 */
class ScreenControllerIT extends AbstractIntegrationTest {

    @Value("${sicredi.app.base-url}")
    private String baseUrl;

    // ------------------------------------------------------------------
    // FORMULARIO — cadastro de pauta
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // SELECAO — listagem de pautas
    // ------------------------------------------------------------------

    @Test
    void shouldReturnSelectionScreenListingTopics() throws Exception {
        Long topicId = createTopic("Balance sheet approval", "Vote on the fiscal year balance sheet");

        mockMvc.perform(get("/api/v1/screens/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Balance sheet approval')]").exists())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Balance sheet approval')].url")
                        .value(baseUrl + "/api/v1/screens/topics/" + topicId));
    }

    @Test
    void shouldPointToTopicDetailRegardlessOfSessionState() throws Exception {
        Long withoutSession = createTopic("Topic without session", null);
        Long withSession = createTopic("Topic with session", null);
        openSession(withSession, 3600);

        mockMvc.perform(get("/api/v1/screens/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Topic without session')].url")
                        .value(baseUrl + "/api/v1/screens/topics/" + withoutSession))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Topic with session')].url")
                        .value(baseUrl + "/api/v1/screens/topics/" + withSession));
    }

    // ------------------------------------------------------------------
    // SELECAO — detalhe da pauta (acoes contextuais)
    // ------------------------------------------------------------------

    @Test
    void shouldOfferOpenSessionActionWhenTopicHasNoSession() throws Exception {
        Long topicId = createTopic("Topic without session", null);

        mockMvc.perform(get("/api/v1/screens/topics/{topicId}", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Abrir sessão de votação')]").exists())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Votar')]").doesNotExist())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Ver resultado')]").doesNotExist());
    }

    @Test
    void shouldOfferVoteAndResultActionsWhenSessionIsOpen() throws Exception {
        Long topicId = createTopic("Open topic", null);
        openSession(topicId, 3600);

        mockMvc.perform(get("/api/v1/screens/topics/{topicId}", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Votar')].url")
                        .value(baseUrl + "/api/v1/screens/topics/" + topicId + "/vote"))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Ver resultado')].url")
                        .value(baseUrl + "/api/v1/screens/topics/" + topicId + "/result"));
    }

    @Test
    void shouldOfferOnlyResultActionWhenSessionIsClosed() throws Exception {
        Long topicId = createTopic("Closed topic", null);
        openSession(topicId, 1);
        Thread.sleep(1500); // aguarda a sessao encerrar

        mockMvc.perform(get("/api/v1/screens/topics/{topicId}", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Votar')]").doesNotExist())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Ver resultado')].url")
                        .value(baseUrl + "/api/v1/screens/topics/" + topicId + "/result"));
    }

    // ------------------------------------------------------------------
    // FORMULARIO — abertura de sessao
    // ------------------------------------------------------------------

    @Test
    void shouldReturnFormScreenToOpenSession() throws Exception {
        Long topicId = createTopic("Topic to open session", null);

        mockMvc.perform(get("/api/v1/screens/topics/{topicId}/sessions/new", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[?(@.id == 'durationSeconds')].tipo").value("INPUT_NUMERO"))
                .andExpect(jsonPath("$.botaoOk.texto").value("Abrir sessão"))
                .andExpect(jsonPath("$.botaoOk.url")
                        .value(baseUrl + "/api/v1/topics/" + topicId + "/sessions"))
                .andExpect(jsonPath("$.botaoCancelar.url")
                        .value(baseUrl + "/api/v1/screens/topics/" + topicId));
    }

    // ------------------------------------------------------------------
    // SELECAO — tela de voto
    // ------------------------------------------------------------------

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
                .andExpect(jsonPath("$.itens[0].body.option").value("YES"))
                .andExpect(jsonPath("$.itens[1].texto").value("Não"))
                .andExpect(jsonPath("$.itens[1].body.option").value("NO"));
    }

    // ------------------------------------------------------------------
    // FORMULARIO — tela de resultado
    // ------------------------------------------------------------------

    @Test
    void shouldReturnFormScreenWithResultAsStaticText() throws Exception {
        Long topicId = createTopic("Topic with votes", null);
        openSession(topicId, 3600);

        mockMvc.perform(get("/api/v1/screens/topics/{topicId}/result", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Sim: 0')]").exists())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Não: 0')]").exists())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Total de votos: 0')]").exists())
                .andExpect(jsonPath("$.botaoOk").doesNotExist())
                .andExpect(jsonPath("$.botaoCancelar.texto").value("Voltar"));
    }

    // ------------------------------------------------------------------
    // Cenarios de erro
    // ------------------------------------------------------------------

    @Test
    void shouldReturn404WhenTopicDoesNotExistForVoteScreen() throws Exception {
        mockMvc.perform(get("/api/v1/screens/topics/{topicId}/vote", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenTopicDoesNotExistForDetailScreen() throws Exception {
        mockMvc.perform(get("/api/v1/screens/topics/{topicId}", 999999L))
                .andExpect(status().isNotFound());
    }
}