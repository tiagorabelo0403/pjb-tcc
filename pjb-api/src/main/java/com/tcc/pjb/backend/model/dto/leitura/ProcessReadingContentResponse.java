package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProcessReadingContentResponse(
        String readerId,
        String readerType,
        String title,
        ProcessReadingSurfaceResponse surface,
        ProcessReadingPreferenceResponse preference,
        String chronologyMode,
        String defaultFocusMode,
        boolean copyEnabled,
        boolean downloadable,
        boolean searchable,
        List<ProcessReadingContentBlockResponse> blocks,
        @Schema(description = "Metadados tecnicos da resposta de conteudo (processoId, flowEntries, documentCount, pageCount, defaultTab)")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingContentResponse {
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

