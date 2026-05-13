package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.util.List;

public record RecursalMeshSearchResponse(
        String source,
        int totalReturned,
        List<RecursalMeshProcessLinkView> items) {

    public RecursalMeshSearchResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
