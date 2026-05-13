package com.tcc.pjb.backend.model.dto.admin.governance;

import java.util.Map;

public record AdminProceduralSnapshotResponse(
        String scope,
        Map<String, Object> payload
) {
}
