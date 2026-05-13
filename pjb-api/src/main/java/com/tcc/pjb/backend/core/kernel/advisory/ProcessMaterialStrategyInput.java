package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.util.List;

record ProcessMaterialStrategyInput(
        String lane,
        String objectLabel,
        String primaryRelief,
        String ramoDireito,
        String ritoName,
        BigDecimal valorCausa,
        Integer evidenceScore,
        Integer negotiationScore,
        String authorId,
        String counterpartyId,
        boolean urgent,
        boolean juizado,
        ProcessMaterialDossierReport dossier,
        List<String> externalSignals,
        Double readinessScore,
        String evidenceText,
        String narrativeText
) {
}
