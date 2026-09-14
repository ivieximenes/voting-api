package com.sicredi.voting.web.screen;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SelectionScreen(
        String tipo,
        String titulo,
        List<SelectionItem> itens
) {

    public static SelectionScreen of(String titulo, List<SelectionItem> itens) {
        return new SelectionScreen("SELECAO", titulo, itens);
    }
}
