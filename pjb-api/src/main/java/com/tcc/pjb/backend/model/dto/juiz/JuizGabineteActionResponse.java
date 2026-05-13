package com.tcc.pjb.backend.model.dto.juiz;

import java.util.Map;

public record JuizGabineteActionResponse(
        String action,
        Map<String, Object> payload
) {
}
