package com.sicredi.voting.web.screen;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SelectionItem(
        String texto,
        String url,
        Map<String, Object> body
) {

    public static SelectionItem of(String texto, String url, Map<String, Object> body) {
        return new SelectionItem(texto, url, body);
    }

    public static SelectionItem of(String texto, String url) {
        return new SelectionItem(texto, url, null);
    }
}
