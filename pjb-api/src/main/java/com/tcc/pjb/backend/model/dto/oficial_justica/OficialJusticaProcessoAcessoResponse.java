package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OficialJusticaProcessoAcessoResponse(
        Instant generatedAt,
        Long processoId,
        String processoNumero,
        boolean acessoPermitido,
        String fundamentoAcesso,
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
