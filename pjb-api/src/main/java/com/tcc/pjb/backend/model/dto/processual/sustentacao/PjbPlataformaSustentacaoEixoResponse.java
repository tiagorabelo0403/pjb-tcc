package com.tcc.pjb.backend.model.dto.processual.sustentacao;

import java.util.List;
import java.util.Map;

public record PjbPlataformaSustentacaoEixoResponse(
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
}
