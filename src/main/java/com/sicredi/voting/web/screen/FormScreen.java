package com.sicredi.voting.web.screen;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Tela do tipo FORMULARIO  
 *
 * <p>Exibe uma colecao de itens ({@link FormItem}) e ate dois botoes de
 * acao na parte inferior. O {@code tipo} e sempre {@code "FORMULARIO"}.</p>
 *
 * <p>O {@code botaoCancelar} e opcional. Quando {@code null}, o campo
 * nao aparece no JSON gracas a {@link JsonInclude.Include#NON_NULL}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormScreen(
        String tipo,
        String titulo,
        List<FormItem> itens,
        ScreenAction botaoOk,
        ScreenAction botaoCancelar
) {

    public static FormScreen of(String titulo,
                                List<FormItem> itens,
                                ScreenAction botaoOk,
                                ScreenAction botaoCancelar) {
        return new FormScreen("FORMULARIO", titulo, itens, botaoOk, botaoCancelar);
    }
}