package com.tcc.pjb.backend.core.kernel.recursal.plan;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;

public record EdgeView(
        String fromProceedingKey,
        String toProceedingKey,
        RecursalRelationType relationType,
        LegalAppealType appealType
) {
}
