package com.tcc.pjb.backend.model.dto.julgamento.safety;

public record DecisionFocusOpenRequest(
        String windowBinding,
        String tabBinding,
        String routeBinding
) {
}
