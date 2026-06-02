package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PjbSubstituicaoNacionalExecucaoEventoResponse(
        Long eventoId,
        String codigo,
        String severidade,
        PjbSubstituicaoExecucaoFase fase,
        String descricao,
        @Schema(description = "Detalhes do evento de execucao nacional — polimórfico por tipo de evento de substituicao", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> detalhes,
        Instant criadoEm
) {
    public PjbSubstituicaoNacionalExecucaoEventoResponse {
        detalhes = detalhes == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(detalhes));
        criadoEm = criadoEm == null ? Instant.now() : criadoEm;
    }
}

