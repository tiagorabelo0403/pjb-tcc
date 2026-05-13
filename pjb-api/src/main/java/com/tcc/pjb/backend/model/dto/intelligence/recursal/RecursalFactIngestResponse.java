package com.tcc.pjb.backend.model.dto.intelligence.recursal;

import java.util.UUID;

public record RecursalFactIngestResponse(
        String systemTag,
        UUID factId,
        String dedupKey,
        Long processoId,
        Long timelineMovementId,
        RecursalPlanDto plan,
        RecursalGraphResponse graph
) {}
