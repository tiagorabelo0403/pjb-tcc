package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;

public record ProfessionalGrantTemplateBatchItemRequest(
        Long targetUserId,
        String processoNumero,
        String uf,
        String comarca,
        String tribunal,
        String unidadeJudiciariaCodigo,
        String orgaoColegiadoCodigo,
        String enteCode,
        Long targetMagistrateUserId,
        String sourceRef,
        String sourceLabel,
        String reason,
        Boolean requiresStepUp,
        LocalDateTime inicioVigencia,
        LocalDateTime fimVigencia
) {
}
