package com.tcc.pjb.backend.ai.juridica.pipeline;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhase;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhaseName;
import com.tcc.pjb.backend.ai.scope.MateriaDecision;
import com.tcc.pjb.backend.ai.scope.MateriaInferenceEngine;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.service.SigiloService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JuridicaStrategistPhase implements CognitivePhase {

    private final MateriaInferenceEngine materiaInferenceEngine;
    private final SigiloService sigiloService;

    public JuridicaStrategistPhase(MateriaInferenceEngine materiaInferenceEngine, SigiloService sigiloService) {
        this.materiaInferenceEngine = materiaInferenceEngine;
        this.sigiloService = sigiloService;
    }

    @Override
    public CognitivePhaseName name() {
        return CognitivePhaseName.THINK;
    }

    @Override
    public void execute(AgentExecutionContext ctx) {
        IARequest req = ctx.request();

        String pergunta = safe(req.getSafeString("pergunta"));
        String assunto = safe(req.getSafeString("assunto"));
        String materiaHintRaw = safe(firstNonBlank(req.getSafeString("materia"), req.getSafeString("materiaPrincipal")));
        String rito = safe(firstNonBlank(req.getSafeString("rito"), req.getSafeString("ritoProcessual")));
        String jurisdicao = safe(firstNonBlank(req.getSafeString("jurisdicao"), req.getSafeString("tipoJustica")));
        String uf = safe(firstNonBlank(req.getSafeString("uf"), req.getSafeString("ufFato"), req.getSafeString("ufProtocolo")));
        String procedureFamily = safe(req.getSafeString("resolvedProcedureFamily"));
        String petitionModel = safe(req.getSafeString("recommendedPetitionModelCode"));

        String corpus = buildCorpus(pergunta, assunto, materiaHintRaw, rito, jurisdicao, uf, procedureFamily, petitionModel);
        MateriaDecision inferred = materiaInferenceEngine.infer(corpus);

        MateriaJurisdicao hinted = MateriaJurisdicao.fromString(materiaHintRaw);
        MateriaJurisdicao materiaFinal = hinted != MateriaJurisdicao.MULTIMATERIA ? hinted : inferred.materia();

        SigiloService.SigiloDecision sigilo = sigiloService.avaliarCorpus(corpus);

        List<String> queries = buildQueries(pergunta, assunto, materiaFinal, procedureFamily, petitionModel, req.getPayload().get("knowledgeCadence"), req.getPayload().get("mcpRagFusion"), req.getPayload().get("strategicExecution"));
        List<String> keywords = buildKeywords(materiaFinal, inferred.signals(), procedureFamily, petitionModel, req.getPayload().get("knowledgeCadence"), req.getPayload().get("mcpRagFusion"), req.getPayload().get("strategicExecution"));

        Map<String, Object> filters = new LinkedHashMap<>();
        if (!rito.isBlank()) filters.put("rito", rito);
        if (!jurisdicao.isBlank()) filters.put("jurisdicao", jurisdicao);
        if (!uf.isBlank()) filters.put("uf", uf);
        if (!procedureFamily.isBlank()) filters.put("procedureFamily", procedureFamily);
        if (!petitionModel.isBlank()) filters.put("petitionModel", petitionModel);
        filters.put("materia", materiaFinal.name());
        filters.put("sigilo", sigilo.nivel().name());

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("pergunta", pergunta);
        plan.put("assunto", assunto);
        plan.put("materia", materiaFinal.name());
        plan.put("materiaConfidence", inferred.confidence());
        plan.put("materiaSignals", inferred.signals());
        plan.put("queries", queries);
        plan.put("keywords", keywords);
        plan.put("filters", filters);
        plan.put("sigiloSignals", sigilo.signals().stream().map(Enum::name).toList());
        plan.put("sigiloScore", sigilo.score());
        applyGovernanceHints(req.getPayload(), plan);
        applyStrategicHints(req.getPayload(), plan);

        ctx.plan(plan);
    }

    private static List<String> buildQueries(String pergunta,
                                             String assunto,
                                             MateriaJurisdicao materia,
                                             String procedureFamily,
                                             String petitionModel,
                                             Object knowledgeCadence,
                                             Object fusionPlan,
                                             Object strategicExecution) {
        LinkedHashSet<String> qs = new LinkedHashSet<>();
        add(qs, pergunta);
        add(qs, assunto);
        add(qs, procedureFamily);
        add(qs, petitionModel);

        switch (materia) {
            case SAUDE -> {
                add(qs, "direito a saude tutela de urgencia obrigacao de fazer");
                add(qs, "plano de saude rol ans negativa de cobertura");
                add(qs, "fornecimento de medicamento sus laudo medico");
            }
            case PREVIDENCIARIA -> {
                add(qs, "inss beneficio incapacidade prova pericial");
                add(qs, "aposentadoria tempo contribuicao carencia");
            }
            case TRABALHISTA -> {
                add(qs, "clt horas extras onus da prova jornada");
                add(qs, "fgts verbas rescisorias multa 477");
            }
            case EXECUCAO_FISCAL -> {
                add(qs, "execucao fiscal cda excecao pre executividade");
                add(qs, "prescricao intercorrente lei 6.830");
            }
            case TRIBUTARIA -> add(qs, "tributos indiretos icms iss pis cofins");
            case PENAL -> {
                add(qs, "habeas corpus requisitos prisao preventiva");
                add(qs, "nulidades processuais cadeia de custodia");
            }
            case FAMILIA -> add(qs, "alimentos guarda melhor interesse");
            case CIVIL -> add(qs, "responsabilidade civil danos morais tutela");
            case ADMINISTRATIVO -> add(qs, "servidor publico concurso licitacao improbidade");
            default -> {
            }
        }

        appendKnowledgeCadenceQueries(qs, knowledgeCadence);
        appendFusionQueries(qs, fusionPlan);
        appendStrategicQueries(qs, strategicExecution);

        if (qs.isEmpty()) {
            add(qs, pergunta);
        }

        return qs.stream().limit(12).toList();
    }

    private static List<String> buildKeywords(MateriaJurisdicao materia,
                                              List<String> signals,
                                              String procedureFamily,
                                              String petitionModel,
                                              Object knowledgeCadence,
                                              Object fusionPlan,
                                              Object strategicExecution) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (signals != null) {
            for (String s : signals) {
                if (s == null) continue;
                String v = s.trim();
                if (!v.isBlank()) out.add(v);
            }
        }
        out.add(materia.name());
        add(out, procedureFamily);
        add(out, petitionModel);
        appendKnowledgeCadenceKeywords(out, knowledgeCadence);
        appendFusionKeywords(out, fusionPlan);
        appendStrategicKeywords(out, strategicExecution);
        return out.stream().limit(24).toList();
    }

    private static void applyGovernanceHints(Map<String, Object> payload, Map<String, Object> plan) {
        Object governanceObj = payload.get("meshGovernance");
        if (!(governanceObj instanceof Map<?, ?> governanceMap)) {
            return;
        }
        Object ragObj = governanceMap.get("rag");
        if (ragObj instanceof Map<?, ?> ragMap) {
            Object topK = ragMap.get("topK");
            if (topK instanceof Number n) {
                plan.put("topK", Math.max(1, n.intValue()));
            }
            Object evidenceBudget = ragMap.get("evidenceBudget");
            if (evidenceBudget instanceof Number n) {
                plan.put("evidenceBudget", Math.max(1, n.intValue()));
            }
            Object minEvidence = ragMap.get("minEvidence");
            if (minEvidence instanceof Number n) {
                plan.put("minEvidence", Math.max(1, n.intValue()));
            }
        }
    }



    private static void applyStrategicHints(Map<String, Object> payload, Map<String, Object> plan) {
        Object strategyObj = payload.get("strategicExecution");
        if (!(strategyObj instanceof Map<?, ?> strategyMap)) {
            return;
        }
        if (strategyMap.get("profile") != null) {
            plan.put("strategyProfile", strategyMap.get("profile"));
        }
        Object plannerObj = strategyMap.get("planner");
        if (plannerObj instanceof Map<?, ?> plannerMap) {
            if (plannerMap.get("queryBudget") instanceof Number n) {
                plan.put("queryBudget", Math.max(1, n.intValue()));
            }
            if (plannerMap.get("batchMode") != null) {
                plan.put("batchMode", plannerMap.get("batchMode"));
            }
            if (plannerMap.get("connectorPriority") != null) {
                plan.put("connectorPriority", plannerMap.get("connectorPriority"));
            }
        }
        Object verifierObj = strategyMap.get("verifier");
        if (verifierObj instanceof Map<?, ?> verifierMap && verifierMap.get("mode") != null) {
            plan.put("verifierMode", verifierMap.get("mode"));
        }
    }

    private static void appendStrategicQueries(Set<String> target, Object strategyObj) {
        if (!(strategyObj instanceof Map<?, ?> strategyMap)) {
            return;
        }
        Object plannerObj = strategyMap.get("planner");
        if (plannerObj instanceof Map<?, ?> plannerMap) {
            appendFusionSeedList(target, plannerMap.get("queryHints"));
            appendFusionSeedList(target, plannerMap.get("readingGoals"));
            appendFusionSeedList(target, plannerMap.get("connectorPriority"));
        }
        Object verifierObj = strategyMap.get("verifier");
        if (verifierObj instanceof Map<?, ?> verifierMap) {
            appendFusionSeedList(target, verifierMap.get("authorityLanes"));
            appendFusionSeedList(target, verifierMap.get("mandatoryChecks"));
        }
    }

    private static void appendStrategicKeywords(Set<String> target, Object strategyObj) {
        if (!(strategyObj instanceof Map<?, ?> strategyMap)) {
            return;
        }
        if (strategyMap.get("profile") != null) {
            add(target, String.valueOf(strategyMap.get("profile")));
        }
        Object ingestionObj = strategyMap.get("ingestion");
        if (ingestionObj instanceof Map<?, ?> ingestionMap) {
            add(target, valueOf(ingestionMap.get("mode")));
            appendFusionSeedList(target, ingestionMap.get("dangerSignals"));
        }
        Object plannerObj = strategyMap.get("planner");
        if (plannerObj instanceof Map<?, ?> plannerMap) {
            appendFusionSeedList(target, plannerMap.get("connectorPriority"));
            appendFusionSeedList(target, plannerMap.get("readingGoals"));
        }
        Object verifierObj = strategyMap.get("verifier");
        if (verifierObj instanceof Map<?, ?> verifierMap) {
            add(target, valueOf(verifierMap.get("mode")));
            appendFusionSeedList(target, verifierMap.get("authorityLanes"));
            appendFusionSeedList(target, verifierMap.get("mandatoryChecks"));
        }
    }

    private static void appendFusionQueries(Set<String> target, Object fusionObj) {
        if (fusionObj instanceof Map<?, ?> fusionMap) {
            appendFusionSeedList(target, fusionMap.get("queryExpansionSeeds"));
        }
    }

    private static void appendFusionKeywords(Set<String> target, Object fusionObj) {
        if (!(fusionObj instanceof Map<?, ?> fusionMap)) {
            return;
        }
        Object retrievalObj = fusionMap.get("retrieval");
        if (retrievalObj instanceof Map<?, ?> retrievalMap) {
            appendFusionSeedList(target, retrievalMap.get("connectorFamilies"));
            appendFusionSeedList(target, retrievalMap.get("allowedToolClasses"));
            appendFusionSeedList(target, retrievalMap.get("queryExpansionSeeds"));
        }
        Object continuousObj = fusionMap.get("continuousReading");
        if (continuousObj instanceof Map<?, ?> continuousMap) {
            appendFusionSeedList(target, continuousMap.get("curriculumFocus"));
            appendFusionSeedList(target, continuousMap.get("petitionFocus"));
        }
    }

    private static void appendFusionSeedList(Set<String> target, Object values) {
        if (!(values instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            add(target, item == null ? null : item.toString());
        }
    }

    private static void appendKnowledgeCadenceQueries(Set<String> target, Object cadenceObj) {
        if (!(cadenceObj instanceof Map<?, ?> cadenceMap)) {
            return;
        }
        Object seeds = cadenceMap.get("queryExpansionSeeds");
        if (seeds instanceof List<?> list) {
            for (Object item : list) {
                add(target, item == null ? null : item.toString());
            }
        }
        Object blueprintObj = cadenceMap.get("petitionBlueprint");
        if (blueprintObj instanceof Map<?, ?> blueprintMap) {
            add(target, valueOf(blueprintMap.get("procedureFamily")));
            add(target, valueOf(blueprintMap.get("recommendedModelCode")));
        }
    }

    private static void appendKnowledgeCadenceKeywords(Set<String> target, Object cadenceObj) {
        if (!(cadenceObj instanceof Map<?, ?> cadenceMap)) {
            return;
        }
        Object curriculumObj = cadenceMap.get("curriculum");
        if (curriculumObj instanceof Map<?, ?> curriculumMap) {
            add(target, valueOf(curriculumMap.get("ramoCodigo")));
            appendList(target, curriculumMap.get("materiasPrioritarias"));
            appendList(target, curriculumMap.get("legislacaoChave"));
        }
        Object blueprintObj = cadenceMap.get("petitionBlueprint");
        if (blueprintObj instanceof Map<?, ?> blueprintMap) {
            add(target, valueOf(blueprintMap.get("procedureFamily")));
            add(target, valueOf(blueprintMap.get("recommendedModelCode")));
            appendList(target, blueprintMap.get("requiredDocuments"));
        }
    }

    private static void appendList(Set<String> target, Object values) {
        if (!(values instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            add(target, item == null ? null : item.toString());
        }
    }

    private static String buildCorpus(String... parts) {
        StringBuilder sb = new StringBuilder(512);
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(p);
        }
        return sb.toString();
    }

    private static void add(Set<String> s, String v) {
        if (v == null) return;
        String x = v.trim();
        if (!x.isBlank()) s.add(x);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
