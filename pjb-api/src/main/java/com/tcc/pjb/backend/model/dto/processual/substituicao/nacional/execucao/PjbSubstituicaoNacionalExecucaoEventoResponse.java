package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record PjbSubstituicaoNacionalExecucaoEventoResponse(
        Long eventoId,
        String codigo,
        String severidade,
        PjbSubstituicaoExecucaoFase fase,
        String descricao,
        Map<String, Object> detalhes,
        Instant criadoEm
) {
    public PjbSubstituicaoNacionalExecucaoEventoResponse {
        detalhes = detalhes == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(detalhes));
        criadoEm = criadoEm == null ? Instant.now() : criadoEm;
    }
}
