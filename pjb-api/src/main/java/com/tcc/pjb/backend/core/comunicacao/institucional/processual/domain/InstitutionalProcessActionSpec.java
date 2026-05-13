package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProcessActionSpec(
        String code,
        String title,
        String description,
        String accentColor,
        boolean requiresCertificate,
        boolean requiresTitularApproval,
        boolean modifiesFlow,
        List<String> fasesPreferenciais,
        List<String> ritosPreferenciais,
        List<String> fundamentos
) {
    public InstitutionalProcessActionSpec {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        Objects.requireNonNull(accentColor);
        fasesPreferenciais = fasesPreferenciais == null ? List.of() : List.copyOf(fasesPreferenciais);
        ritosPreferenciais = ritosPreferenciais == null ? List.of() : List.copyOf(ritosPreferenciais);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
