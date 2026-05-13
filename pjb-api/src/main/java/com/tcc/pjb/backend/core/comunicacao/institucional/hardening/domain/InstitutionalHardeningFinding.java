package com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalHardeningFinding(
        String code,
        InstitutionalHardeningSeverity severity,
        String message,
        List<String> evidencias
) {
    public InstitutionalHardeningFinding {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        evidencias = PayloadMaps.copyDistinctStrings(evidencias);
    }
}
