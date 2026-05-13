package com.tcc.pjb.backend.core.kernel.recursal.plan;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public record ProceedingView(
        String proceedingKey,
        boolean shadow,
        CaseProceedingStatus status,
        InstanceLevel instanceLevel,
        String court,
        String numeroUnificado,
        Long linkedProcessoId,
        NivelSigilo secrecy,
        LegalIntegrationSystem sourceSystem
) {
}
