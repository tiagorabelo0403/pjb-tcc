package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RecursalTransitionResult(
        RecursalStateSnapshot previous,
        RecursalStateSnapshot current,
        RecursalSpecies species,
        RecursalRoutePlan routePlan,
        Set<RecursalTransitionEvent> nextEvents) {

    public RecursalTransitionResult {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(species, "species");
        Objects.requireNonNull(routePlan, "routePlan");
        nextEvents = Set.copyOf(new LinkedHashSet<>(nextEvents));
    }
}
