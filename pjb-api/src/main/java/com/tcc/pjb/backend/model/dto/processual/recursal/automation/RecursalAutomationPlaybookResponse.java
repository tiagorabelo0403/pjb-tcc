package com.tcc.pjb.backend.model.dto.processual.recursal.automation;

import java.util.List;
import java.util.Set;

public record RecursalAutomationPlaybookResponse(
        String rotaPrioritaria,
        String rotaAlternativa,
        int prazoBaseDiasUteis,
        String competenciaAdmissibilidade,
        Set<String> secoesEssenciais,
        List<String> alertasCriticos,
        List<RecursalAutomationPlaybookStepView> passos) {
}
