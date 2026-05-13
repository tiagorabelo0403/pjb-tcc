package com.tcc.pjb.backend.ai.financeira.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhase;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhaseName;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceContradictionAnalyzer;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceContradictionReport;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceContradictionResolution;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceContradictionResolver;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceNormalizer;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceQualityAnalyzer;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceQualityReport;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceSufficiencyPlan;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceSufficiencyPlanner;
import com.tcc.pjb.backend.ai.core.pipeline.MinimalQuestionPlanner;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.core.util.SafeMaps;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@Component
public class FinanceiraResearcherPhase implements CognitivePhase {

    private final VectorSearchService vector;
    private final EvidenceQualityAnalyzer quality;
    private final EvidenceContradictionAnalyzer contradiction;
    private final EvidenceContradictionResolver contradictionResolver;
    private final EvidenceSufficiencyPlanner planner;
    private final MinimalQuestionPlanner questionPlanner;

    public FinanceiraResearcherPhase(VectorSearchService vector,
                                    EvidenceSufficiencyPlanner planner,
                                    EvidenceContradictionResolver contradictionResolver,
                                    MinimalQuestionPlanner questionPlanner) {
        this.vector = vector;
        this.planner = planner;
        this.contradictionResolver = contradictionResolver;
        this.questionPlanner = questionPlanner;
        this.quality = new EvidenceQualityAnalyzer();
        this.contradiction = new EvidenceContradictionAnalyzer();
    }

    @Override
    public CognitivePhaseName name() {
        return CognitivePhaseName.RAG;
    }

    @Override
    public void execute(AgentExecutionContext ctx) {
        if (ctx.isFailFast()) return;

        Object qObj = ctx.plan().get("queries");
        List<String> queries = (qObj instanceof List<?> list)
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        int topK = 8;
        Object topKObj = ctx.plan().get("topK");
        if (topKObj instanceof Number n) topK = Math.max(1, n.intValue());

        Map<String, Object> filtros = SafeMaps.of(
                "domain", "finance",
                "capability", ctx.capability(),
                "apiVersion", ctx.version().name()
        );

        List<EvidenceItem> evidences = new ArrayList<>();
        for (String q : queries) {
            if (q == null || q.isBlank()) continue;
            VectorSearchService.VectorSearchResult r = searchByVersion(ctx.version(), q, filtros, topK);
            if (r == null || r.resultados() == null || r.resultados().isEmpty()) continue;
            for (VectorSearchService.ResultItem it : r.resultados()) {
                if (it == null) continue;
                EvidenceItem.EvidenceType type = classifyFinancialEvidence(it);
                String source = (it.ramo() != null && !it.ramo().isBlank()) ? it.ramo() : "VectorSearch";
                evidences.add(EvidenceItem.builder()
                        .docId(it.docId())
                        .tipo(type)
                        .titulo(it.titulo())
                        .fonteSistema(source)
                        .url(null)
                        .dataPublicacao(ctx.now())
                        .score(it.score())
                        .trecho("ramo=" + source + " score=" + it.score() + " cosine=" + it.cosine() + " boost=" + it.boost())
                        .build());
            }
        }

        List<EvidenceItem> normalized = EvidenceNormalizer.dedupeByDocIdKeepBestScore(
                evidences,
                ctx.version().isAtLeast(ApiVersion.V3) ? 40 : 28
        );

        ctx.addEvidences(normalized);
        ctx.putFact("evidenceCount", normalized.size());

        EvidenceQualityReport report = quality.analyze(normalized, ctx.version());
        ctx.putFact("evidenceQuality", SafeMaps.of(
                "count", report.evidenceCount(),
                "sourceDiversity", report.sourceDiversity(),
                "meanScore", report.meanScore(),
                "sufficiencyScore", report.sufficiencyScore(),
                "conflictRisk", report.conflictRisk(),
                "missingDataHints", report.missingDataHints(),
                "meta", report.meta()
        ));

        EvidenceContradictionReport cr = contradiction.analyze(normalized, AiTelemetryDomain.FINANCE, ctx.version());
        ctx.putFact("evidenceContradiction", SafeMaps.of(
                "contradictionScore", cr.contradictionScore(),
                "inconsistencyScore", cr.inconsistencyScore(),
                "positiveStance", cr.positiveStance(),
                "negativeStance", cr.negativeStance(),
                "uncertainStance", cr.uncertainStance(),
                "unknownStance", cr.unknownStance(),
                "temporalSpreadYears", cr.temporalSpreadYears(),
                "mixedJurisdiction", cr.mixedJurisdiction(),
                "signals", cr.signals(),
                "meta", cr.meta()
        ));

        EvidenceContradictionResolution resolution = contradictionResolver.resolve(normalized, AiTelemetryDomain.FINANCE, ctx.version(), cr);
        ctx.putFact("contradictionResolution", SafeMaps.of(
                "resolved", resolution.resolved(),
                "residualInconsistencyScore", resolution.residualInconsistencyScore(),
                "rationale", resolution.rationale(),
                "requiredClarifications", resolution.requiredClarifications(),
                "meta", resolution.meta()
        ));

        EvidenceSufficiencyPlan plan = planner.plan(AiTelemetryDomain.FINANCE, ctx.capability(), ctx.version(), ctx.request(), report, cr);

        List<String> candidates = new ArrayList<>();
        if (plan != null && plan.missingDataRequests() != null) candidates.addAll(plan.missingDataRequests());
        if (!resolution.requiredClarifications().isEmpty()) candidates.addAll(resolution.requiredClarifications());

        List<String> minQuestions = questionPlanner.topQuestions(AiTelemetryDomain.FINANCE, ctx.capability(), ctx.version(), candidates);

        ctx.putFact("sufficiencyPlan", SafeMaps.of(
                "missingDataRequests", plan != null ? plan.missingDataRequests() : List.of(),
                "suggestedQueryExpansions", plan != null ? plan.suggestedQueryExpansions() : List.of(),
                "minQuestions", minQuestions,
                "meta", plan != null ? plan.meta() : Map.of()
        ));

        int min = minEvidenceByVersion(ctx.version());
        boolean insufficientCount = normalized.size() < min;
        boolean insufficientQuality = report.sufficiencyScore() < (ctx.version().isAtLeast(ApiVersion.V3) ? 0.90 : 0.75);

        if (insufficientCount || insufficientQuality) {
            ctx.failFast("insufficient_financial_evidence");
            ctx.setDraft(failFastDraft("Fatos insuficientes encontrados para uma conclusão segura.", minQuestions));
        } else if (!resolution.resolved()) {
            ctx.failFast("conflicting_financial_evidence");
            ctx.setDraft(failFastDraft("Evidências recuperadas, porém há conflito/inconsistência relevante entre fontes regulatórias/mercado.", minQuestions));
        }
    }

