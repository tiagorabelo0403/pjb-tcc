package com.tcc.pjb.backend.core.kernel.recursal.plan;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;

public record EdgeUpsert(
        String fromProceedingKey,
        String toProceedingKey,
        RecursalRelationType relationType,
        LegalAppealType appealType
) {

    public EdgeUpsert {
        fromProceedingKey = Objects.requireNonNull(fromProceedingKey, "fromProceedingKey é obrigatório").trim();
        toProceedingKey = Objects.requireNonNull(toProceedingKey, "toProceedingKey é obrigatório").trim();
        Objects.requireNonNull(relationType, "relationType é obrigatório");
        Objects.requireNonNull(appealType, "appealType é obrigatório");
        if (fromProceedingKey.isBlank() || toProceedingKey.isBlank()) {
            throw new IllegalArgumentException("proceedingKey vazio");
        }
    }
}
