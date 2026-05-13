package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class ProcessMaterialDossierDiagnosticsFactory {

    private final ProcessMaterialDossierTextSupport textSupport;

    ProcessMaterialDossierDiagnosticsFactory(ProcessMaterialDossierTextSupport textSupport) {
        this.textSupport = Objects.requireNonNull(textSupport);
    }

    Map<String, Object> create(ProcessMaterialDossierInput input,
                               ProcessMaterialDossierAnalysis analysis) {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("evidenceScore", input.evidenceScore());
        diagnostics.put("negotiationScore", input.negotiationScore());
        diagnostics.put("evidenceDensity", analysis.evidenceDensity());
        diagnostics.put("pedidoDensity", analysis.pedidoDensity());
        diagnostics.put("controversyDensity", analysis.controversyDensity());
        diagnostics.put("claimsCount", input.claims().size());
        diagnostics.put("evidenceCount", input.evidenceItems().size());
        diagnostics.put("ramoDireito", input.ramoDireito());
        diagnostics.put("rito", input.ritoName());
        diagnostics.put("valorCausa", input.valorCausa() != null ? input.valorCausa().setScale(2, RoundingMode.HALF_UP) : null);
        diagnostics.put("authorIdPresent", input.authorIdPresent());
        diagnostics.put("counterpartyIdPresent", input.counterpartyIdPresent());
        diagnostics.put("dossierReadinessScore", analysis.dossierReadinessScore());
        diagnostics.put("attentionBand", analysis.attentionBand());
        diagnostics.put("executiveSummary", analysis.executiveSummary());
        diagnostics.put("strategicFocus", analysis.strategicFocus());
        diagnostics.put("evidentiaryBracket", analysis.evidentiaryBracket());
        diagnostics.put("negotiationBracket", analysis.negotiationBracket());
        diagnostics.put("riskSignalsCount", input.riskSignals().size());
        diagnostics.put("claimFocus", textSupport.truncate(textSupport.firstItem(input.claims()), 180));
        diagnostics.put("evidenceFocus", textSupport.truncate(textSupport.firstItem(input.evidenceItems()), 180));
        diagnostics.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return java.util.Collections.unmodifiableMap(diagnostics);
    }
}
