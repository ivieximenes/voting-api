package com.sicredi.voting.web.screen;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.domain.VotingSession;
import com.sicredi.voting.service.TopicService;
import com.sicredi.voting.service.VotingSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/screens")
@Tag(name = "Screens", description = "Mobile client screens as defined in Anexo 1 (FORMULARIO/SELECAO)")
public class ScreenController {

    private final TopicService topicService;
    private final VotingSessionService sessionService;
    private final String baseUrl;

    public ScreenController(
            TopicService topicService,
            VotingSessionService sessionService,
            @Value("${sicredi.app.base-url}") String baseUrl) {
        this.topicService = topicService;
        this.sessionService = sessionService;
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
        List<SelectionItem> itens = topicService.findAll().stream()
                .map(this::toSelectionItem)
                .toList();

        return SelectionScreen.of("Pautas", itens);
    }

    @GetMapping("/topics/{topicId}/vote")
    public SelectionScreen voteForm(@PathVariable Long topicId) {
        Topic topic = topicService.findById(topicId);

        String votesUrl = baseUrl + "/api/v1/topics/" + topic.getId() + "/votes";

        List<SelectionItem> itens = List.of(
                SelectionItem.of("Sim", votesUrl, Map.of("option", "SIM")),
                SelectionItem.of("Não", votesUrl, Map.of("option", "NAO"))
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
}
