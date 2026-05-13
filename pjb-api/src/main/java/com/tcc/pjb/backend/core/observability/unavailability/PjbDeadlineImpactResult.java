package com.tcc.pjb.backend.core.observability.unavailability;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public record PjbDeadlineImpactResult(
        boolean extendsDeadline,
        LocalDate nextBusinessDayCandidate,
        Duration creditedOutage,
        List<String> reasons
) {
    public PjbDeadlineImpactResult {
        creditedOutage = creditedOutage == null ? Duration.ZERO : creditedOutage;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
