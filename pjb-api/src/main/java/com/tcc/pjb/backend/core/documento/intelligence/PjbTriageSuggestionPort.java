package com.tcc.pjb.backend.core.documento.intelligence;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Optional;

public interface PjbTriageSuggestionPort {

    record TriageSuggestionRequest(
            String classeProcessual,
            String assuntoPrincipal,
            List<String> documentSignals,
            List<String> petitionSignals
    ) {
        public List<String> safeDocumentSignals() {
            return documentSignals == null ? List.of() : List.copyOf(documentSignals);
        }
        public List<String> safePetitionSignals() {
            return petitionSignals == null ? List.of() : List.copyOf(petitionSignals);
        }
    }

    record TriageSuggestionResult(
            Optional<RitoProcessual> suggestedRito,
            int urgencyScore,
            String explanation,
            double confidence,
            List<String> correctionSuggestions
    ) {
        public List<String> safeCorrections() {
            return correctionSuggestions == null ? List.of() : List.copyOf(correctionSuggestions);
        }
    }

    TriageSuggestionResult suggest(TriageSuggestionRequest request);
}
