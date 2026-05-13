package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionDetails;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;

public record RecursalMeshTransitionRequest(
        @NotBlank @Size(max = 160) String recursoId,
        @Valid @NotNull RecursalMeshContextRequest context,
        @Valid @NotNull RecursalMeshSpeciesRequest species,
        @Valid RecursalStateSnapshot snapshot,
        @NotNull RecursalTransitionEvent event,
        @Size(max = 160) String actor,
        Instant occurredAt,
        @Valid RecursalTransitionDetails details,
        @Size(max = 160) String commandId,
        @PositiveOrZero Integer expectedRevision) {

    public RecursalMeshTransitionRequest(
            String recursoId,
            RecursalMeshContextRequest context,
            RecursalMeshSpeciesRequest species,
            RecursalStateSnapshot snapshot,
            RecursalTransitionEvent event,
            String actor,
            Instant occurredAt,
            String commandId,
            Integer expectedRevision) {
        this(recursoId, context, species, snapshot, event, actor, occurredAt, RecursalTransitionDetails.empty(), commandId, expectedRevision);
    }
}
