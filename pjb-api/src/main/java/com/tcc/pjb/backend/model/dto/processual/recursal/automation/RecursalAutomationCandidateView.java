package com.tcc.pjb.backend.model.dto.processual.recursal.automation;

import java.util.Set;

public record RecursalAutomationCandidateView(
        String recurso,
        int prioridade,
        String fundamentoBase,
        int prazoDiasUteis,
        String juizoAdmissibilidadeCompetencia,
        String meritoErroTipoSugerido,
        Set<String> secoesObrigatorias,
        boolean subordinadoAoPrincipal) {
}
