package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LegalPrazoRuleEngine implements LegalDeterministicRuleEngine {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b");
    private static final Pattern DURATION_PATTERN = Pattern.compile("\\b\\d+\\s*(dia|dias|mes|meses|ano|anos|hora|horas)\\b");

    @Override
    public String engineCode() {
        return LegalSymbolicValidationCatalog.ENGINE_PRAZO;
    }

    @Override
    public LegalSymbolicValidationOutcome evaluate(LegalSymbolicValidationContext context) {
        String text = context == null ? "" : context.normalizedText();
        boolean relevant = context != null && (context.objectiveContains("prazo", "tempest", "prescri", "decad")
                || context.textContains("prazo", "tempest", "intempest", "prescri", "decad"));
        boolean hasTemporalMarker = DATE_PATTERN.matcher(text).find()
                || DURATION_PATTERN.matcher(text).find()
                || context.textContains("publicacao", "publicação", "intimacao", "intimação", "ciencia", "ciência", "juntada", "termo inicial", "contagem");
        boolean assertsPrazoConclusion = context != null && context.textContains("tempestiv", "intempestiv", "prescrito", "decadente");
        List<LegalSymbolicValidationIssue> issues = new ArrayList<>();
        if (relevant && !hasTemporalMarker) {
            issues.add(LegalSymbolicValidationIssue.missingEvidence(
                    engineCode(),
                    "Análise de prazo sem marco temporal, termo inicial ou unidade de contagem identificável.",
                    Map.of("objetivo", context == null ? "" : context.normalizedObjetivo())
            ));
        }
        if (assertsPrazoConclusion && !hasTemporalMarker) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Conclusão de tempestividade ou decadência sem base temporal mínima confirmável.",
                    Map.of("texto", text)
            ));
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("relevant", relevant);
        diagnostics.put("temporalMarkerDetected", hasTemporalMarker);
        diagnostics.put("prazoConclusionDetected", assertsPrazoConclusion);
        return LegalSymbolicValidationOutcome.of(engineCode(), issues, diagnostics);
    }
}
