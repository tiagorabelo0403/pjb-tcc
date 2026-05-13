package com.tcc.pjb.backend.model.dto.admin;

import java.util.List;
import java.util.UUID;

public record RitoDraftBulkProposeResponse(
        int requested,
        int createdOrReused,
        List<UUID> proposalIds
) {
}
