package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RouteTelemetryUpsertRequest(
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude,
        @DecimalMin(value = "0.0") Double precisaoMetros,
        @DecimalMin(value = "0.0") Double velocidadeKmh,
        @Min(0) @Max(100) Integer bateriaPercentual,
        String fonte,
        Instant capturadoEm,
        Boolean foreground
) {
}
