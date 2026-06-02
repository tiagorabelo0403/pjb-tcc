package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Metadados de ciclo de vida pos-transito — heterogeneos por fase de arquivamento", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public PostArchiveLifecycleResponse {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

