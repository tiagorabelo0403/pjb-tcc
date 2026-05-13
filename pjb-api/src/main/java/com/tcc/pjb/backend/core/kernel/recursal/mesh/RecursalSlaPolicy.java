package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;

public record RecursalSlaPolicy(
        RecursalLifecycleState estado,
        RecursalTribunal tribunal,
        int diasUteis,
        boolean fatalParaPartes,
        String fundamentoLegal) {

    public RecursalSlaPolicy {
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(tribunal, "tribunal");
        if (diasUteis <= 0) {
            throw new IllegalArgumentException("diasUteis deve ser positivo");
        }
        fundamentoLegal = fundamentoLegal == null || fundamentoLegal.isBlank()
                ? "Regimento interno e governança operacional recursal"
                : fundamentoLegal.trim();
    }
}
