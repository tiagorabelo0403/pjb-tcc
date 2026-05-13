package com.tcc.pjb.backend.model.dto.admin;

import java.util.List;




public record RitoSuggestionDto(
        String ritoResolved,
        String ritoChosen,
        long count,
        List<String> sampleReasons
) {
    public long occurrences() { return count(); }
}

