package com.tcc.pjb.backend.model.dto.leitura;

import com.tcc.pjb.backend.model.dto.shared.reading.ProcessReadingFlowMetadataDto;
import java.util.List;

public record ProcessReadingFlowResponse(
        long totalEntries,
        long totalInlineActs,
        long totalMovements,
        long totalEvents,
        String chronologyMode,
        String defaultOpenMode,
        List<ProcessReadingProcessEntryResponse> entries,
        ProcessReadingFlowMetadataDto metadata
) {
    public ProcessReadingFlowResponse {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
