package com.tcc.pjb.backend.model.dto.julgamento.safety;

import jakarta.validation.constraints.NotBlank;

public record DecisionFocusHeartbeatRequest(
        @NotBlank String windowBinding,
        @NotBlank String tabBinding,
        String routeBinding
) {
}
