package com.tcc.pjb.backend.service.casefile;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;

public record CaseContinuityEdgeLink(
        String fromProceedingKey,
        String toProceedingKey,
        RecursalRelationType relationType,
        LegalAppealType appealType
) {
}
