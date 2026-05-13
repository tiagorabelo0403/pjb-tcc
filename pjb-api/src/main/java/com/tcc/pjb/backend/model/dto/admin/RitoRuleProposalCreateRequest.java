package com.tcc.pjb.backend.model.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record RitoRuleProposalCreateRequest(
        @NotBlank String ritoResolved,
        @NotBlank String ritoChosen,
        Integer occurrences,
        String sampleReasonsJson,
        String notes
) {
}
