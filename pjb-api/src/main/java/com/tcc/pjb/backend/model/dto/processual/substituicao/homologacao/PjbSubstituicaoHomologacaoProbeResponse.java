package com.tcc.pjb.backend.model.dto.processual.substituicao.homologacao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoHomologacaoProbeSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PjbSubstituicaoHomologacaoProbeResponse(
        Long probeId,
        String tribunalCodigo,
        String probeCodigo,
        String connectorCodigo,
        String ambienteCodigo,
        PjbSubstituicaoHomologacaoProbeSituacao situacao,
        int gateScore,
        Map<String, Object> evidencias,
        Map<String, Object> resultado,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoHomologacaoProbeResponse {
        evidencias = evidencias == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(evidencias));
        resultado = resultado == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(resultado));
    }
}
