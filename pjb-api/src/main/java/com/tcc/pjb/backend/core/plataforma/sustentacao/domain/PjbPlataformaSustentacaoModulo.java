package com.tcc.pjb.backend.core.plataforma.sustentacao.domain;

import java.util.List;

public record PjbPlataformaSustentacaoModulo(
        String codigo,
        String titulo,
        String camada,
        int beansConectados,
        int score,
        String status,
        List<String> conexoes,
        List<String> riscos
) {
    public PjbPlataformaSustentacaoModulo {
        conexoes = conexoes == null ? List.of() : List.copyOf(conexoes);
        riscos = riscos == null ? List.of() : List.copyOf(riscos);
    }
}
