package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.LocalDateTime;

public record NegotiationRoundResponse(
        Integer round,
        String version,
        String eventType,
        String summary,
        LocalDateTime registeredAt,
        String registeredBy
) {
}
