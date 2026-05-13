package com.tcc.pjb.backend.ai.juridica.eval;

import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalCase;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalMetric;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalSuite;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpPlanScorer {

    public List<LegalEvalMetric> score(LegalEvalSuite suite, LegalMcpExecutionPlan plan) {
        Objects.requireNonNull(suite, "suite");
        Objects.requireNonNull(plan, "plan");
        List<LegalEvalMetric> metrics = new ArrayList<>();
        for (LegalEvalCase evalCase : suite.cases()) {
            metrics.add(selectionModeMetric(evalCase, plan));
            metrics.add(pinnedServersMetric(evalCase, plan));
            metrics.add(safeguardsMetric(evalCase, plan));
            metrics.add(evidenceBudgetMetric(evalCase, plan));
            metrics.add(serverBudgetMetric(evalCase, plan));
            metrics.add(trustModeMetric(evalCase, plan));
        }
        return List.copyOf(metrics);
    }

    public double qualityScore(List<LegalEvalMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return 0.0d;
        }
        return metrics.stream().mapToDouble(LegalEvalMetric::score).average().orElse(0.0d) * 100.0d;
    }

    public boolean passed(List<LegalEvalMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return false;
        }
        return metrics.stream().filter(metric -> !metric.metricId().endsWith("TRUST_MODE") || metric.expected() != null).allMatch(metric -> metric.score() >= metric.threshold());
    }

    private LegalEvalMetric selectionModeMetric(LegalEvalCase evalCase, LegalMcpExecutionPlan plan) {
        String expected = evalCase.expectedSelectionMode();
        double score = expected == null || expected.equals(plan.selectionMode()) ? 1.0d : 0.0d;
        return new LegalEvalMetric(evalCase.caseId() + "_SELECTION_MODE", "Selection mode", score, 1.0d, score >= 1.0d, plan.selectionMode(), expected);
    }

    private LegalEvalMetric pinnedServersMetric(LegalEvalCase evalCase, LegalMcpExecutionPlan plan) {
        List<String> expected = evalCase.requiredPinnedServers() == null ? List.of() : List.copyOf(evalCase.requiredPinnedServers());
        List<String> observed = plan.pinnedServers() == null ? List.of() : plan.pinnedServers().stream().map(LegalMcpServerDescriptor::serverId).toList();
        if (expected.isEmpty()) {
            return new LegalEvalMetric(evalCase.caseId() + "_PINNED_SERVERS", "Pinned servers", 1.0d, 1.0d, true, observed, expected);
        }
        LinkedHashSet<String> observedSet = new LinkedHashSet<>(observed);
        long matched = expected.stream().filter(observedSet::contains).count();
        double score = (double) matched / (double) expected.size();
        return new LegalEvalMetric(evalCase.caseId() + "_PINNED_SERVERS", "Pinned servers", score, 1.0d, score >= 1.0d, observed, expected);
    }

    private LegalEvalMetric safeguardsMetric(LegalEvalCase evalCase, LegalMcpExecutionPlan plan) {
        List<String> expected = evalCase.requiredSafeguards() == null ? List.of() : List.copyOf(evalCase.requiredSafeguards());
        List<String> observed = plan.safeguards() == null ? List.of() : List.copyOf(plan.safeguards());
        if (expected.isEmpty()) {
            return new LegalEvalMetric(evalCase.caseId() + "_SAFEGUARDS", "Safeguards", 1.0d, 1.0d, true, observed, expected);
        }
        LinkedHashSet<String> observedSet = new LinkedHashSet<>(observed);
        long matched = expected.stream().filter(observedSet::contains).count();
        double score = (double) matched / (double) expected.size();
        return new LegalEvalMetric(evalCase.caseId() + "_SAFEGUARDS", "Safeguards", score, 1.0d, score >= 1.0d, observed, expected);
    }

    private LegalEvalMetric evidenceBudgetMetric(LegalEvalCase evalCase, LegalMcpExecutionPlan plan) {
        Integer expected = evalCase.minEvidenceBudget();
        double score = expected == null || plan.evidenceBudget() >= expected ? 1.0d : 0.0d;
        return new LegalEvalMetric(evalCase.caseId() + "_EVIDENCE_BUDGET", "Evidence budget", score, 1.0d, score >= 1.0d, plan.evidenceBudget(), expected);
    }

    private LegalEvalMetric serverBudgetMetric(LegalEvalCase evalCase, LegalMcpExecutionPlan plan) {
        Integer expected = evalCase.maxServerBudget();
        double score = expected == null || plan.serverBudget() <= expected ? 1.0d : 0.0d;
        return new LegalEvalMetric(evalCase.caseId() + "_SERVER_BUDGET", "Server budget", score, 1.0d, score >= 1.0d, plan.serverBudget(), expected);
    }

    private LegalEvalMetric trustModeMetric(LegalEvalCase evalCase, LegalMcpExecutionPlan plan) {
        String expected = evalCase.expectedTrustMode();
        double score = expected == null || expected.equals(plan.trustMode()) ? 1.0d : 0.0d;
        return new LegalEvalMetric(evalCase.caseId() + "_TRUST_MODE", "Trust mode", score, 1.0d, score >= 1.0d, plan.trustMode(), expected);
    }
}
