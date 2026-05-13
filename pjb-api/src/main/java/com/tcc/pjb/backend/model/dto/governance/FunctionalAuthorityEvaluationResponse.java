package com.tcc.pjb.backend.model.dto.governance;

import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.OperacaoProcessualCritica;

public record FunctionalAuthorityEvaluationResponse(
        Long processoId,
        String numeroProcesso,
        OperacaoProcessualCritica operacao,
        String perfilExecutor,
        boolean permitido,
        String autoridadeResponsavel,
        boolean exigeStepUp,
        boolean exigeDuplaAprovacao,
        boolean exigeRevisaoIndependente,
        List<String> capacidadesAtivas,
        List<String> exigencias,
        List<String> restricoes,
        List<String> fundamentos
) {
}
