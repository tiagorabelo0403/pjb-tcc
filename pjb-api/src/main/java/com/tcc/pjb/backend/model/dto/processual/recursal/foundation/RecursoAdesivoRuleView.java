package com.tcc.pjb.backend.model.dto.processual.recursal.foundation;

import java.util.Set;

public record RecursoAdesivoRuleView(
        Set<String> recursosCabiveis,
        boolean exigeSucumbenciaReciproca,
        boolean prazoSegueContrarrazoes,
        boolean subordinadoAoPrincipal,
        boolean extingueSePrincipalDesistidoOuInadmitido) {
}
