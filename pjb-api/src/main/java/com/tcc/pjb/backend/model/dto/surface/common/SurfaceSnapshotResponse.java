package com.tcc.pjb.backend.model.dto.surface.common;

import java.util.List;

public record SurfaceSnapshotResponse(
        String scope,
        List<SurfaceFieldResponse> fields
) {
}
