package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecursalMeshPlanRequest(
        @NotBlank @Size(max = 160) String recursoId,
        @Valid @NotNull RecursalMeshContextRequest context,
        @Valid @NotNull RecursalMeshSpeciesRequest species) {
}
