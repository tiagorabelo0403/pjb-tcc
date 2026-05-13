package com.tcc.pjb.backend.model.dto.infra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ScaleArchitecturePartitionPlanRequest(
        @NotBlank String tableName,
        @NotBlank String partitionColumn,
        @NotBlank String partitionPrefix,
        @Min(2020) int startYear,
        @Min(1) int yearsAhead,
        String notes
) {
}
