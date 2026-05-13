package com.tcc.pjb.backend.ai.juridica.eval;

import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpServerProfile;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayArtifact;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalSuite;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LegalEvalReplayRunner {

    private final LegalBenchmarkCatalog benchmarkCatalog;
    private final LegalMcpPlanScorer planScorer;
    private final LegalMcpServerPromotionPolicy promotionPolicy;
    private final LegalMcpServerDemotionPolicy demotionPolicy;

    public LegalEvalReplayRunner(LegalBenchmarkCatalog benchmarkCatalog,
                                 LegalMcpPlanScorer planScorer,
                                 LegalMcpServerPromotionPolicy promotionPolicy,
                                 LegalMcpServerDemotionPolicy demotionPolicy) {
        this.benchmarkCatalog = Objects.requireNonNull(benchmarkCatalog, "benchmarkCatalog");
        this.planScorer = Objects.requireNonNull(planScorer, "planScorer");
        this.promotionPolicy = Objects.requireNonNull(promotionPolicy, "promotionPolicy");
        this.demotionPolicy = Objects.requireNonNull(demotionPolicy, "demotionPolicy");
    }

    public LegalEvalReplayResult run(LegalMcpServerProfile.ResolveRequest request, LegalMcpExecutionPlan plan) {
        LegalEvalSuite suite = benchmarkCatalog.resolveSuite(request);
        var metrics = planScorer.score(suite, plan);
        double qualityScore = round(planScorer.qualityScore(metrics));
        boolean passed = planScorer.passed(metrics);
        var promotionCandidates = promotionPolicy.promote(suite, plan, metrics, qualityScore);
        var demotionCandidates = demotionPolicy.demote(suite, plan, metrics, qualityScore);
        String replayId = "LEGAL_MCP_REPLAY_" + UUID.randomUUID();
        var artifact = new LegalEvalReplayArtifact(
                replayId,
                suite.suiteId(),
                request == null ? null : request.capability(),
                request == null || request.version() == null ? "V3" : request.version().name(),
                plan.selectionMode(),
                plan.pinnedServers() == null ? List.of() : plan.pinnedServers().stream().map(LegalMcpServerDescriptor::serverId).toList(),
                plan.safeguards(),
                replayContext(request, plan)
        );
        return new LegalEvalReplayResult(
                replayId,
                suite.suiteId(),
                suite.scope(),
                passed,
                qualityScore,
                metrics,
                promotionCandidates,
                demotionCandidates,
                adaptationHints(request, plan, qualityScore, passed),
                artifact
        );
    }

    private Map<String, Object> replayContext(LegalMcpServerProfile.ResolveRequest request, LegalMcpExecutionPlan plan) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("capability", request == null ? null : request.capability());
        out.put("version", request == null || request.version() == null ? null : request.version().name());
        out.put("ramo", request == null ? null : request.ramo());
        out.put("rito", request == null ? null : request.rito());
        out.put("sigilo", request != null && request.sigilo());
        out.put("promptInjectionDetected", request != null && request.promptInjectionDetected());
        out.put("quarantinedContext", request != null && request.quarantinedContext());
        out.put("attachmentCount", request == null || request.attachments() == null ? 0 : request.attachments().size());
        out.put("historyDepth", request == null || request.history() == null ? 0 : request.history().size());
        out.put("serverBudget", plan.serverBudget());
        out.put("evidenceBudget", plan.evidenceBudget());
        out.values().removeIf(java.util.Objects::isNull);
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> adaptationHints(LegalMcpServerProfile.ResolveRequest request,
                                                LegalMcpExecutionPlan plan,
                                                double qualityScore,
                                                boolean passed) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("toolDiscoveryMode", "DISCOVERY_THEN_PIN".equals(plan.selectionMode()) ? "DISCOVER_THEN_PIN" : "PINNED_ONLY");
        out.put("toolExamplesPolicy", requiresCanonicalExamples(request) ? "LOAD_CANONICAL_TOOL_EXAMPLES" : "OPTIONAL");
        out.put("contextCompactionPolicy", request != null && request.history() != null && request.history().size() >= 6 ? "SLIDING_COMPACTION" : "FULL_CONTEXT_ALLOWED");
        out.put("replayStrategy", "TRANSCRIPT_CAPTURE_AND_REPLAY");
        out.put("approvalDriftPolicy", request != null && (request.sigilo() || request.promptInjectionDetected()) ? "STRICT_REVIEW" : "AUTO_READONLY_MONITORED");
        out.put("qualityBand", qualityScore >= 90.0d ? "A" : qualityScore >= 80.0d ? "B" : qualityScore >= 70.0d ? "C" : "D");
        out.put("benchmarkPassed", passed);
        out.values().removeIf(java.util.Objects::isNull);
        return Collections.unmodifiableMap(out);
    }

    private boolean requiresCanonicalExamples(LegalMcpServerProfile.ResolveRequest request) {
        String capability = request == null || request.capability() == null ? "" : request.capability().toUpperCase();
        return capability.contains("PETICAO") || capability.contains("PARECER") || capability.contains("DECISAO") || capability.contains("DESPACHO");
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
