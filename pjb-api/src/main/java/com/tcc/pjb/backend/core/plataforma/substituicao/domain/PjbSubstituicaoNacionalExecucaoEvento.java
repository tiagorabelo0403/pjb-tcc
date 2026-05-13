package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record PjbSubstituicaoNacionalExecucaoEvento(
        Long eventoId,
        String codigo,
        String severidade,
        PjbSubstituicaoExecucaoFase fase,
        String descricao,
        Map<String, Object> detalhes,
        Instant criadoEm
) {
    public PjbSubstituicaoNacionalExecucaoEvento {
        detalhes = detalhes == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(detalhes));
        criadoEm = criadoEm == null ? Instant.now() : criadoEm;
    }
}