    private static String failFastDraft(String header, List<String> minQuestions) {
        StringBuilder sb = new StringBuilder();
        sb.append(header == null ? "" : header.trim());
        sb.append("\n\nPara avançar com segurança, informe (top-5):");
        int n = 0;
        if (minQuestions != null) {
            for (String r : minQuestions) {
                if (r == null || r.isBlank()) continue;
                n++;
                if (n > 5) break;
                sb.append("\n- ").append(r.trim());
            }
        }
        if (n == 0) {
            sb.append("\n- Valores, período de referência e base regulatória/documental.");
        }
        return sb.toString();
    }

    private static EvidenceItem.EvidenceType classifyFinancialEvidence(VectorSearchService.ResultItem it) {
        String ramo = (it.ramo() == null) ? "" : it.ramo().toLowerCase(java.util.Locale.ROOT);
        String title = (it.titulo() == null) ? "" : it.titulo().toLowerCase(java.util.Locale.ROOT);
        String s = ramo + " " + title;
        if (containsAny(s, "resolução", "normativa", "bacen", "cvm", "regulation", "regulament")) {
            return EvidenceItem.EvidenceType.REGULATORY;
        }
        if (containsAny(s, "market", "câmbio", "taxa", "selic", "índice", "inflation", "price")) {
            return EvidenceItem.EvidenceType.MARKET_DATA;
        }
        return EvidenceItem.EvidenceType.OUTRO;
    }

    private static boolean containsAny(String s, String... needles) {
        if (s == null || s.isBlank() || needles == null) return false;
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (s.contains(n)) return true;
        }
        return false;
    }

    private static int minEvidenceByVersion(ApiVersion v) {
        if (v != null && v.isAtLeast(ApiVersion.V3)) return 3;
        if (v != null && v.isAtLeast(ApiVersion.V2)) return 2;
        return 1;
    }

    private VectorSearchService.VectorSearchResult searchByVersion(ApiVersion v,
                                                                  String q,
                                                                  Map<String, Object> filtros,
                                                                  int topK) {
        if (v == null) return vector.searchSimilarResult(q, filtros, topK);
        if (v.isAtLeast(ApiVersion.V3)) return vector.searchSimilarV3(q, filtros, topK);
        if (v.isAtLeast(ApiVersion.V2)) return vector.searchSimilarV2(q, filtros, topK);
        return vector.searchSimilarV1(q, filtros, topK);
    }
}
