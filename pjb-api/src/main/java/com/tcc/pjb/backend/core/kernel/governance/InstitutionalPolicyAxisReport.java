package com.tcc.pjb.backend.core.kernel.governance;

import java.util.List;

public record InstitutionalPolicyAxisReport(
        String ramoDireito,
        String materia,
        String ritoProcessual,
        String classeProcessual,
        String faseProcessual,
        String tribunalCodigo,
        String selectionMode,
        List<String> matchedAxes,
        List<String> declaredAxes
) {

    public static InstitutionalPolicyAxisReport empty() {
        return new InstitutionalPolicyAxisReport(
                null,
                null,
                null,
                null,
                null,
                null,
                "UNSPECIFIED",
                List.of(),
                List.of()
        );
    }
}
