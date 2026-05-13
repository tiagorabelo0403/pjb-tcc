package com.tcc.pjb.backend.core.plataforma.sustentacao.domain;

import java.util.List;
import java.util.Map;

public record PjbPlataformaSustentacaoEixo(
        String codigo,
        String titulo,
        int score,
        String status,
        boolean pronto,
        List<String> sinais,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        Map<String, Object> evidencias
) {
    public PjbPlataformaSustentacaoEixo {
        sinais = sinais == null ? List.of() : List.copyOf(sinais);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        evidencias = evidencias == null ? Map.of() : Map.copyOf(evidencias);
    }
}
