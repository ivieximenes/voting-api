package com.sicredi.voting.web.screen;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.service.TopicService;
import com.sicredi.voting.service.VoteService;
import com.sicredi.voting.service.VotingResult;
import com.sicredi.voting.service.VotingSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/screens")
@Tag(name = "Screens", description = "Mobile client screens as defined in Anexo 1 (FORMULARIO/SELECAO)")
public class ScreenController {

    private final TopicService topicService;
    private final VotingSessionService sessionService;
    private final VoteService voteService;
    private final String baseUrl;

    public ScreenController(
            TopicService topicService,
            VotingSessionService sessionService,
            VoteService voteService,
            @Value("${sicredi.app.base-url}") String baseUrl) {
        this.topicService = topicService;
        this.sessionService = sessionService;
        this.voteService = voteService;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/topics/new")
    public FormScreen newTopicForm() {
        List<FormItem> itens = List.of(
                FormItem.inputTexto("title", "Título da pauta", null),
                FormItem.inputTexto("description", "Descrição", null)
        );

        ScreenAction botaoOk = ScreenAction.of(
                "Cadastrar",
                baseUrl + "/api/v1/topics");

        ScreenAction botaoCancelar = ScreenAction.of(
                "Cancelar",
                baseUrl + "/api/v1/screens/topics");

        return FormScreen.of("Nova pauta", itens, botaoOk, botaoCancelar);
    }

    @GetMapping("/topics")
    public SelectionScreen topicList() {
        // Cada item leva ao hub de detalhe da pauta, nunca direto a um endpoint REST:
        // é o hub (abaixo) que decide, a cada acesso, quais ações fazem sentido.
        List<SelectionItem> itens = topicService.findAll().stream()
                .map(topic -> SelectionItem.of(
                        topic.getTitle(),
                        baseUrl + "/api/v1/screens/topics/" + topic.getId()))
                .toList();

        return SelectionScreen.of("Pautas", itens);
    }

    @GetMapping("/topics/{topicId}")
    public SelectionScreen topicDetail(@PathVariable Long topicId) {
        Topic topic = topicService.findById(topicId);
        Optional<VotingSession> session = sessionService.findOptionalByTopicId(topicId);

        List<SelectionItem> itens = new ArrayList<>();

        if (session.isEmpty()) {
            itens.add(SelectionItem.of(
                    "Abrir sessão de votação",
                    baseUrl + "/api/v1/screens/topics/" + topicId + "/sessions/new"));
        } else if (session.get().isOpen()) {
            itens.add(SelectionItem.of(
                    "Votar",
                    baseUrl + "/api/v1/screens/topics/" + topicId + "/vote"));
        }

        if (session.isPresent()) {
            itens.add(SelectionItem.of(
                    "Ver resultado",
                    baseUrl + "/api/v1/screens/topics/" + topicId + "/result"));
        }

        String titulo = topic.getTitle()
                + (topic.getDescription() != null ? " — " + topic.getDescription() : "");

        return SelectionScreen.of(titulo, itens);
    }

    @GetMapping("/topics/{topicId}/sessions/new")
    public FormScreen openSessionForm(@PathVariable Long topicId) {
        Topic topic = topicService.findById(topicId);

        List<FormItem> itens = List.of(
                FormItem.staticText("Informe a duração da sessão em segundos, ou deixe em branco para usar o padrão."),
                FormItem.inputNumero("durationSeconds", "Duração (segundos)", null)
        );

        ScreenAction botaoOk = ScreenAction.of(
                "Abrir sessão",
                baseUrl + "/api/v1/topics/" + topicId + "/sessions");

        ScreenAction botaoCancelar = ScreenAction.of(
                "Cancelar",
                baseUrl + "/api/v1/screens/topics/" + topicId);

        return FormScreen.of("Abrir sessão: " + topic.getTitle(), itens, botaoOk, botaoCancelar);
    }

    @GetMapping("/topics/{topicId}/vote")
    public SelectionScreen voteForm(@PathVariable Long topicId) {
        Topic topic = topicService.findById(topicId);

        String votesUrl = baseUrl + "/api/v1/topics/" + topic.getId() + "/votes";

        List<SelectionItem> itens = List.of(
                SelectionItem.of("Sim", votesUrl, Map.of("option", "YES")),
                SelectionItem.of("Não", votesUrl, Map.of("option", "NO"))
        );

        return SelectionScreen.of(topic.getTitle(), itens);
    }

    private SelectionItem toSelectionItem(Topic topic) {
        Optional<VotingSession> session = sessionService.findOptionalByTopicId(topic.getId());

        // Sessão aberta: leva à tela de voto. Caso contrário (sem sessão ou já encerrada):
        // leva direto ao endpoint REST de resultado, que não precisa de formato de tela.
        String url = (session.isPresent() && session.get().isOpen())
                ? baseUrl + "/api/v1/screens/topics/" + topic.getId() + "/vote"
                : baseUrl + "/api/v1/topics/" + topic.getId() + "/result";

        return SelectionItem.of(topic.getTitle(), url);
    }

    @GetMapping("/topics/{topicId}/result")
    public FormScreen resultScreen(@PathVariable Long topicId) {
        VotingResult result = voteService.tally(topicId);

        List<FormItem> itens = List.of(
                FormItem.staticText(result.sessionClosed() ? "Sessão encerrada" : "Sessão em andamento"),
                FormItem.staticText("Sim: " + result.yesVotes()),
                FormItem.staticText("Não: " + result.noVotes()),
                FormItem.staticText("Total de votos: " + result.total())
        );

        ScreenAction botaoCancelar = ScreenAction.of(
                "Voltar",
                baseUrl + "/api/v1/screens/topics/" + topicId);

        return FormScreen.of("Resultado: " + result.topic().getTitle(), itens, null, botaoCancelar);
    }
}
