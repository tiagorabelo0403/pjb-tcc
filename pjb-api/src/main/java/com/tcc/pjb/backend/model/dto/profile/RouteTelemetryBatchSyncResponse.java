package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record RouteTelemetryBatchSyncResponse(
        String actor,
        String canal,
        int recebidas,
        int persistidas,
        int reaproveitadas,
        Instant ultimaCaptura,
        List<RouteTelemetrySnapshotResponse> amostras
) {
}
