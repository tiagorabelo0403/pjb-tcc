package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Set;

public record RecursalPlanningResult(
        RecursalSpecies species,
        RecursalCaseContext context,
        RecursalRoutePlan routePlan,
        RecursalStateSnapshot initialSnapshot,
        Set<RecursalTransitionEvent> initialEvents) {
}
