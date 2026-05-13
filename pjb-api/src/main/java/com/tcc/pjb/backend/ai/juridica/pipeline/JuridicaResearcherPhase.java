package com.tcc.pjb.backend.ai.juridica.pipeline;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JuridicaResearcherPhase implements CognitivePhase {

    private final VectorSearchService vector;
    private final EvidenceQualityAnalyzer quality;
    private final EvidenceContradictionAnalyzer contradiction;
    private final EvidenceContradictionResolver contradictionResolver;
    private final EvidenceSufficiencyPlanner planner;
    private final MinimalQuestionPlanner questionPlanner;
    private final JuridicaSemanticSourceDistillationService distillationService;

    public JuridicaResearcherPhase(VectorSearchService vector,
                                   EvidenceSufficiencyPlanner planner,
                                   EvidenceContradictionResolver contradictionResolver,
                                   MinimalQuestionPlanner questionPlanner,
                                   JuridicaSemanticSourceDistillationService distillationService) {
        this.vector = vector;
        this.planner = planner;
        this.contradictionResolver = contradictionResolver;
        this.questionPlanner = questionPlanner;
        this.distillationService = distillationService;
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

        List<String> queries = resolveQueries(ctx);
        int topK = resolveTopK(ctx);
        int evidenceBudget = resolveEvidenceBudget(ctx);
        int minEvidence = resolveMinEvidence(ctx);
        Map<String, Object> filtros = buildFilters(ctx);
        Map<String, Object> strategicExecution = resolveStrategicExecution(ctx);

        List<EvidenceItem> evidences = new ArrayList<>();
        List<String> touchedQueries = new ArrayList<>();
        ArrayList<Map<String, Object>> distillationAudits = new ArrayList<>();
        ArrayList<String> expansions = new ArrayList<>();

        for (String q : queries) {
            if (q == null || q.isBlank()) continue;
            touchedQueries.add(q);
            VectorSearchService.VectorSearchResult raw = searchByVersion(ctx.version(), q, filtros, topK);
            JuridicaSemanticSourceDistillationService.DistillationResult distilled = distillationService.distill(ctx, q, raw, evidenceBudget);
            evidences.addAll(distilled.evidences());
            if (!distilled.metadata().isEmpty()) {
                distillationAudits.add(Map.of(
                        "query", q,
                        "meta", distilled.metadata()
                ));
                Object seeds = distilled.metadata().get("expansionSeeds");
                if (seeds instanceof List<?> list) {
                    for (Object item : list) {
                        if (item == null) continue;
                        String value = String.valueOf(item).trim();
                        if (!value.isBlank()) expansions.add(value);
                    }
                }
            }
        }

        List<EvidenceItem> normalized = EvidenceNormalizer.dedupeByDocIdKeepBestScore(
                evidences,
                ctx.version().isAtLeast(ApiVersion.V3) ? Math.max(60, evidenceBudget * 4) : Math.max(40, evidenceBudget * 4)
        );

        ctx.addEvidences(normalized);
        ctx.putFact("ragQueries", List.copyOf(touchedQueries));
        ctx.putFact("evidenceCount", normalized.size());
        ctx.putFact("strategicExecutionApplied", strategicExecution);
        ctx.putFact("semanticSourceDistillation", SafeMaps.of(
                "auditCount", distillationAudits.size(),
                "entries", List.copyOf(distillationAudits),
                "expansionSeeds", expansions.stream().distinct().limit(12).toList()
        ));

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

        EvidenceContradictionReport cr = contradiction.analyze(normalized, AiTelemetryDomain.LEGAL, ctx.version());
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

        EvidenceContradictionResolution resolution = contradictionResolver.resolve(normalized, AiTelemetryDomain.LEGAL, ctx.version(), cr);
        ctx.putFact("contradictionResolution", SafeMaps.of(
                "resolved", resolution.resolved(),
                "residualInconsistencyScore", resolution.residualInconsistencyScore(),
                "rationale", resolution.rationale(),
                "requiredClarifications", resolution.requiredClarifications(),
                "meta", resolution.meta()
        ));

        EvidenceSufficiencyPlan plan = planner.plan(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), ctx.request(), report, cr);

        List<String> candidates = new ArrayList<>();
        if (plan != null && plan.missingDataRequests() != null) candidates.addAll(plan.missingDataRequests());
        if (!resolution.requiredClarifications().isEmpty()) candidates.addAll(resolution.requiredClarifications());
        candidates.addAll(expansions.stream().distinct().limit(6).toList());

        List<String> minQuestions = questionPlanner.topQuestions(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), candidates);

        ctx.putFact("sufficiencyPlan", SafeMaps.of(
                "missingDataRequests", plan != null ? plan.missingDataRequests() : List.of(),
                "suggestedQueryExpansions", plan != null ? plan.suggestedQueryExpansions() : List.of(),
                "minQuestions", minQuestions,
                "meta", plan != null ? plan.meta() : Map.of()
        ));

        boolean insufficientCount = normalized.size() < minEvidence;
        boolean insufficientQuality = report.sufficiencyScore() < requiredQuality(ctx.version(), ctx.request().getPayload());

        if (insufficientCount || insufficientQuality) {
            ctx.failFast("insufficient_legal_evidence");
            ctx.setDraft(failFastDraft("Fatos insuficientes encontrados para uma conclusão segura.", minQuestions));
        } else if (!resolution.resolved()) {
            ctx.failFast("conflicting_legal_evidence");
            ctx.setDraft(failFastDraft("Evidências recuperadas, porém há conflito/inconsistência relevante entre precedentes/fontes.", minQuestions));
        }
    }

    private List<String> resolveQueries(AgentExecutionContext ctx) {
        Object qObj = ctx.plan().get("queries");
        ArrayList<String> queries = new ArrayList<>();
        if (qObj instanceof List<?> list) {
            for (Object item : list) {
                if (item == null) continue;
                String value = String.valueOf(item).trim();
                if (value.isBlank()) continue;
                queries.add(value);
            }
        }
        Object cadenceObj = ctx.request().getPayload().get("knowledgeCadence");
        if (cadenceObj instanceof Map<?, ?> cadenceMap) {
            Object seeds = cadenceMap.get("queryExpansionSeeds");
            if (seeds instanceof List<?> list) {
                for (Object item : list) {
                    if (item == null) continue;
                    String value = String.valueOf(item).trim();
                    if (value.isBlank()) continue;
                    queries.add(value);
                    if (queries.size() >= 12) break;
                }
            }
        }
        Object fusionObj = ctx.request().getPayload().get("mcpRagFusion");
        if (fusionObj instanceof Map<?, ?> fusionMap) {
            Object seeds = fusionMap.get("queryExpansionSeeds");
            if (seeds instanceof List<?> list) {
                for (Object item : list) {
                    if (item == null) continue;
                    String value = String.valueOf(item).trim();
                    if (value.isBlank()) continue;
                    queries.add(value);
                }
            }
        }
        Object strategyObj = ctx.request().getPayload().get("strategicExecution");
        if (strategyObj instanceof Map<?, ?> strategyMap) {
            Object plannerObj = strategyMap.get("planner");
            if (plannerObj instanceof Map<?, ?> plannerMap) {
                Object hints = plannerMap.get("queryHints");
                if (hints instanceof List<?> list) {
                    for (Object item : list) {
                        if (item == null) continue;
                        String value = String.valueOf(item).trim();
                        if (value.isBlank()) continue;
                        queries.add(value);
                    }
                }
                Object goals = plannerMap.get("readingGoals");
                if (goals instanceof List<?> list) {
                    for (Object item : list) {
                        if (item == null) continue;
                        String value = String.valueOf(item).trim();
                        if (value.isBlank()) continue;
                        queries.add(value);
                    }
                }
                int queryBudget = 16;
                if (plannerMap.get("queryBudget") instanceof Number n) {
                    queryBudget = Math.max(queryBudget, Math.min(24, n.intValue()));
                }
                return queries.stream().distinct().limit(queryBudget).toList();
            }
        }
        return queries.stream().distinct().limit(16).toList();
    }

    private int resolveTopK(AgentExecutionContext ctx) {
        Object topKObj = ctx.plan().get("topK");
        int topK = topKObj instanceof Number n ? Math.max(1, n.intValue()) : 10;
        Object governanceObj = ctx.request().getPayload().get("meshGovernance");
        if (governanceObj instanceof Map<?, ?> governanceMap) {
            Object ragObj = governanceMap.get("rag");
            if (ragObj instanceof Map<?, ?> ragMap && ragMap.get("topK") instanceof Number n) {
                topK = Math.max(topK, n.intValue());
            }
        }
        Object fusionObj = ctx.request().getPayload().get("mcpRagFusion");
        if (fusionObj instanceof Map<?, ?> fusionMap) {
            Object retrieval = fusionMap.get("retrieval");
            if (retrieval instanceof Map<?, ?> retrievalMap && retrievalMap.get("topKOverride") instanceof Number n) {
                topK = Math.max(topK, n.intValue());
            }
        }
        return topK;
    }

    private int resolveEvidenceBudget(AgentExecutionContext ctx) {
        Object governanceObj = ctx.request().getPayload().get("meshGovernance");
        if (governanceObj instanceof Map<?, ?> governanceMap) {
            Object ragObj = governanceMap.get("rag");
            if (ragObj instanceof Map<?, ?> ragMap && ragMap.get("evidenceBudget") instanceof Number n) {
                int value = Math.max(1, n.intValue());
                Object fusionObj = ctx.request().getPayload().get("mcpRagFusion");
                if (fusionObj instanceof Map<?, ?> fusionMap) {
                    Object retrieval = fusionMap.get("retrieval");
                    if (retrieval instanceof Map<?, ?> retrievalMap && retrievalMap.get("evidenceBudgetOverride") instanceof Number fn) {
                        value = Math.max(value, fn.intValue());
                    }
                }
                return value;
            }
        }
        Object fusionObj = ctx.request().getPayload().get("mcpRagFusion");
        if (fusionObj instanceof Map<?, ?> fusionMap) {
            Object retrieval = fusionMap.get("retrieval");
            if (retrieval instanceof Map<?, ?> retrievalMap && retrievalMap.get("evidenceBudgetOverride") instanceof Number n) {
                return Math.max(1, n.intValue());
            }
        }
        return ctx.version().isAtLeast(ApiVersion.V3) ? 12 : 8;
    }

    private int resolveMinEvidence(AgentExecutionContext ctx) {
        Object governanceObj = ctx.request().getPayload().get("meshGovernance");
        if (governanceObj instanceof Map<?, ?> governanceMap) {
            Object ragObj = governanceMap.get("rag");
            if (ragObj instanceof Map<?, ?> ragMap && ragMap.get("minEvidence") instanceof Number n) {
                int value = Math.max(minEvidenceByVersion(ctx.version()), n.intValue());
                Object fusionObj = ctx.request().getPayload().get("mcpRagFusion");
                if (fusionObj instanceof Map<?, ?> fusionMap) {
                    Object retrieval = fusionMap.get("retrieval");
                    if (retrieval instanceof Map<?, ?> retrievalMap && retrievalMap.get("minEvidenceOverride") instanceof Number fn) {
                        value = Math.max(value, fn.intValue());
                    }
                }
                return value;
            }
        }
        Object fusionObj = ctx.request().getPayload().get("mcpRagFusion");
        if (fusionObj instanceof Map<?, ?> fusionMap) {
            Object retrieval = fusionMap.get("retrieval");
            if (retrieval instanceof Map<?, ?> retrievalMap && retrievalMap.get("minEvidenceOverride") instanceof Number n) {
                return Math.max(minEvidenceByVersion(ctx.version()), n.intValue());
            }
        }
        return minEvidenceByVersion(ctx.version());
    }

    private double requiredQuality(ApiVersion version, Map<String, Object> payload) {
        double floor = version.isAtLeast(ApiVersion.V3) ? 0.90 : 0.75;
        Object governanceObj = payload.get("meshGovernance");
        if (governanceObj instanceof Map<?, ?> governanceMap) {
            Object risk = governanceMap.get("injectionRiskScore");
            if (risk instanceof Number n && n.intValue() >= 35) {
                return Math.max(floor, 0.92d);
            }
        }
        return floor;
    }

    private Map<String, Object> buildFilters(AgentExecutionContext ctx) {
        LinkedHashMap<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("domain", "legal");
        filtros.put("capability", ctx.capability());
        filtros.put("apiVersion", ctx.version().name());
        Object planFilters = ctx.plan().get("filters");
        if (planFilters instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                filtros.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        Object governanceObj = ctx.request().getPayload().get("meshGovernance");
        if (governanceObj instanceof Map<?, ?> governanceMap) {
            Object toolPolicy = governanceMap.get("toolPolicy");
            if (toolPolicy instanceof Map<?, ?> toolMap && toolMap.get("effectiveMode") != null) {
                filtros.put("toolMode", toolMap.get("effectiveMode"));
            }
            Object sourceScope = governanceMap.get("sourceScope");
            if (sourceScope instanceof Map<?, ?> sourceMap && sourceMap.get("procedureFamily") != null) {
                filtros.put("procedureFamily", sourceMap.get("procedureFamily"));
            }
        }
        Object fusionObj = ctx.request().getPayload().get("mcpRagFusion");
        if (fusionObj instanceof Map<?, ?> fusionMap) {
            if (fusionMap.get("profile") != null) {
                filtros.put("fusionProfile", fusionMap.get("profile"));
            }
            Object retrieval = fusionMap.get("retrieval");
            if (retrieval instanceof Map<?, ?> retrievalMap) {
                if (retrievalMap.get("toolSearchEnabled") != null) filtros.put("toolSearchEnabled", retrievalMap.get("toolSearchEnabled"));
                if (retrievalMap.get("deferLoading") != null) filtros.put("deferMcpLoading", retrievalMap.get("deferLoading"));
                if (retrievalMap.get("connectorFamilies") != null) filtros.put("connectorFamilies", retrievalMap.get("connectorFamilies"));
                if (retrievalMap.get("evidenceLanes") != null) filtros.put("evidenceLanes", retrievalMap.get("evidenceLanes"));
            }
            Object mcp = fusionMap.get("mcp");
            if (mcp instanceof Map<?, ?> mcpMap) {
                if (mcpMap.get("approvalMode") != null) filtros.put("mcpApprovalMode", mcpMap.get("approvalMode"));
            }
        }
        Object strategyObj = ctx.request().getPayload().get("strategicExecution");
        if (strategyObj instanceof Map<?, ?> strategyMap) {
            if (strategyMap.get("profile") != null) filtros.put("strategyProfile", strategyMap.get("profile"));
            Object ingestion = strategyMap.get("ingestion");
            if (ingestion instanceof Map<?, ?> ingestionMap) {
                if (ingestionMap.get("mode") != null) filtros.put("ingestionMode", ingestionMap.get("mode"));
                if (ingestionMap.get("pageBudget") != null) filtros.put("pageBudget", ingestionMap.get("pageBudget"));
                if (ingestionMap.get("batchRead") != null) filtros.put("batchRead", ingestionMap.get("batchRead"));
            }
            Object verifier = strategyMap.get("verifier");
            if (verifier instanceof Map<?, ?> verifierMap) {
                if (verifierMap.get("mode") != null) filtros.put("verifierMode", verifierMap.get("mode"));
                if (verifierMap.get("authorityLanes") != null) filtros.put("authorityLanes", verifierMap.get("authorityLanes"));
            }
            Object protocol = strategyMap.get("protocol");
            if (protocol instanceof Map<?, ?> protocolMap && protocolMap.get("enabled") != null) {
                filtros.put("protocolStage", protocolMap.get("enabled"));
            }
        }
        return Map.copyOf(filtros);
    }


    private Map<String, Object> resolveStrategicExecution(AgentExecutionContext ctx) {
        Object strategyObj = ctx.request().getPayload().get("strategicExecution");
        if (!(strategyObj instanceof Map<?, ?> strategyMap)) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (strategyMap.get("profile") != null) out.put("profile", strategyMap.get("profile"));
        Object ingestionObj = strategyMap.get("ingestion");
        if (ingestionObj instanceof Map<?, ?> ingestionMap) {
            if (ingestionMap.get("mode") != null) out.put("ingestionMode", ingestionMap.get("mode"));
            if (ingestionMap.get("batchRead") != null) out.put("batchRead", ingestionMap.get("batchRead"));
            if (ingestionMap.get("pageBudget") != null) out.put("pageBudget", ingestionMap.get("pageBudget"));
        }
        Object verifierObj = strategyMap.get("verifier");
        if (verifierObj instanceof Map<?, ?> verifierMap) {
            if (verifierMap.get("mode") != null) out.put("verifierMode", verifierMap.get("mode"));
            if (verifierMap.get("authorityLanes") != null) out.put("authorityLanes", verifierMap.get("authorityLanes"));
        }
        return Collections.unmodifiableMap(out);
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
            sb.append("\n- Matéria/assunto, tribunal/UF, datas e documentos essenciais do caso.");
        }
        return sb.toString();
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
