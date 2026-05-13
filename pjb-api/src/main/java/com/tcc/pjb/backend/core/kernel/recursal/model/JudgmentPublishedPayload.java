package com.tcc.pjb.backend.core.kernel.recursal.model;

import java.time.LocalDate;
import java.util.Objects;

public record JudgmentPublishedPayload(
        String court,
        String panel,
        LocalDate decisionDate,
        String resultSummary,
        String ementa
) implements CanonicalFactPayload {

    public JudgmentPublishedPayload {
        court = Objects.toString(court, "").trim();
        panel = Objects.toString(panel, "").trim();
        resultSummary = Objects.toString(resultSummary, "").trim();
        ementa = Objects.toString(ementa, "").trim();
    }
}
