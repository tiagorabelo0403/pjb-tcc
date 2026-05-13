package com.tcc.pjb.backend.model.dto.governance;

import java.util.List;

public record SensitiveDataAccessResponse(
        Long processoId,
        String numeroProcesso,
        String perfilExecutor,
        String nivelSensibilidade,
        boolean permitido,
        boolean exigeStepUp,
        boolean exigeJustificativa,
        boolean exigeDuplaAprovacao,
        List<String> camposVisiveis,
        List<String> camposMascarados,
        List<String> fundamentos,
        List<String> restricoes
) {
}
