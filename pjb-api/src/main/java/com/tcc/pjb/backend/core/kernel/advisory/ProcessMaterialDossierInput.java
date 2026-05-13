package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.util.List;

record ProcessMaterialDossierInput(
        String lane,
        String phase,
        String objectLabel,
        String primaryRelief,
        String narrative,
        String evidenceText,
        List<String> claims,
        List<String> evidenceItems,
        Integer evidenceScore,
        Integer negotiationScore,
        BigDecimal valorCausa,
        String ramoDireito,
        String ritoName,
        boolean authorIdPresent,
        boolean counterpartyIdPresent,
        List<String> riskSignals
) {
}
