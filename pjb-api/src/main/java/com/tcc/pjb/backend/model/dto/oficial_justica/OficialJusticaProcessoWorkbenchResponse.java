package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OficialJusticaProcessoWorkbenchResponse(
        Instant generatedAt,
        Long processoId,
        String processoNumero,
        boolean acessoProcessoPermitido,
        String fundamentoAcesso,
        Map<String, Object> unidadeContexto,
        Summary summary,
        Map<String, Object> securityEnvelope,
        List<FolderBucket> pastas,
        Map<String, Object> legendaAndamento,
        Map<String, Object> calculadoraJudicial,
        Map<String, Object> assistenciaOperacionalIa,
        List<PendingAction> pendencias,
        List<String> alerts
) {
    public OficialJusticaProcessoWorkbenchResponse {
        unidadeContexto = unidadeContexto == null ? Map.of() : Map.copyOf(unidadeContexto);
        securityEnvelope = securityEnvelope == null ? Map.of() : Map.copyOf(securityEnvelope);
        pastas = pastas == null ? List.of() : List.copyOf(pastas);
        legendaAndamento = legendaAndamento == null ? Map.of() : Map.copyOf(legendaAndamento);
        calculadoraJudicial = calculadoraJudicial == null ? Map.of() : Map.copyOf(calculadoraJudicial);
        assistenciaOperacionalIa = assistenciaOperacionalIa == null ? Map.of() : Map.copyOf(assistenciaOperacionalIa);
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record Summary(
            String tribunal,
            String vara,
            String rito,
            String ramo,
            String faseAtual,
            String processoStatus,
            String sigilo,
            String resumoCompleto,
            String fundamentoOperacional,
            String alvoPrincipal,
            String localidadeAlvo,
            int pendenciasAtivas,
            boolean possuiPendenciaCritica,
            boolean somenteLeitura
    ) {
    }

    public record FolderBucket(
            String code,
            String label,
            int count,
            String colorToken,
            String activationPath
    ) {
    }

    public record PendingAction(
            Long workItemId,
            String titulo,
            String categoria,
            String prioridadeOperacional,
            Instant prazoFatalEm,
            String proximaAcao,
            String corAndamento
    ) {
    }
}
