package com.tcc.pjb.backend.core.documento.intelligence;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PjbLegalAiTriageAdapter implements PjbTriageSuggestionPort {

    private static final Set<String> URGENCY_SIGNALS = Set.of(
            "TUTELA", "URGENCIA", "LIMINAR", "MEDIDA", "UTI", "SAUDE", "MEDICAMENTO",
            "CRIANCA", "IDOSO", "VIOLENCIA", "ALIMENTO", "PRISAO", "FLAGRANTE"
    );

    @Override
    public TriageSuggestionResult suggest(TriageSuggestionRequest request) {
        if (request == null) {
            return new TriageSuggestionResult(Optional.empty(), 0, "Requisição vazia", 0.0, List.of());
        }

        Optional<RitoProcessual> suggestedRito = RitoProcessual.tryParse(request.classeProcessual())
                .or(() -> RitoProcessual.tryParse(request.assuntoPrincipal()));

        List<String> allSignals = new ArrayList<>();
        allSignals.addAll(request.safeDocumentSignals());
        allSignals.addAll(request.safePetitionSignals());

        int urgencyScore = computeUrgencyScore(allSignals, suggestedRito.orElse(null));
        double confidence = suggestedRito.isPresent() ? 0.85 : 0.40;
        String explanation = buildExplanation(suggestedRito, urgencyScore);
        List<String> corrections = buildCorrections(request, suggestedRito);

        return new TriageSuggestionResult(suggestedRito, urgencyScore, explanation, confidence, corrections);
    }

    private int computeUrgencyScore(List<String> signals, RitoProcessual rito) {
        int score = 20;
        for (String signal : signals) {
            String normalized = normalize(signal);
            for (String term : URGENCY_SIGNALS) {
                if (normalized.contains(term)) {
                    score += 12;
                    break;
                }
            }
        }
        if (rito != null && rito.isPenal()) score += 15;
        return Math.min(99, score);
    }

    private String buildExplanation(Optional<RitoProcessual> rito, int urgencyScore) {
        String ritoLabel = rito.map(r -> r.getGrupoPrincipal().name()).orElse("NAO_IDENTIFICADO");
        return "Grupo=" + ritoLabel + " urgencyScore=" + urgencyScore;
    }

    private List<String> buildCorrections(TriageSuggestionRequest request, Optional<RitoProcessual> suggestedRito) {
        List<String> corrections = new ArrayList<>();
        if (suggestedRito.isEmpty()) {
            corrections.add("Revisar classe processual e assunto principal para identificar o rito");
        }
        if (request.safeDocumentSignals().isEmpty() && request.safePetitionSignals().isEmpty()) {
            corrections.add("Incluir sinais extraídos dos documentos para melhorar a precisão da triagem");
        }
        return List.copyOf(corrections);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.strip().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Â', 'A').replace('Ã', 'A')
                .replace('É', 'E').replace('Ê', 'E').replace('Í', 'I')
                .replace('Ó', 'O').replace('Ô', 'O').replace('Õ', 'O')
                .replace('Ú', 'U').replace('Ç', 'C');
    }
}
