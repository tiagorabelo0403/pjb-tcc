package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record EmbargosDeclaracaoBlueprint(
        int prazoDiasUteis,
        boolean cabivelContraQualquerDecisao,
        boolean interrompePrazoRecursalPrincipal,
        Set<String> fundamentosCabiveis,
        boolean efeitoInfringenteExigeFundamentoApto) {

    public EmbargosDeclaracaoBlueprint {
        if (prazoDiasUteis <= 0) {
            throw new IllegalArgumentException("prazoDiasUteis deve ser positivo");
        }
        Objects.requireNonNull(fundamentosCabiveis, "fundamentosCabiveis");
        fundamentosCabiveis = Set.copyOf(new LinkedHashSet<>(fundamentosCabiveis));
        if (fundamentosCabiveis.isEmpty()) {
            throw new IllegalArgumentException("fundamentosCabiveis é obrigatório");
        }
    }
}
