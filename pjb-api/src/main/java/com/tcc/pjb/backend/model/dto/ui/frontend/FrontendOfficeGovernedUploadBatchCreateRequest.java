package com.tcc.pjb.backend.model.dto.ui.frontend;

import jakarta.validation.constraints.Min;

public record FrontendOfficeGovernedUploadBatchCreateRequest(
        @Min(1) Integer expectedCount
) {
}
