package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class JuridicaSymbolicValidationExecutionService {

    private final List<LegalDeterministicRuleEngine> ruleEngines;

    public JuridicaSymbolicValidationExecutionService(List<LegalDeterministicRuleEngine> ruleEngines) {
        this.ruleEngines = List.copyOf(Objects.requireNonNull(ruleEngines, "ruleEngines"));
    }

    public LegalSymbolicValidationExecution execute(LegalSymbolicValidationContext context, List<String> requestedEngineCodes) {
        Set<String> requested = normalizeRequestedCodes(requestedEngineCodes);
        List<LegalSymbolicValidationOutcome> outcomes = ruleEngines.stream()
                .filter(engine -> requested.isEmpty() || requested.stream().anyMatch(engine::supports))
                .map(engine -> engine.evaluate(context))
                .toList();
        List<String> contradictions = outcomes.stream()
                .flatMap(outcome -> outcome.issues().stream())
                .filter(LegalSymbolicValidationIssue::isContradiction)
                .map(LegalSymbolicValidationIssue::message)
                .distinct()
                .toList();
        List<String> missingEvidence = outcomes.stream()
                .flatMap(outcome -> outcome.issues().stream())
                .filter(LegalSymbolicValidationIssue::isMissingEvidence)
                .map(LegalSymbolicValidationIssue::message)
                .distinct()
                .toList();
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("requestedEngineCodes", List.copyOf(requested));
        diagnostics.put("executedEngineCodes", outcomes.stream().map(LegalSymbolicValidationOutcome::engineCode).toList());
        diagnostics.put("outcomeCount", outcomes.size());
        diagnostics.put("blockingOutcomeCount", outcomes.stream().filter(outcome -> LegalSymbolicValidationOutcome.VERDICT_BLOCK.equals(outcome.verdict())).count());
        diagnostics.put("warningOutcomeCount", outcomes.stream().filter(outcome -> LegalSymbolicValidationOutcome.VERDICT_WARN.equals(outcome.verdict())).count());
        return new LegalSymbolicValidationExecution(
                overallStatus(contradictions, missingEvidence),
                outcomes,
                contradictions,
                missingEvidence,
                diagnostics
        );
    }

    private Set<String> normalizeRequestedCodes(List<String> requestedEngineCodes) {
        LinkedHashSet<String> requested = new LinkedHashSet<>();
        if (requestedEngineCodes == null) {
            return requested;
        }
        for (String code : requestedEngineCodes) {
            if (code != null && !code.isBlank()) {
                requested.add(code.trim().toUpperCase(java.util.Locale.ROOT));
            }
        }
        return requested;
    }

    private String overallStatus(List<String> contradictions, List<String> missingEvidence) {
        if (contradictions != null && !contradictions.isEmpty()) {
            return LegalSymbolicValidationExecution.STATUS_BLOCK;
        }
        if (missingEvidence != null && !missingEvidence.isEmpty()) {
            return LegalSymbolicValidationExecution.STATUS_WARN;
        }
        return LegalSymbolicValidationExecution.STATUS_PASS;
    }
}
