package com.tcc.pjb.backend.ai.juridica.policy;

import com.tcc.pjb.backend.ai.academy.CurriculumKnowledgeService;
import com.tcc.pjb.backend.ai.academy.CurriculumSnapshot;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.policy.support.LegalAiPolicyTextCatalogService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.platform.versioning.VersionHints;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoEditorBlueprintCatalogService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class JuridicaAdaptiveMeshGovernanceService {

    private static final int MAX_TEXT_LENGTH = 24000;
    private static final int MAX_LIST_ITEMS = 24;
    private static final int MAX_MAP_ENTRIES = 64;
    private static final int MAX_DEPTH = 5;
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern INJECTION = Pattern.compile("(?i)(ignore\\s+previous|ignore\\s+all|system\\s+prompt|developer\\s+message|jailbreak|prompt\\s*injection|tool\\s*call|function\\s*call|base64|curl\\s+http|wget\\s+http|```json|<tool|</tool>|mcp\\s*server|oauth\\s*token|api\\s*key|authorization\\s*:)" );
    private static final Pattern SENSITIVE = Pattern.compile("(?i)(senha|password|token|secret|authorization|bearer\\s+[a-z0-9._-]+)");

    private final CurriculumKnowledgeService curriculumKnowledgeService;
    private final PeticionamentoEditorBlueprintCatalogService editorBlueprintCatalogService;
    private final JuridicaMcpRagFusionService fusionService;
    private final JuridicaStrategicExecutionService strategicExecutionService;
    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;
    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;
    private final LegalAiPolicyTextCatalogService policyTextCatalogService;

    public JuridicaAdaptiveMeshGovernanceService(CurriculumKnowledgeService curriculumKnowledgeService,
                                                 PeticionamentoEditorBlueprintCatalogService editorBlueprintCatalogService,
                                                 JuridicaMcpRagFusionService fusionService,
                                                 JuridicaStrategicExecutionService strategicExecutionService,
                                                 JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService,
                                                 JuridicaLegalAiSpineService juridicaLegalAiSpineService,
                                                 LegalAiPolicyTextCatalogService policyTextCatalogService) {
        this.curriculumKnowledgeService = Objects.requireNonNull(curriculumKnowledgeService, "curriculumKnowledgeService");
        this.editorBlueprintCatalogService = Objects.requireNonNull(editorBlueprintCatalogService, "editorBlueprintCatalogService");
        this.fusionService = Objects.requireNonNull(fusionService, "fusionService");
        this.strategicExecutionService = Objects.requireNonNull(strategicExecutionService, "strategicExecutionService");
        this.juridicaUnifiedMeshProfileService = Objects.requireNonNull(juridicaUnifiedMeshProfileService, "juridicaUnifiedMeshProfileService");
        this.juridicaLegalAiSpineService = Objects.requireNonNull(juridicaLegalAiSpineService, "juridicaLegalAiSpineService");
        this.policyTextCatalogService = Objects.requireNonNull(policyTextCatalogService, "policyTextCatalogService");
    }

    public GovernedRequest govern(IARequest request) {
        Objects.requireNonNull(request, "request");

        LinkedHashMap<String, Object> payload = sanitizeMap(copyMap(request.getPayload()), 0);
        String capability = normalizeCapability(VersionHints.resolveCapability(payload, request.getAcao()));
        String question = firstString(payload, "pergunta", "question", "texto", "textoPeticaoLivre", "textoFatosResumido", "prompt");
        String ramo = normalize(firstString(payload, "ramoDireito", "ramo", "branch", "resolvedRamoDireito"));
        String rito = normalize(firstString(payload, "ritoProcessual", "rito", "resolvedRitoProcessual"));
        String tipoJustica = normalize(firstString(payload, "tipoJustica", "justica", "segmentoJustica", "resolvedTipoJustica"));
        String classeProcessual = normalize(firstString(payload, "classeProcessual", "classe", "resolvedClasseProcessual"));
        String assuntoTpu = normalize(firstString(payload, "assuntoTpu", "assunto", "resolvedAssuntoTpu"));
        String materiaPrincipal = normalize(firstString(payload, "materiaPrincipal", "materia", "resolvedMateriaPrincipal"));
        String naturezaJuridica = normalize(firstString(payload, "naturezaJuridica", "natureza", "resolvedNaturezaJuridica"));
        TipoUsuario tipoUsuario = TipoUsuario.fromString(firstString(payload, "tipoUsuario", "perfilUsuario", "perfil", "role"));
        boolean tutelaUrgencia = firstBoolean(payload, "tutelaUrgencia", "liminar", "urgencia", "urgência");
        boolean petitionDetected = detectPetitionPayload(payload, question);
        boolean contextoConsensual = detectConsensualContext(payload, capability, question);
        boolean sigilo = firstBoolean(payload, "sigilo", "secret", "segredoJustica") || containsAny(question, "sigilo", "segredo de justiça", "segredo de estado");
        int complexityScore = scoreComplexity(payload, capability, question, ramo, rito, classeProcessual, petitionDetected, tutelaUrgencia, sigilo);
        int injectionRisk = scoreInjectionRisk(payload, question);
        ApiVersion requestedVersion = VersionHints.resolveVersion(payload, ApiVersion.latest());
        ApiVersion effectiveVersion = resolveEffectiveVersion(requestedVersion, capability, complexityScore, injectionRisk, petitionDetected, sigilo);
        RagPolicy ragPolicy = resolveRagPolicy(effectiveVersion, complexityScore, injectionRisk, petitionDetected);
        ToolPolicy toolPolicy = resolveToolPolicy(payload, tipoUsuario, capability, injectionRisk, petitionDetected);

        PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint blueprint = editorBlueprintCatalogService.resolve(
                new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                        ramo,
                        rito,
                        tipoJustica,
                        classeProcessual,
                        assuntoTpu,
                        materiaPrincipal,
                        naturezaJuridica,
                        tipoUsuario,
                        petitionDetected,
                        tutelaUrgencia,
                        contextoConsensual,
                        extractMap(payload, "visualIdentity", "identidadeVisual")
                )
        );

        CurriculumSnapshot curriculum = curriculumKnowledgeService.snapshot(ramo, materiaPrincipal, rito);
        LinkedHashMap<String, Object> knowledgeCadence = buildKnowledgeCadence(curriculum, blueprint, petitionDetected, capability, effectiveVersion);
        JuridicaMcpRagFusionService.FusionPlan fusionPlan = fusionService.resolve(
                new JuridicaMcpRagFusionService.ResolveRequest(
                        capability,
                        effectiveVersion,
                        ramo,
                        rito,
                        tipoJustica,
                        stringValue(blueprint.editorBlueprint().get("resolvedProcedureFamily")),
                        stringValue(blueprint.editorBlueprint().get("recommendedModelCode")),
                        tipoUsuario,
                        complexityScore,
                        injectionRisk,
                        sigilo,
                        petitionDetected,
                        payload,
                        toolPolicy.toMap(),
                        knowledgeCadence
                )
        );
        JuridicaStrategicExecutionService.StrategyPlan strategicPlan = strategicExecutionService.resolve(
                new JuridicaStrategicExecutionService.ResolveRequest(
                        capability,
                        effectiveVersion,
                        ramo,
                        rito,
                        tipoJustica,
                        stringValue(blueprint.editorBlueprint().get("resolvedProcedureFamily")),
                        stringValue(blueprint.editorBlueprint().get("recommendedModelCode")),
                        tipoUsuario,
                        complexityScore,
                        injectionRisk,
                        sigilo,
                        petitionDetected,
                        payload,
                        ragPolicy.toMap(),
                        fusionPlan.asMap(),
                        knowledgeCadence
                )
        );
        var juridicaMeshProfile = juridicaUnifiedMeshProfileService.resolveForIa(
                request,
                effectiveVersion,
                capability,
                Map.of(
                        "complexityScore", complexityScore,
                        "injectionRiskScore", injectionRisk,
                        "petitionDetected", petitionDetected,
                        "sigilo", sigilo
                ),
                knowledgeCadence,
                toolPolicy.toMap()
        );

        var juridicaSpineProfile = juridicaLegalAiSpineService.resolveForIa(request, effectiveVersion, capability);

        LinkedHashMap<String, Object> governance = buildGovernance(payload, capability, effectiveVersion, complexityScore, injectionRisk, ragPolicy, toolPolicy, blueprint, curriculum, petitionDetected, tutelaUrgencia, sigilo, fusionPlan.asMap(), strategicPlan.asMap());
        governance.put("juridicaMeshProfile", juridicaMeshProfile.asMap());
        governance.put("juridicaLegalTools", juridicaMeshProfile.tools().stream().map(tool -> tool.id()).toList());
        governance.put("juridicaSpineProfile", juridicaSpineProfile.asMap());
        governance.put("juridicaStructuredOutputs", juridicaSpineProfile.structuredOutputs().stream().map(output -> output.schemaId()).toList());

        payload.put("capability", capability);
        payload.put("effectiveCapability", capability);
        payload.put("effectiveVersion", effectiveVersion.name());
        payload.put("meshGovernance", immutableMap(governance));
        payload.put("knowledgeCadence", immutableMap(knowledgeCadence));
        payload.put("ragProfile", ragPolicy.profile());
        payload.put("mcpPolicy", immutableMap(toolPolicy.toMap()));
        payload.put("mcpRagFusion", fusionPlan.asMap());
        payload.put("strategicExecution", strategicPlan.asMap());
        payload.put("toolSearchProfile", stringValue(fusionPlan.mcp().get("toolSelectionProfile")));
        payload.put("promptCacheProfile", fusionPlan.promptCache());
        payload.put("reasoningEffort", fusionPlan.reasoningEffort());
        payload.put("ssdProfile", ragPolicy.distillationProfile());
        payload.put("resolvedProcedureFamily", stringValue(blueprint.editorBlueprint().get("resolvedProcedureFamily")));
        payload.put("recommendedPetitionModelCode", stringValue(blueprint.editorBlueprint().get("recommendedModelCode")));
        payload.put("requiredDocumentsByTrack", blueprint.requiredDocuments());
        payload.put("meshGovernanceVersion", policyTextCatalogService.governancePolicyVersionV4());
        payload.put("juridicaMeshProfile", juridicaMeshProfile.asMap());
        payload.put("juridicaLegalTools", juridicaMeshProfile.tools().stream().map(tool -> tool.id()).toList());

        IARequest governed = IARequest.builder()
                .withRequestId(request.getRequestId())
                .withCorrelationId(request.getCorrelationId())
                .withOrigem(request.getOrigem())
                .withAcao(request.getAcao())
                .withUsuarioId(request.getUsuarioId())
                .withTimestamp(request.getTimestamp())
                .withPayload(payload)
                .build();

        return new GovernedRequest(governed, effectiveVersion, capability, immutableMap(governance), immutableMap(knowledgeCadence), immutableMap(toolPolicy.toMap()));
    }

    private LinkedHashMap<String, Object> buildGovernance(Map<String, Object> payload,
                                                          String capability,
                                                          ApiVersion effectiveVersion,
                                                          int complexityScore,
                                                          int injectionRisk,
                                                          RagPolicy ragPolicy,
                                                          ToolPolicy toolPolicy,
                                                          PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint blueprint,
                                                          CurriculumSnapshot curriculum,
                                                          boolean petitionDetected,
                                                          boolean tutelaUrgencia,
                                                          boolean sigilo,
                                                          Map<String, Object> fusionPlan,
                                                          Map<String, Object> strategicExecution) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("policy", policyTextCatalogService.governancePolicyVersionV3());
        out.put("effectiveVersion", effectiveVersion.name());
        out.put("capability", capability);
        out.put("complexityScore", complexityScore);
        out.put("injectionRiskScore", injectionRisk);
        out.put("petitionDetected", petitionDetected);
        out.put("tutelaUrgencia", tutelaUrgencia);
        out.put("sigilo", sigilo);
        out.put("rag", ragPolicy.toMap());
        out.put("toolPolicy", toolPolicy.toMap());
        out.put("sourceScope", buildSourceScope(curriculum, blueprint, petitionDetected));
        out.put("filters", buildFilterEnvelope(payload, effectiveVersion, injectionRisk, petitionDetected));
        out.put("knowledgeMesh", buildKnowledgeMesh(curriculum, blueprint));
        out.put("mcpRagFusion", fusionPlan == null ? Map.of() : immutableMap(fusionPlan));
        out.put("strategicExecution", strategicExecution == null ? Map.of() : immutableMap(strategicExecution));
        out.put("decisionReasons", buildDecisionReasons(capability, effectiveVersion, complexityScore, injectionRisk, petitionDetected, sigilo));
        return out;
    }

    private LinkedHashMap<String, Object> buildKnowledgeCadence(CurriculumSnapshot curriculum,
                                                                PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint blueprint,
                                                                boolean petitionDetected,
                                                                String capability,
                                                                ApiVersion effectiveVersion) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("profile", policyTextCatalogService.knowledgeCadenceProfileV2());
        out.put("activationMode", petitionDetected
                ? policyTextCatalogService.activationModeEditDraftProtocol()
                : policyTextCatalogService.activationModeQueryDraft());
        out.put("effectiveVersion", effectiveVersion.name());
        out.put("capability", capability);
        out.put("curriculum", Map.of(
                "ramoCodigo", curriculum.ramoCodigo(),
                "nome", curriculum.nome(),
                "materiasPrioritarias", limit(curriculum.materiasPrioritarias(), 8),
                "legislacaoChave", limit(curriculum.legislacaoChave(), 8),
                "principiosChave", limit(curriculum.principiosChave(), 6),
                "prazosCriticos", limit(curriculum.prazosCriticos(), 6),
                "ritosRelacionados", limit(curriculum.ritosRelacionados(), 6)
        ));
        out.put("petitionBlueprint", Map.of(
                "procedureFamily", stringValue(blueprint.editorBlueprint().get("resolvedProcedureFamily")),
                "recommendedModelCode", stringValue(blueprint.editorBlueprint().get("recommendedModelCode")),
                "requiredDocuments", limit(blueprint.requiredDocuments(), 12),
                "modelCatalog", limit(mapCodes(blueprint.petitionModels()), 12),
                "questionBlocks", limit(mapCodes(blueprint.specializedQuestionBlocks()), 12)
        ));
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        keywords.add(curriculum.ramoCodigo());
        keywords.addAll(limit(curriculum.materiasPrioritarias(), 6));
        keywords.addAll(limit(curriculum.legislacaoChave(), 6));
        keywords.add(stringValue(blueprint.editorBlueprint().get("resolvedProcedureFamily")));
        keywords.add(stringValue(blueprint.editorBlueprint().get("recommendedModelCode")));
        keywords.removeIf(v -> v == null || v.isBlank());
        out.put("queryExpansionSeeds", List.copyOf(keywords));
        return out;
    }

    private Map<String, Object> buildSourceScope(CurriculumSnapshot curriculum,
                                                 PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint blueprint,
                                                 boolean petitionDetected) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("sources", petitionDetected
                ? policyTextCatalogService.sourceScopePetitionSources()
                : policyTextCatalogService.sourceScopeQuerySources());
        out.put("curriculumRamo", curriculum.ramoCodigo());
        out.put("procedureFamily", stringValue(blueprint.editorBlueprint().get("resolvedProcedureFamily")));
        out.put("recommendedModelCode", stringValue(blueprint.editorBlueprint().get("recommendedModelCode")));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildFilterEnvelope(Map<String, Object> payload,
                                                    ApiVersion version,
                                                    int injectionRisk,
                                                    boolean petitionDetected) {
        LinkedHashMap<String, Object> filters = new LinkedHashMap<>();
        filters.put("strictPromptIsolation", injectionRisk >= 35);
        filters.put("denyPromptOverrides", true);
        filters.put("stripSecrets", containsSensitivePayload(payload));
        filters.put("denyExternalTools", injectionRisk >= 45);
        filters.put("readOnlyKnowledge", true);
        filters.put("version", version.name());
        filters.put("petitionAware", petitionDetected);
        return immutableMap(filters);
    }

    private Map<String, Object> buildKnowledgeMesh(CurriculumSnapshot curriculum,
                                                   PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint blueprint) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("curriculumModules", curriculum.materiasPrioritarias());
        out.put("legislation", curriculum.legislacaoChave());
        out.put("procedureFamily", stringValue(blueprint.editorBlueprint().get("resolvedProcedureFamily")));
        out.put("recommendedModelCode", stringValue(blueprint.editorBlueprint().get("recommendedModelCode")));
        out.put("requiredDocuments", blueprint.requiredDocuments());
        return Collections.unmodifiableMap(out);
    }

    private List<String> buildDecisionReasons(String capability,
                                              ApiVersion effectiveVersion,
                                              int complexityScore,
                                              int injectionRisk,
                                              boolean petitionDetected,
                                              boolean sigilo) {
        ArrayList<String> reasons = new ArrayList<>();
        reasons.add(policyTextCatalogService.adaptiveDecisionReasonCapabilityPrefix() + capability);
        reasons.add(policyTextCatalogService.adaptiveDecisionReasonEffectiveVersionPrefix() + effectiveVersion.name());
        reasons.add(policyTextCatalogService.adaptiveDecisionReasonComplexityPrefix() + complexityScore);
        reasons.add(policyTextCatalogService.adaptiveDecisionReasonInjectionRiskPrefix() + injectionRisk);
        if (petitionDetected) reasons.add(policyTextCatalogService.adaptiveDecisionReasonPetitionDetected());
        if (sigilo) reasons.add(policyTextCatalogService.adaptiveDecisionReasonSigiloSensitive());
        if (complexityScore >= 65) reasons.add(policyTextCatalogService.adaptiveDecisionReasonComplexCaseStrictRag());
        if (injectionRisk >= 45) reasons.add(policyTextCatalogService.adaptiveDecisionReasonExternalToolingLocked());
        return List.copyOf(reasons);
    }

    private RagPolicy resolveRagPolicy(ApiVersion version,
                                       int complexityScore,
                                       int injectionRisk,
                                       boolean petitionDetected) {
        boolean strict = injectionRisk >= 35 || petitionDetected;
        int topK;
        int evidenceBudget;
        int minEvidence;
        if (version.isAtLeast(ApiVersion.V3) || complexityScore >= 70) {
            topK = strict ? 16 : 12;
            evidenceBudget = strict ? 14 : 12;
            minEvidence = strict ? 4 : 3;
        } else if (version.isAtLeast(ApiVersion.V2) || complexityScore >= 40) {
            topK = strict ? 12 : 8;
            evidenceBudget = strict ? 10 : 8;
            minEvidence = strict ? 3 : 2;
        } else {
            topK = 6;
            evidenceBudget = 6;
            minEvidence = 1;
        }
        String profile = strict
                ? policyTextCatalogService.ragProfileStrictMultistage()
                : version.name() + policyTextCatalogService.ragProfileBalancedSuffix();
        String distillation = strict
                ? policyTextCatalogService.distillationStrictProfile()
                : policyTextCatalogService.distillationBalancedProfile();
        return new RagPolicy(profile, topK, evidenceBudget, minEvidence, distillation, strict, petitionDetected);
    }

    private ToolPolicy resolveToolPolicy(Map<String, Object> payload,
                                         TipoUsuario tipoUsuario,
                                         String capability,
                                         int injectionRisk,
                                         boolean petitionDetected) {
        String requestedMode = normalize(firstString(payload, "mcpMode", "mcp", "toolMode", "tooling"));
        boolean institutional = tipoUsuario != null && tipoUsuario.isInstitucional();
        boolean allowReadOnly = institutional && injectionRisk < 35 && !containsSensitivePayload(payload);
        String effectiveMode;
        if (injectionRisk >= 45) {
            effectiveMode = policyTextCatalogService.toolModeDisabledByRisk();
        } else if (allowReadOnly && (requestedMode.contains("MCP") || requestedMode.contains("TOOL") || petitionDetected || policyTextCatalogService.isHighStakesCapability(capability))) {
            effectiveMode = policyTextCatalogService.toolModeReadOnlyGuarded();
        } else {
            effectiveMode = policyTextCatalogService.toolModeLocalOnly();
        }
        return new ToolPolicy(requestedMode.isBlank() ? policyTextCatalogService.toolModeAuto() : requestedMode, effectiveMode, allowReadOnly, institutional, petitionDetected);
    }

    private ApiVersion resolveEffectiveVersion(ApiVersion requestedVersion,
                                               String capability,
                                               int complexityScore,
                                               int injectionRisk,
                                               boolean petitionDetected,
                                               boolean sigilo) {
        ApiVersion requested = requestedVersion == null ? ApiVersion.latest() : requestedVersion;
        if (sigilo || injectionRisk >= 55 || complexityScore >= 80) {
            return ApiVersion.V3;
        }
        if (petitionDetected || policyTextCatalogService.isHighStakesCapability(capability)) {
            return requested.isAtLeast(ApiVersion.V2) ? requested : ApiVersion.V3;
        }
        if (complexityScore >= 45 && requested == ApiVersion.V1) {
            return ApiVersion.V2;
        }
        return requested;
    }

    private int scoreComplexity(Map<String, Object> payload,
                                String capability,
                                String question,
                                String ramo,
                                String rito,
                                String classeProcessual,
                                boolean petitionDetected,
                                boolean tutelaUrgencia,
                                boolean sigilo) {
        int score = 10;
        score += Math.min(25, safeLength(question) / 700);
        score += countPresent(ramo, rito, classeProcessual) * 8;
        if (petitionDetected) score += 18;
        if (tutelaUrgencia) score += 12;
        if (sigilo) score += 10;
        if (policyTextCatalogService.isHighStakesCapability(capability)) score += 18;
        if (firstBoolean(payload, "prevention", "prevenção", "redistribuicao", "redistribuição")) score += 8;
        int documents = collectionSize(payload.get("documentosAnexados"));
        score += Math.min(12, documents * 2);
        return Math.max(0, Math.min(100, score));
    }

    private int scoreInjectionRisk(Map<String, Object> payload, String question) {
        int score = 0;
        if (question != null && INJECTION.matcher(question).find()) score += 40;
        if (question != null && SENSITIVE.matcher(question).find()) score += 20;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = entry.getKey();
            String value = Objects.toString(entry.getValue(), "");
            if (INJECTION.matcher(key).find() || INJECTION.matcher(value).find()) score += 12;
            if (SENSITIVE.matcher(value).find()) score += 8;
        }
        return Math.max(0, Math.min(100, score));
    }

    private boolean containsSensitivePayload(Map<String, Object> payload) {
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (entry.getKey() != null && SENSITIVE.matcher(entry.getKey()).find()) return true;
            if (entry.getValue() != null && SENSITIVE.matcher(String.valueOf(entry.getValue())).find()) return true;
        }
        return false;
    }

    private boolean detectPetitionPayload(Map<String, Object> payload, String question) {
        if (collectionSize(payload.get("documentosAnexados")) > 0) return true;
        if (payload.containsKey("textoPeticaoLivre")) return true;
        if (payload.containsKey("petitionModels")) return true;
        return containsAny(question, "petição", "peticao", "inicial", "contestação", "contestacao", "recurso");
    }

    private boolean detectConsensualContext(Map<String, Object> payload, String capability, String question) {
        if (firstBoolean(payload, "contextoConsensual", "acordo", "conciliacao", "conciliação", "mediacao", "mediação")) return true;
        if (containsAny(question, "acordo", "conciliação", "conciliacao", "mediação", "mediacao", "transigir")) return true;
        return capability.contains("CONCILI") || capability.contains("MEDIAC") || capability.contains("ACORDO");
    }

    private LinkedHashMap<String, Object> sanitizeMap(Map<String, Object> in, int depth) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (in == null || depth > MAX_DEPTH) return out;
        int count = 0;
        for (Map.Entry<String, Object> entry : in.entrySet()) {
            if (count >= MAX_MAP_ENTRIES) break;
            String key = sanitizeKey(entry.getKey());
            if (key == null) continue;
            Object value = sanitizeValue(entry.getValue(), depth + 1);
            if (value == null) continue;
            out.put(key, value);
            count++;
        }
        return out;
    }

    private Object sanitizeValue(Object value, int depth) {
        if (value == null) return null;
        if (depth > MAX_DEPTH) return null;
        if (value instanceof String s) return sanitizeString(s);
        if (value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count >= MAX_MAP_ENTRIES) break;
                String key = sanitizeKey(Objects.toString(entry.getKey(), null));
                if (key == null) continue;
                Object nestedValue = sanitizeValue(entry.getValue(), depth + 1);
                if (nestedValue == null) continue;
                nested.put(key, nestedValue);
                count++;
            }
            return nested.isEmpty() ? null : Map.copyOf(nested);
        }
        if (value instanceof Collection<?> collection) {
            ArrayList<Object> list = new ArrayList<>();
            int count = 0;
            for (Object item : collection) {
                if (count >= MAX_LIST_ITEMS) break;
                Object nested = sanitizeValue(item, depth + 1);
                if (nested == null) continue;
                list.add(nested);
                count++;
            }
            return list.isEmpty() ? null : List.copyOf(list);
        }
        return sanitizeString(String.valueOf(value));
    }

    private String sanitizeString(String raw) {
        if (raw == null) return null;
        String value = CONTROL.matcher(raw).replaceAll(" ");
        value = value.replace('\u0000', ' ');
        value = value.replace("\r\n", "\n");
        value = value.replace('\r', '\n');
        value = WHITESPACE.matcher(value).replaceAll(" ").trim();
        if (value.isBlank()) return null;
        if (value.length() > MAX_TEXT_LENGTH) value = value.substring(0, MAX_TEXT_LENGTH);
        return value;
    }

    private String sanitizeKey(String key) {
        if (key == null) return null;
        String value = CONTROL.matcher(key).replaceAll("");
        value = value.trim();
        if (value.isBlank()) return null;
        if (value.length() > 80) value = value.substring(0, 80);
        return value;
    }

    private Map<String, Object> copyMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) return Collections.emptyMap();
        return new LinkedHashMap<>(input);
    }

    private Map<String, Object> extractMap(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Map<?, ?> map) {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String safeKey = sanitizeKey(Objects.toString(entry.getKey(), null));
                    Object safeValue = sanitizeValue(entry.getValue(), 1);
                    if (safeKey == null || safeValue == null) continue;
                    out.put(safeKey, safeValue);
                }
                return out;
            }
        }
        return Map.of();
    }

    private String firstString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) continue;
            String text = sanitizeString(String.valueOf(value));
            if (text != null && !text.isBlank()) return text;
        }
        return "";
    }

    private boolean firstBoolean(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Boolean bool) return bool;
            if (value instanceof String text) {
                String normalized = text.trim().toLowerCase(Locale.ROOT);
                if (Set.of("true", "1", "sim", "yes").contains(normalized)) return true;
            }
        }
        return false;
    }

    private String normalizeCapability(String raw) {
        String base = normalize(raw);
        if (base.isBlank()) return "LEGAL_GENERAL_ASSIST_V3";
        return base.replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String normalize(String raw) {
        if (raw == null) return "";
        String text = sanitizeString(raw);
        return text == null ? "" : text.replace('ç', 'c').replace('Ç', 'C').trim();
    }

    private List<String> mapCodes(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        ArrayList<String> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) continue;
            String code = stringValue(row.get("code"));
            if (code == null || code.isBlank()) {
                code = stringValue(row.get("modelCode"));
            }
            if (code == null || code.isBlank()) continue;
            out.add(code);
            if (out.size() >= 24) break;
        }
        return List.copyOf(out);
    }

    private List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = sanitizeString(value);
            if (normalized == null || normalized.isBlank()) continue;
            out.add(normalized);
            if (out.size() >= Math.max(1, max)) break;
        }
        return List.copyOf(out);
    }

    private int collectionSize(Object value) {
        if (value instanceof Collection<?> collection) return collection.size();
        return 0;
    }

    private int countPresent(String... values) {
        int count = 0;
        for (String value : values) {
            if (value != null && !value.isBlank()) count++;
        }
        return count;
    }

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank()) return false;
        String haystack = text.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (needle != null && haystack.contains(needle.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String stringValue(Object value) {
        return value == null ? null : sanitizeString(String.valueOf(value));
    }

    public record GovernedRequest(
            IARequest request,
            ApiVersion effectiveVersion,
            String effectiveCapability,
            Map<String, Object> governance,
            Map<String, Object> knowledgeCadence,
            Map<String, Object> toolPolicy
    ) {
        public GovernedRequest {
            Objects.requireNonNull(request, "request");
            effectiveVersion = effectiveVersion == null ? ApiVersion.latest() : effectiveVersion;
            effectiveCapability = effectiveCapability == null ? "LEGAL_GENERAL_ASSIST_V3" : effectiveCapability;
            governance = governance == null ? Map.of() : immutableMap(governance);
            knowledgeCadence = knowledgeCadence == null ? Map.of() : immutableMap(knowledgeCadence);
            toolPolicy = immutableMap(toolPolicy);
        }
    }

    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) continue;
            out.put(entry.getKey(), entry.getValue());
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private record RagPolicy(
            String profile,
            int topK,
            int evidenceBudget,
            int minEvidence,
            String distillationProfile,
            boolean strictIsolation,
            boolean petitionAware
    ) {
        private Map<String, Object> toMap() {
            return Map.of(
                    "profile", profile,
                    "topK", topK,
                    "evidenceBudget", evidenceBudget,
                    "minEvidence", minEvidence,
                    "distillationProfile", distillationProfile,
                    "strictIsolation", strictIsolation,
                    "petitionAware", petitionAware,
                    "refreshAt", Instant.now().toString()
            );
        }
    }

    private record ToolPolicy(
            String requestedMode,
            String effectiveMode,
            boolean readOnlyAllowed,
            boolean institutionalPrincipal,
            boolean petitionAware
    ) {
        private Map<String, Object> toMap() {
            return Map.of(
                    "requestedMode", requestedMode,
                    "effectiveMode", effectiveMode,
                    "readOnlyAllowed", readOnlyAllowed,
                    "institutionalPrincipal", institutionalPrincipal,
                    "petitionAware", petitionAware
            );
        }
    }
}
