package com.tcc.pjb.backend.model.dto.profile;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record RouteTelemetryBatchSyncRequest(
        @NotEmpty List<@Valid RouteTelemetryUpsertRequest> amostras
) {
}
