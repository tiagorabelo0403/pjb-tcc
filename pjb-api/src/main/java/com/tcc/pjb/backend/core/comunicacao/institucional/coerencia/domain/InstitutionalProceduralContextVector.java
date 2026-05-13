package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProceduralContextVector(
        String profileCode,
        String displayName,
        String panel,
        String processProfile,
        String trustFloor,
        String ritoProcessual,
        String faseProcessual,
        String statusProcessual,
        String ramoDireito,
        boolean recursal,
        boolean embargos,
        boolean execucao,
        boolean urgente,
        boolean custodial,
        boolean technical,
        boolean governance,
        List<String> fundamentos
) {
    public InstitutionalProceduralContextVector {
        Objects.requireNonNull(profileCode);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(panel);
        Objects.requireNonNull(processProfile);
        Objects.requireNonNull(trustFloor);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
