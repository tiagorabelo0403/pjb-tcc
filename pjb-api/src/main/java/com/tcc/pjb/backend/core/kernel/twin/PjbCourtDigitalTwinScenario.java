package com.tcc.pjb.backend.core.kernel.twin;

import java.util.List;
import java.util.Objects;

public record PjbCourtDigitalTwinScenario(
        String tribunalCode,
        int currentBacklog,
        int weeklyIncomingCases,
        int availableTeams,
        List<String> activatedCapabilities
) {
    public PjbCourtDigitalTwinScenario {
        tribunalCode = Objects.toString(tribunalCode, "").trim().toUpperCase();
        currentBacklog = Math.max(0, currentBacklog);
        weeklyIncomingCases = Math.max(0, weeklyIncomingCases);
        availableTeams = Math.max(0, availableTeams);
        activatedCapabilities = activatedCapabilities == null ? List.of() : List.copyOf(activatedCapabilities);
    }
}
