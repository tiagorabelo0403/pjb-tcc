package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProcessReadingContentBlockResponse(
        String blockId,
        String sourceType,
        String blockType,
        String title,
        String body,
        Integer pageNumber,
        String anchor,
        String importance,
        List<String> tags,
        @Schema(description = "Metadados tecnicos do bloco de conteudo — chaves variam por blockType")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingContentBlockResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

