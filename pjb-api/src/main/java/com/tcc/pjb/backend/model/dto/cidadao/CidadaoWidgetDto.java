package com.tcc.pjb.backend.model.dto.cidadao;

import com.fasterxml.jackson.databind.JsonNode;

public record CidadaoWidgetDto(
        String key,
        String type,
        JsonNode data
) {
}
