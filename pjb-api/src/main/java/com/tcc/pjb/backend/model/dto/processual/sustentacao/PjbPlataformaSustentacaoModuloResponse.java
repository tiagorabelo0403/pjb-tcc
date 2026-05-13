package com.tcc.pjb.backend.model.dto.processual.sustentacao;

import java.util.List;

public record PjbPlataformaSustentacaoModuloResponse(
        String codigo,
        String titulo,
        String camada,
        int beansConectados,
        int score,
        String status,
        List<String> conexoes,
        List<String> riscos
) {
}
