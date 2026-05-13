package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;

public record PostArchiveLifecycleResponse(
        Long processoId,
        String numeroProcesso,
        String statusAtual,
        boolean aptoArquivamentoDefinitivo,
        boolean desarquivamentoRecomendado,
        boolean desarquivamentoSolicitado,
        int totalPendenciasAbertas,
        long totalDocumentos,
        int totalMovimentacoesRecentes,
        List<String> alertas,
        Map<String, Object> metadata
) {
    public PostArchiveLifecycleResponse {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
