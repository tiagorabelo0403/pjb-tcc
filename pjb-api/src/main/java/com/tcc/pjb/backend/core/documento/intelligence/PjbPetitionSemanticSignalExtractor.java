package com.tcc.pjb.backend.core.documento.intelligence;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Optional;

public interface PjbPetitionSemanticSignalExtractor {

    record SignalExtractionRequest(String petitionText, String classeProcessual, String assuntoPrincipal) {}

    record SignalExtractionResult(
            List<String> extractedSignals,
            Optional<RitoProcessual> suggestedRito,
            boolean urgencyDetected,
            boolean vulnerablePartyDetected,
            double confidence
    ) {
        public List<String> safeSignals() {
            return extractedSignals == null ? List.of() : List.copyOf(extractedSignals);
        }
    }

    SignalExtractionResult extract(SignalExtractionRequest request);
}
