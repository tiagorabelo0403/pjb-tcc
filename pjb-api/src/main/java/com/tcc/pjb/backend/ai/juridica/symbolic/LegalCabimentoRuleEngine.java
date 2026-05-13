package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalCabimentoRuleEngine implements LegalDeterministicRuleEngine {

    @Override
    public String engineCode() {
        return LegalSymbolicValidationCatalog.ENGINE_CABIMENTO;
    }

    @Override
    public LegalSymbolicValidationOutcome evaluate(LegalSymbolicValidationContext context) {
        boolean relevant = context != null && (context.objectiveContains("cabimento", "recurso", "peca", "peça")
                || context.classeContains("recurso", "agravo", "apelacao", "apelação", "embargos", "mandado", "habeas")
                || context.textContains("recurso", "agravo", "apelacao", "apelação", "embargos", "mandado", "habeas"));
        boolean hasProceduralArtifact = context != null && context.textContains(
                "peticao", "petição", "recurso", "acao", "ação", "mandado", "habeas", "apelação", "apelacao", "agravo", "embargos", "contestacao", "contestação", "denuncia", "denúncia", "queixa"
        );
        List<LegalSymbolicValidationIssue> issues = new ArrayList<>();
        if (relevant && !hasProceduralArtifact) {
            issues.add(LegalSymbolicValidationIssue.missingEvidence(
                    engineCode(),
                    "Análise de cabimento sem indicação mínima da peça, recurso ou ação pretendida.",
                    Map.of("classe", context == null ? "" : context.normalizedClasse())
            ));
        }
        if (context != null && context.ritoContains("juizado")
                && context.textContains("agravo de instrumento", "recurso especial", "recurso extraordinario", "recurso extraordinário")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Rito de juizado com peça recursal incompatível com a trilha simplificada indicada.",
                    Map.of("rito", context.normalizedRito())
            ));
        }
        if (context != null && context.classeContains("recurso inominado") && context.textContains("apelação", "apelacao")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Classe recursal indica recurso inominado, mas o texto sustenta apelação.",
                    Map.of("classe", context.normalizedClasse())
            ));
        }
        if (context != null && context.classeContains("apelação", "apelacao") && context.textContains("recurso inominado")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Classe recursal indica apelação, mas o texto sustenta recurso inominado.",
                    Map.of("classe", context.normalizedClasse())
            ));
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("relevant", relevant);
        diagnostics.put("proceduralArtifactDetected", hasProceduralArtifact);
        diagnostics.put("classe", context == null ? "" : context.normalizedClasse());
        return LegalSymbolicValidationOutcome.of(engineCode(), issues, diagnostics);
    }
}
