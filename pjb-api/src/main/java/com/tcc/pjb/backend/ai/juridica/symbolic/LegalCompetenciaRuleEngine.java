package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalCompetenciaRuleEngine implements LegalDeterministicRuleEngine {

    @Override
    public String engineCode() {
        return LegalSymbolicValidationCatalog.ENGINE_COMPETENCIA;
    }

    @Override
    public LegalSymbolicValidationOutcome evaluate(LegalSymbolicValidationContext context) {
        boolean relevant = context != null && (context.objectiveContains("competencia", "competência", "foro", "juizo", "juízo")
                || context.textContains("competencia", "competência", "foro", "juizo", "juízo", "vara", "tribunal", "turma", "comarca"));
        boolean hasAuthorityMarker = context != null && context.textContains(
                "vara", "tribunal", "turma", "comarca", "foro", "juizado", "juizado especial", "justica federal", "justiça federal",
                "justica do trabalho", "justiça do trabalho", "tribunal do juri", "tribunal do júri", "seção judiciária", "secao judiciaria"
        );
        List<LegalSymbolicValidationIssue> issues = new ArrayList<>();
        if (relevant && !hasAuthorityMarker) {
            issues.add(LegalSymbolicValidationIssue.missingEvidence(
                    engineCode(),
                    "Discussão de competência sem indicação mínima de órgão julgador, vara, foro ou tribunal.",
                    Map.of("ramo", context == null ? "" : context.normalizedRamo())
            ));
        }
        if (context != null && context.ramoContains("trabalh") && context.textContains("vara federal", "justica federal", "justiça federal")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Contexto trabalhista com indicação de órgão típico da Justiça Federal comum.",
                    Map.of("ramo", context.normalizedRamo())
            ));
        }
        if (context != null && context.ramoContains("penal") && context.textContains("vara do trabalho", "justica do trabalho", "justiça do trabalho")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Contexto penal com indicação de competência típica da Justiça do Trabalho.",
                    Map.of("ramo", context.normalizedRamo())
            ));
        }
        if (context != null && context.ramoContains("civel", "cível") && context.textContains("tribunal do juri", "tribunal do júri")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Contexto cível com remissão a órgão tipicamente penal do júri.",
                    Map.of("ramo", context.normalizedRamo())
            ));
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("relevant", relevant);
        diagnostics.put("authorityMarkerDetected", hasAuthorityMarker);
        diagnostics.put("ramo", context == null ? "" : context.normalizedRamo());
        return LegalSymbolicValidationOutcome.of(engineCode(), issues, diagnostics);
    }
}
