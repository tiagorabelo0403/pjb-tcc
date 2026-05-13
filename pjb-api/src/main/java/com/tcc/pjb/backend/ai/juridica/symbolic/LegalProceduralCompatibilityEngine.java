package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalProceduralCompatibilityEngine implements LegalDeterministicRuleEngine {

    @Override
    public String engineCode() {
        return LegalSymbolicValidationCatalog.ENGINE_PROCEDURAL_COMPATIBILITY;
    }

    @Override
    public LegalSymbolicValidationOutcome evaluate(LegalSymbolicValidationContext context) {
        List<LegalSymbolicValidationIssue> issues = new ArrayList<>();
        if (context != null && context.normalizedRamo().isBlank() && context.normalizedRito().isBlank() && context.normalizedClasse().isBlank()) {
            issues.add(LegalSymbolicValidationIssue.missingEvidence(
                    engineCode(),
                    "Contexto procedimental insuficiente: ramo, rito e classe não foram informados.",
                    Map.of()
            ));
        }
        if (context != null && context.ramoContains("penal") && context.textContains("cumprimento de sentença", "contestacao", "contestação", "réu revel", "reu revel")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Contexto penal com marcadores típicos de fase cognitiva cível ou execução civil.",
                    Map.of("ramo", context.normalizedRamo())
            ));
        }
        if (context != null && context.ramoContains("civel", "cível") && context.textContains("denuncia", "denúncia", "materialidade", "ação penal", "acao penal")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Contexto cível com marcadores típicos de ação penal.",
                    Map.of("ramo", context.normalizedRamo())
            ));
        }
        if (context != null && context.ritoContains("juizado") && context.textContains("recurso especial", "recurso extraordinario", "recurso extraordinário", "agravo de instrumento")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Rito de juizado com escalada recursal incompatível identificada no texto.",
                    Map.of("rito", context.normalizedRito())
            ));
        }
        if (context != null && context.classeContains("execucao", "execução") && context.textContains("denuncia", "denúncia", "ação penal", "acao penal")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Classe executiva com referência textual a ato inaugural penal incompatível.",
                    Map.of("classe", context.normalizedClasse())
            ));
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("ramo", context == null ? "" : context.normalizedRamo());
        diagnostics.put("rito", context == null ? "" : context.normalizedRito());
        diagnostics.put("classe", context == null ? "" : context.normalizedClasse());
        diagnostics.put("hasIssues", !issues.isEmpty());
        return LegalSymbolicValidationOutcome.of(engineCode(), issues, diagnostics);
    }
}
