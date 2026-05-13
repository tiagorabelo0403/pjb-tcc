package com.tcc.pjb.backend.model.dto.surface.common;

import java.util.List;

public record SurfaceListItemResponse(
        String key,
        List<SurfaceFieldResponse> fields
) {
}
