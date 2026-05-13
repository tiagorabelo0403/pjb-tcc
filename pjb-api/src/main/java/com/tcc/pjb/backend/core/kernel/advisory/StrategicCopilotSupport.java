package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class StrategicCopilotSupport {

    StrategicCopilotDraft petitionAssistDraft() {
        return new StrategicCopilotDraft(0.58d);
    }

    StrategicCopilotDraft processTwinDraft() {
        return new StrategicCopilotDraft(0.61d);
    }

    StrategicCopilotReport.Action action(String code,
                                         String title,
                                         String severity,
                                         String rationale,
                                         List<String> steps) {
        return new StrategicCopilotReport.Action(
                code,
                title,
                severity,
                rationale,
                steps == null ? List.of() : List.copyOf(new LinkedHashSet<>(steps))
        );
    }

    boolean hasCritical(ProtocolDryRunReport dryRun) {
        return dryRun != null && dryRun.checks().stream().anyMatch(check -> !check.passed() && "CRITICAL".equals(check.severity()));
    }

    String normalizePhaseLabel(FaseProcessual fase) {
        return (fase == null ? "PETITION_PRE_PROTOCOL" : fase.name()).toUpperCase(Locale.ROOT);
    }

    boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    boolean blank(String value) {
        return value == null || value.isBlank();
    }

    String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
