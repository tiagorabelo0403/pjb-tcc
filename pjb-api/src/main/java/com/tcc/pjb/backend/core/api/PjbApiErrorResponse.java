package com.tcc.pjb.backend.core.api;

import java.time.Instant;
import java.util.List;

public record PjbApiErrorResponse(
        int status,
        String codigo,
        String mensagem,
        List<String> detalhes,
        Instant timestamp,
        String rastreioId
) {
    public static PjbApiErrorResponse de(int status, String codigo, String mensagem) {
        return new PjbApiErrorResponse(status, codigo, mensagem, List.of(), Instant.now(), null);
    }

    public static PjbApiErrorResponse comDetalhes(int status, String codigo,
            String mensagem, List<String> detalhes) {
        return new PjbApiErrorResponse(status, codigo, mensagem, detalhes, Instant.now(), null);
    }
}
