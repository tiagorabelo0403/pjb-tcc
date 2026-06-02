package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record OficialJusticaProcessoWorkbenchResponse(
        Instant generatedAt,
        Long processoId,
        String processoNumero,
        boolean acessoProcessoPermitido,
        String fundamentoAcesso,
        @Schema(description = "Contexto da unidade judicial executora — chaves variam por tribunal e tipo de diligencia", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> unidadeContexto,
        Summary summary,
        @Schema(description = "Envelope de seguranca do workbench — dados de autenticacao e autorizacao por sessao", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> securityEnvelope,
        List<FolderBucket> pastas,
        @Schema(description = "Legenda de andamento processual — chaves variam por rito e fase processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> legendaAndamento,
        @Schema(description = "Estado da calculadora judicial integrada — parametros variam por tipo de calculo", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> calculadoraJudicial,
        @Schema(description = "Assistencia operacional da IA — sugestoes variam por tipo de diligencia e tribunal", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
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

