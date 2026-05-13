package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RecursoAdesivoRegra(
        Set<String> recursosCabiveis,
        boolean exigeSucumbenciaReciproca,
        boolean prazoSegueContrarrazoes,
        boolean subordinadoAoPrincipal,
        boolean extingueSePrincipalDesistidoOuInadmitido) {

    public RecursoAdesivoRegra {
        Objects.requireNonNull(recursosCabiveis, "recursosCabiveis");
        recursosCabiveis = Set.copyOf(new LinkedHashSet<>(recursosCabiveis));
        if (recursosCabiveis.isEmpty()) {
            throw new IllegalArgumentException("recursosCabiveis é obrigatório");
        }
    }
}
