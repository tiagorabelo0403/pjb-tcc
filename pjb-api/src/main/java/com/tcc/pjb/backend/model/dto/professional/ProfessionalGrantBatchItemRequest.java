package com.tcc.pjb.backend.model.dto.professional;

import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessBasis;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import java.time.LocalDateTime;

public record ProfessionalGrantBatchItemRequest(
        Long targetUserId,
        ProfessionalActorClass actorClass,
        ProfessionalAccessGrantType grantType,
        ProfessionalAccessBasis accessBasis,
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
