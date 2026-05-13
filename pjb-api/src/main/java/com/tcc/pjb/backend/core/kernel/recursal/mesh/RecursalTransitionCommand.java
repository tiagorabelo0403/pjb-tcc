package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.Instant;
import java.util.Objects;

public record RecursalTransitionCommand(
        RecursalStateSnapshot snapshot,
        RecursalCaseContext context,
        RecursalSpecies species,
        RecursalTransitionEvent event,
        String actor,
        Instant occurredAt,
        RecursalTransitionDetails details) {

    public RecursalTransitionCommand {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(species, "species");
        Objects.requireNonNull(event, "event");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        actor = actor == null ? "SYSTEM" : actor;
        details = details == null ? RecursalTransitionDetails.empty() : details;
    }

    public RecursalTransitionCommand(
            RecursalStateSnapshot snapshot,
            RecursalCaseContext context,
            RecursalSpecies species,
            RecursalTransitionEvent event,
            String actor,
            Instant occurredAt) {
        this(snapshot, context, species, event, actor, occurredAt, RecursalTransitionDetails.empty());
    }
}
