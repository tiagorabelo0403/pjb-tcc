package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoModo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PjbSubstituicaoNacionalExecucaoResponse(
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
        List<PjbSubstituicaoNacionalExecucaoEventoResponse> eventos,
        Instant criadoEm,
        Instant iniciadoEm,
        Instant concluidoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoNacionalExecucaoResponse {
        payload = payload == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        resultado = resultado == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(resultado));
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
        criadoEm = criadoEm == null ? Instant.now() : criadoEm;
        atualizadoEm = atualizadoEm == null ? criadoEm : atualizadoEm;
    }
}
