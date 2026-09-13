package com.tcc.pjb.backend.core.plataforma.substituicao.parity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PjbLegacyParityTestKit {

    public PjbLegacyParityReport evaluate(List<PjbLegacyParityFinding> findings) {
        List<PjbLegacyParityFinding> safe = findings == null ? List.of() : List.copyOf(findings);
        EnumMap<PjbLegacyParityCapability, Boolean> coverage = new EnumMap<>(PjbLegacyParityCapability.class);
        List<PjbLegacyParityFinding> missing = new ArrayList<>();
        for (PjbLegacyParityCapability capability : PjbLegacyParityCapability.values()) {
            boolean covered = safe.stream().anyMatch(finding -> finding != null && finding.capability() == capability && finding.covered());
            coverage.put(capability, covered);
        }
        for (PjbLegacyParityFinding finding : safe) {
            if (finding != null && !finding.covered()) {
                missing.add(finding);
            }
        }
        String status = coverage.containsValue(false) || !missing.isEmpty() ? "PARITY_GAPS_REMAIN" : "PARITY_READY";
        return new PjbLegacyParityReport(status, Map.copyOf(coverage), List.copyOf(missing));
    }
}
