package com.tcc.pjb.backend.model.dto.workitem;

import jakarta.validation.constraints.NotNull;

public record WorkItemGenerationRequest(
        @NotNull Long processoId,
        
        boolean force,
        
        String fase
) {
}
