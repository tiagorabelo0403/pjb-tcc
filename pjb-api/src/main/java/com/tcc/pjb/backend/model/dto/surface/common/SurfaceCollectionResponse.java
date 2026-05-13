package com.tcc.pjb.backend.model.dto.surface.common;

import java.util.List;

public record SurfaceCollectionResponse(
        String scope,
        List<SurfaceListItemResponse> items
) {
}
