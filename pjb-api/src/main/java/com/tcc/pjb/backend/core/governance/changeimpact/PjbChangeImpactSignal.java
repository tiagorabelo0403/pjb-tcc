package com.tcc.pjb.backend.core.governance.changeimpact;

import java.util.Objects;

public record PjbChangeImpactSignal(
        PjbChangeSurface surface,
        PjbChangeImpactSeverity severity,
        String affectedPath,
        String reason,
        String testAnchor
) {
    public PjbChangeImpactSignal {
        surface = surface == null ? PjbChangeSurface.TEST_GOVERNANCE : surface;
        severity = severity == null ? PjbChangeImpactSeverity.MEDIUM : severity;
        affectedPath = Objects.toString(affectedPath, "").trim();
        reason = Objects.toString(reason, "").trim();
        testAnchor = Objects.toString(testAnchor, "").trim();
    }

    public boolean blocking() {
        return severity == PjbChangeImpactSeverity.CRITICAL;
    }
}
