package com.tcc.pjb.backend.model.dto.admin;

import java.time.OffsetDateTime;
import java.util.List;







public record RitoRuleDraftResponse(
        OffsetDateTime generatedAt,
        int windowDays,
        double threshold,
        int top,
        List<RitoRuleDraftItemDto> items
) {
}
