package com.sicredi.voting.web.screen;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Item de uma tela do tipo FORMULARIO (ver Anexo 1 do enunciado).
 *
 * <p>O campo {@code tipo} define qual dos demais campos e relevante:
 * <ul>
 *     <li>{@code TEXTO}         -> usa apenas {@code texto}</li>
 *     <li>{@code INPUT_TEXTO}   -> usa {@code id}, {@code titulo}, {@code valor} (String)</li>
 *     <li>{@code INPUT_NUMERO}  -> usa {@code id}, {@code titulo}, {@code valor} (Number)</li>
 *     <li>{@code INPUT_DATA}    -> usa {@code id}, {@code titulo}, {@code valor} (String)</li>
 * </ul>
 * Campos nao usados ficam {@code null} e nao aparecem no JSON gracas a
 * {@link JsonInclude.Include#NON_NULL}.</p>
 *
 * <p>Use os factory methods em vez do construtor direto - eles garantem
 * que apenas os campos corretos sejam preenchidos para cada tipo.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormItem(
        FormItemType tipo,
        String texto,
        String id,
        String titulo,
        Object valor
) {

    public static FormItem staticText(String texto) {
        return new FormItem(FormItemType.TEXTO, texto, null, null, null);
    }

    public static FormItem inputTexto(String id, String titulo, String valor) {
        return new FormItem(FormItemType.INPUT_TEXTO, null, id, titulo, valor);
    }

    public static FormItem inputNumero(String id, String titulo, Number valor) {
        return new FormItem(FormItemType.INPUT_NUMERO, null, id, titulo, valor);
    }

    public static FormItem inputData(String id, String titulo, String valor) {
        return new FormItem(FormItemType.INPUT_DATA, null, id, titulo, valor);
    }
}