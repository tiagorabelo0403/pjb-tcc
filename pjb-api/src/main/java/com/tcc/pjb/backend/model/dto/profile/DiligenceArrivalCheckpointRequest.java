package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DiligenceArrivalCheckpointRequest(
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double destinoLatitude,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double destinoLongitude,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double observadaLatitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double observadaLongitude,
        @DecimalMin(value = "10.0") Double raioMetros,
        Instant capturadoEm,
        String fonte
) {
}
