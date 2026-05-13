package com.tcc.pjb.backend.core.kernel.recursal.plan;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;

public record SyncDirective(
        LegalIntegrationSystem system,
        String proceedingKey,
        String numeroOrHint,
        InstanceLevel targetInstance,
        String targetCourt,
        int priority
) {

    public SyncDirective {
        Objects.requireNonNull(system, "system é obrigatório");
        proceedingKey = Objects.requireNonNull(proceedingKey, "proceedingKey é obrigatório").trim();
        numeroOrHint = Objects.toString(numeroOrHint, "").trim();
        Objects.requireNonNull(targetInstance, "targetInstance é obrigatório");
        targetCourt = Objects.toString(targetCourt, "").trim();
        if (priority < 0) priority = 0;
        if (priority > 100) priority = 100;
    }
}
