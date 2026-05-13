package com.tcc.pjb.backend.model.dto.processual.recursal.foundation;

import java.util.List;
import java.util.Set;

public record RecursalApelacaoBlueprintView(
        int prazoDiasUteis,
        boolean cabivelContraSentenca,
        boolean juizAquonaoFazJuizoAdmissibilidade,
        boolean admitePreliminarContraInterlocutoriaNaoAgravavel,
        List<String> pecasObrigatorias,
        Set<String> pressupostosGenericos) {
}
