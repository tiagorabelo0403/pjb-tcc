package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;

record ProcessMaterialDossierAnalysis(
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
        int evidenceDensity,
        int pedidoDensity,
        int controversyDensity,
        int dossierReadinessScore,
        String attentionBand,
        String executiveSummary,
        String strategicFocus
) {
}
