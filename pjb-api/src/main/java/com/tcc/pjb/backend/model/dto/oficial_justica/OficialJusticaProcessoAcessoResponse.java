package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record OficialJusticaProcessoAcessoResponse(
        Instant generatedAt,
        Long processoId,
        String processoNumero,
        boolean acessoPermitido,
        String fundamentoAcesso,
        @Schema(description = "Snapshot do processo judicial — estrutura varia por sistema de origem (PJe/eProc/eSAJ)", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> processo,
        List<VinculoRow> vinculos,
        List<PendenciaRelacionada> pendenciasRelacionadas,
        List<String> alerts
) {
    public OficialJusticaProcessoAcessoResponse {
        processo = processo == null ? Map.of() : Map.copyOf(processo);
        vinculos = vinculos == null ? List.of() : List.copyOf(vinculos);
        pendenciasRelacionadas = pendenciasRelacionadas == null ? List.of() : List.copyOf(pendenciasRelacionadas);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record VinculoRow(
            Long workItemId,
            String titulo,
            String tipo,
            String status,
            Instant dueAt,
            String queueCode,
            String inboxKey,
            String templateCode
    ) {
    }

    public record PendenciaRelacionada(
            Long workItemId,
            String titulo,
            String status,
            Instant dueAt,
            String proximaAcao
    ) {
    }
}

