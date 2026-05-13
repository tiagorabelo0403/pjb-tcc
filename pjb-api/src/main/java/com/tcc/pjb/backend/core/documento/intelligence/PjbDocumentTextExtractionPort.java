package com.tcc.pjb.backend.core.documento.intelligence;

import java.util.List;

public interface PjbDocumentTextExtractionPort {

    record ExtractionRequest(String documentId, byte[] content, String mimeType) {}

    record ExtractionResult(String documentId, String extractedText, List<String> signals, double confidence) {
        public List<String> safeSignals() {
            return signals == null ? List.of() : List.copyOf(signals);
        }
    }

    ExtractionResult extract(ExtractionRequest request);
}
