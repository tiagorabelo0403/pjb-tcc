package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PjbSubstituicaoNacionalExecucaoAggregate(
        Long execucaoId,
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        PjbSubstituicaoExecucaoAcao acao,
        PjbSubstituicaoExecucaoSituacao situacao,
        PjbSubstituicaoExecucaoFase faseAtual,
        PjbSubstituicaoExecucaoModo modoExecucao,
        boolean dryRun,
        boolean gateAprovado,
        boolean rollbackReversivel,
        int gateScore,
        UUID jobId,
        String correlationId,
        String requestHash,
        String requestedBy,
        String justificativa,
        String ondaAlvo,
        Map<String, Object> payload,
        Map<String, Object> resultado,
        List<PjbSubstituicaoNacionalExecucaoEvento> eventos,
        Instant criadoEm,
        Instant iniciadoEm,
        Instant concluidoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoNacionalExecucaoAggregate {
        payload = payload == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        resultado = resultado == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(resultado));
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
        criadoEm = criadoEm == null ? Instant.now() : criadoEm;
        atualizadoEm = atualizadoEm == null ? criadoEm : atualizadoEm;
    }
}
