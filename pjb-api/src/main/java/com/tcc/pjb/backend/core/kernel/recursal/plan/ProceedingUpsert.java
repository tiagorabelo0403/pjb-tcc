package com.tcc.pjb.backend.core.kernel.recursal.plan;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public record ProceedingUpsert(
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

    public ProceedingUpsert {
        proceedingKey = Objects.requireNonNull(proceedingKey, "proceedingKey é obrigatório").trim();
        if (proceedingKey.isBlank()) throw new IllegalArgumentException("proceedingKey vazio");
        Objects.requireNonNull(status, "status é obrigatório");
        Objects.requireNonNull(instanceLevel, "instanceLevel é obrigatório");
        court = Objects.toString(court, "").trim();
        numeroUnificado = Objects.toString(numeroUnificado, "").trim();
        if (secrecy == null) secrecy = NivelSigilo.PUBLICO;
        if (sourceSystem == null) sourceSystem = LegalIntegrationSystem.OTHER;
    }
}
