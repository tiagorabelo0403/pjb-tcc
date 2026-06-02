package com.tcc.pjb.backend.model.dto.processual.substituicao.homologacao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoHomologacaoProbeSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PjbSubstituicaoHomologacaoProbeResponse(
        Long probeId,
        String tribunalCodigo,
        String probeCodigo,
        String connectorCodigo,
        String ambienteCodigo,
        PjbSubstituicaoHomologacaoProbeSituacao situacao,
        int gateScore,
        @Schema(description = "Evidencias de homologacao — heterogeneas por tipo de probe de validacao", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> evidencias,
        @Schema(description = "Resultado de homologacao — heterogeneo por tipo de probe de validacao", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> resultado,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoHomologacaoProbeResponse {
        evidencias = evidencias == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(evidencias));
        resultado = resultado == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(resultado));
    }
}

