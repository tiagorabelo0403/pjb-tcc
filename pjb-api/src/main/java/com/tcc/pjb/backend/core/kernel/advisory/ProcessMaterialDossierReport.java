package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record ProcessMaterialDossierReport(
        String lane,
        String phase,
        String objectLabel,
        String primaryRelief,
        String evidentiaryBracket,
        String negotiationBracket,
        List<String> controversyAxes,
        List<String> thesisVectors,
        List<String> evidenceAnchors,
        List<String> proofGaps,
        List<String> petitionSections,
        List<String> settlementLevers,
        List<String> protocolChecklist,
        Map<String, Object> diagnostics
) {
}
