package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingProceduralContextResponse(
        String justiceTrack,
        String tribunalTier,
        String ramo,
        String materia,
        String rito,
        String ritoFamily,
        String fase,
        String instanciaLeitura,
        String orgaoLeitura,
        String recursalTrack,
        String embargoTrack,
        String nativeActTrack,
        String signatureTrack,
        boolean htmlInlinePreferred,
        boolean pdfSignedPreferred,
        List<String> markers,
        Map<String, Object> metadata
) {
    public ProcessReadingProceduralContextResponse {
        markers = markers == null ? List.of() : List.copyOf(markers);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
