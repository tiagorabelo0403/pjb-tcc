package com.tcc.pjb.backend.core.processo.sigilo.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoSigiloGuarda(
        String code,
        String title,
        String scope,
        String severity,
        boolean mandatory,
        boolean blocking,
        boolean requiresInstitutionalContext,
        List<String> allowedModes,
        List<String> requiredCapabilities
) {
    public ProcessoSigiloGuarda {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        scope = scope == null ? "PROCESSO" : scope;
        severity = severity == null ? "CONTROLADA" : severity;
        allowedModes = allowedModes == null ? List.of() : List.copyOf(allowedModes);
        requiredCapabilities = requiredCapabilities == null ? List.of() : List.copyOf(requiredCapabilities);
    }
}
