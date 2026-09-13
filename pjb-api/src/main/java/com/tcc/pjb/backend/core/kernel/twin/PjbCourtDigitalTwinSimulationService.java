package com.tcc.pjb.backend.core.kernel.twin;

import java.util.LinkedHashSet;
import java.util.List;

public final class PjbCourtDigitalTwinSimulationService {

    public PjbCourtDigitalTwinProjection simulate(PjbCourtDigitalTwinScenario scenario) {
        if (scenario == null) {
            return new PjbCourtDigitalTwinProjection("INSUFFICIENT_DATA", 0, 0, true, List.of("informar cenário operacional do tribunal"));
        }
        int baseCapacity = scenario.availableTeams() * 45;
        int capabilityBoost = scenario.activatedCapabilities().size() * 8;
        int weeklyCapacity = baseCapacity + capabilityBoost;
        int projectedBacklog = Math.max(0, scenario.currentBacklog() + scenario.weeklyIncomingCases() * 4 - weeklyCapacity * 4);
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();
        if (weeklyCapacity < scenario.weeklyIncomingCases()) {
            recommendations.add("ativar núcleo digital, redistribuição assistida ou reforço temporário de equipes");
        }
        if (!scenario.activatedCapabilities().contains("PUBLIC_PORTAL")) {
            recommendations.add("ativar portal público para reduzir demanda manual de balcão");
        }
        if (!scenario.activatedCapabilities().contains("SECRETARIAT_AUTOPILOT")) {
            recommendations.add("ativar priorização operacional de secretaria para filas críticas");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("manter simulação semanal com telemetria de filas e entrada nova");
        }
        boolean intervention = projectedBacklog > scenario.currentBacklog() || weeklyCapacity < scenario.weeklyIncomingCases();
        String status = intervention ? "INTERVENTION_REQUIRED" : projectedBacklog == 0 ? "CLEARING_BACKLOG" : "STABLE";
        return new PjbCourtDigitalTwinProjection(status, weeklyCapacity, projectedBacklog, intervention, List.copyOf(recommendations));
    }
}
