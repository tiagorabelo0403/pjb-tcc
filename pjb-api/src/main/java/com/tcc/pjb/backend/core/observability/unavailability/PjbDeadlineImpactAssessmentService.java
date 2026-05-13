package com.tcc.pjb.backend.core.observability.unavailability;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class PjbDeadlineImpactAssessmentService {

    private static final Duration MINIMUM_EXTERNAL_OUTAGE = Duration.ofMinutes(60);

    public PjbDeadlineImpactResult assess(PjbSystemUnavailabilityEvent event,
                                          LocalDate dueDate,
                                          ZoneId zoneId) {
        if (event == null) {
            return new PjbDeadlineImpactResult(false, dueDate, Duration.ZERO, List.of("missing unavailability event"));
        }
        ZoneId zone = zoneId == null ? ZoneId.of("America/Fortaleza") : zoneId;
        List<String> reasons = new ArrayList<>();
        boolean extendsDeadline = false;
        if (!event.affectsExternalUser()) {
            reasons.add("no external critical service affected");
        }
        if (event.planned()) {
            reasons.add("planned outage requires explicit operational decision");
        }
        if (event.duration().compareTo(MINIMUM_EXTERNAL_OUTAGE) >= 0 && event.affectsExternalUser() && !event.planned()) {
            extendsDeadline = true;
            reasons.add("external critical service outage reached policy threshold");
        }
        boolean lateWindow = event.endedAt().atZone(zone).toLocalTime().getHour() == 23
                || event.startedAt().atZone(zone).toLocalTime().getHour() == 23;
        if (lateWindow && event.affectsExternalUser() && !event.planned()) {
            extendsDeadline = true;
            reasons.add("external critical service outage affected final filing hour");
        }
        LocalDate nextBusinessDay = dueDate == null ? null : nextBusinessDay(dueDate);
        return new PjbDeadlineImpactResult(extendsDeadline, nextBusinessDay, event.duration(), reasons);
    }

    private LocalDate nextBusinessDay(LocalDate date) {
        LocalDate candidate = date.plusDays(1);
        while (candidate.getDayOfWeek().getValue() >= 6) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
