package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;

public record RouteTelemetrySnapshotResponse(
        String actor,
        String canal,
        double latitude,
        double longitude,
        Double precisaoMetros,
        Double velocidadeKmh,
        Integer bateriaPercentual,
        String fonte,
        boolean foreground,
        String deviceHashPrefix,
        boolean reaproveitada,
        Instant capturadoEm,
        Instant persistidoEm
) {
}
