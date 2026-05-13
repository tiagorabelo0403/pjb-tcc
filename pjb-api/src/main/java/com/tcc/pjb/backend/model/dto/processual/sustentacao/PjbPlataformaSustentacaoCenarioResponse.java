package com.tcc.pjb.backend.model.dto.processual.sustentacao;

import java.util.List;

public record PjbPlataformaSustentacaoCenarioResponse(
        String codigo,
        String titulo,
        String tribunalCodigo,
        String ramo,
        String rito,
        int score,
        boolean apto,
        List<String> alertas,
        List<String> fundamentos
) {
}
