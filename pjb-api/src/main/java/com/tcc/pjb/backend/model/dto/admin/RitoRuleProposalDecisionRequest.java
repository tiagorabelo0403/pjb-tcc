package com.tcc.pjb.backend.model.dto.admin;


import jakarta.validation.constraints.NotBlank;


public record RitoRuleProposalDecisionRequest(
        @NotBlank(message = "Motivo é obrigatório")
        String reason
) {
}
