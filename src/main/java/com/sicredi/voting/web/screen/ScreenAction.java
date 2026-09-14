package com.sicredi.voting.web.screen;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Botao de acao exibido no rodape de uma tela
 *
 * <p>Contem o {@code texto} do botao, a {@code url} para onde o app envia
 * a requisicao quando o botao e acionado, e um {@code body} opcional com
 * os dados fixos que devem ser enviados junto com os valores dos campos.</p>
 *
 * <p>Para botoes sem corpo (ex: "Cancelar"), use
 * {@link #of(String, String)} - o campo {@code body} nao aparece no JSON.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScreenAction(
        String texto,
        String url,
        Map<String, Object> body
) {

    public static ScreenAction of(String texto, String url, Map<String, Object> body) {
        return new ScreenAction(texto, url, body);
    }

    public static ScreenAction of(String texto, String url) {
        return new ScreenAction(texto, url, null);
    }
}