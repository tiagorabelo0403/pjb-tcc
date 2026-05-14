package com.tcc.pjb.backend.core.api;

import java.time.Instant;
import java.util.List;

public record PjbApiResponseEnvelope<T>(
        boolean sucesso,
        T dados,
        List<String> erros,
        List<String> avisos,
        PjbPaginationMetadata paginacao,
        Instant timestamp,
        String versao
) {
    public static <T> PjbApiResponseEnvelope<T> ok(T dados) {
        return new PjbApiResponseEnvelope<>(true, dados, List.of(), List.of(), null, Instant.now(), "1");
    }

    public static <T> PjbApiResponseEnvelope<T> paginado(T dados, PjbPaginationMetadata paginacao) {
        return new PjbApiResponseEnvelope<>(true, dados, List.of(), List.of(), paginacao, Instant.now(), "1");
    }

    public static <T> PjbApiResponseEnvelope<T> erro(List<String> erros) {
        return new PjbApiResponseEnvelope<>(false, null, erros, List.of(), null, Instant.now(), "1");
    }
}
