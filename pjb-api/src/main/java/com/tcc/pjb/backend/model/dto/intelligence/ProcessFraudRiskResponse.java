package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record ProcessFraudRiskResponse(
        Long processoId,
        String nivelGlobal,
        int scoreGlobal,
        boolean exigeRevisaoHumana,
        boolean cpfValido,
        boolean enderecoSuspeito,
        boolean litiganciaMassificada,
        List<FraudSignal> sinais,
        List<String> fundamentos,
        List<String> recomendacoes
) {
    public record FraudSignal(
            String codigo,
            String categoria,
            String nivel,
            int score,
            String titulo,
            List<String> fundamentos
    ) {
    }
}
