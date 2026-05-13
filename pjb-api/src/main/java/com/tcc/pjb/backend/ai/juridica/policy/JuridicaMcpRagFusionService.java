package com.tcc.pjb.backend.ai.juridica.policy;

import com.tcc.pjb.backend.ai.juridica.policy.support.LegalAiPolicyTextCatalogService;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
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
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class JuridicaMcpRagFusionService {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9À-ÿ_./:-]+");

    private final LegalAiPolicyTextCatalogService policyTextCatalogService;

    public JuridicaMcpRagFusionService(LegalAiPolicyTextCatalogService policyTextCatalogService) {
        this.policyTextCatalogService = Objects.requireNonNull(policyTextCatalogService, "policyTextCatalogService");
    }

    public FusionPlan resolve(ResolveRequest request) {
        Objects.requireNonNull(request, "request");

        String capability = normalize(request.capability());
        ApiVersion version = request.version() == null ? ApiVersion.latest() : request.version();
        String ramo = normalize(request.ramo());
        String rito = normalize(request.rito());
        String tipoJustica = normalize(request.tipoJustica());
        String procedureFamily = normalize(request.procedureFamily());
        String petitionModel = normalize(request.petitionModel());
        String toolMode = normalize(stringValue(request.toolPolicy().get("effectiveMode")));
        boolean petitionDetected = request.petitionDetected();
        boolean strict = request.sigilo() || request.injectionRiskScore() >= 35 || petitionDetected;
        boolean highComplexity = request.complexityScore() >= 70;
        boolean highStakes = policyTextCatalogService.isHighStakesCapability(capability);
        boolean mcpAllowed = toolMode.contains("READ_ONLY") && !toolMode.contains("DISABLED");
        boolean toolSearchEnabled = mcpAllowed && (highStakes || petitionDetected || highComplexity || policyTextCatalogService.isReadHeavyCapability(capability));
        boolean remoteMcpEnabled = mcpAllowed && (request.tipoUsuario() == null || !request.tipoUsuario().isCidadaniaExterna());
        boolean deferredLoading = remoteMcpEnabled && !strict;
        boolean verificationLane = highStakes || strict || highComplexity;
        String fusionProfile = resolveFusionProfile(version, strict, petitionDetected, highComplexity, remoteMcpEnabled);
        String executionMode = resolveExecutionMode(strict, petitionDetected, highComplexity, remoteMcpEnabled);
        String reasoningEffort = resolveReasoningEffort(version, request.complexityScore(), strict, highStakes);
        String promptCacheRetention = resolvePromptCacheRetention(petitionDetected, highComplexity, strict);

        List<String> connectorFamilies = resolveConnectorFamilies(ramo, tipoJustica, procedureFamily, capability, remoteMcpEnabled);
        List<String> allowedToolClasses = resolveAllowedToolClasses(capability, petitionDetected, strict, remoteMcpEnabled);
        List<String> evidenceLanes = resolveEvidenceLanes(petitionDetected, toolSearchEnabled, remoteMcpEnabled);
        List<String> querySeeds = resolveQuerySeeds(request.knowledgeCadence(), connectorFamilies, ramo, rito, tipoJustica, procedureFamily, petitionModel, capability);
        List<String> verifierChecks = resolveVerifierChecks(request, connectorFamilies, strict, verificationLane);

        LinkedHashMap<String, Object> retrieval = new LinkedHashMap<>();
        retrieval.put("mode", executionMode);
        retrieval.put("vectorLane", true);
        retrieval.put("curriculumLane", true);
        retrieval.put("petitionBlueprintLane", petitionDetected);
        retrieval.put("toolSearchEnabled", toolSearchEnabled);
        retrieval.put("remoteMcpEnabled", remoteMcpEnabled);
        retrieval.put("deferLoading", deferredLoading);
        retrieval.put("connectorFamilies", connectorFamilies);
        retrieval.put("allowedToolClasses", allowedToolClasses);
        retrieval.put("evidenceLanes", evidenceLanes);
        retrieval.put("queryExpansionSeeds", querySeeds);
        retrieval.put("topKOverride", resolveTopK(version, request.complexityScore(), strict, remoteMcpEnabled));
        retrieval.put("evidenceBudgetOverride", resolveEvidenceBudget(version, petitionDetected, strict, remoteMcpEnabled));
        retrieval.put("minEvidenceOverride", resolveMinEvidence(version, petitionDetected, strict, remoteMcpEnabled));
        retrieval.put("requeryRounds", resolveRequeryRounds(highComplexity, remoteMcpEnabled, strict));
        retrieval.put("connectorSearchBudget", remoteMcpEnabled ? (strict ? 2 : 3) : 0);

        LinkedHashMap<String, Object> agentMesh = new LinkedHashMap<>();
        agentMesh.put("subagentsEnabled", verificationLane || toolSearchEnabled);
        agentMesh.put("planner", "LEGAL_STRATEGIST");
        agentMesh.put("retriever", toolSearchEnabled ? "RAG_RESEARCHER_WITH_TOOL_SEARCH" : "RAG_RESEARCHER");
        agentMesh.put("drafter", petitionDetected ? "PETITION_DRAFTER" : "LEGAL_RELATOR");
        agentMesh.put("verifier", verificationLane ? "LEGAL_VERIFIER_STRICT" : "LEGAL_VERIFIER_BALANCED");
        agentMesh.put("protocolGuard", petitionDetected ? "PETITION_PROTOCOL_GUARD" : "DISABLED");
        agentMesh.put("compactionProfile", strict ? "LEGAL_CONTEXT_COMPACTION_STRICT_V2" : "LEGAL_CONTEXT_COMPACTION_BALANCED_V2");

        LinkedHashMap<String, Object> mcp = new LinkedHashMap<>();
        mcp.put("enabled", remoteMcpEnabled);
        mcp.put("toolSearchEnabled", toolSearchEnabled);
        mcp.put("approvalMode", resolveApprovalMode(strict, remoteMcpEnabled, capability));
        mcp.put("allowReadOnly", mcpAllowed);
        mcp.put("allowMutatingTools", false);
        mcp.put("connectorFamilies", connectorFamilies);
        mcp.put("allowedToolClasses", allowedToolClasses);
        mcp.put("deferLoading", deferredLoading);
        mcp.put("sessionMode", resolveSessionMode(strict, petitionDetected, remoteMcpEnabled));
        mcp.put("toolSelectionProfile", remoteMcpEnabled ? (toolSearchEnabled ? "DISCOVERY_THEN_PIN" : "PINNED_ONLY") : "OFF");
        mcp.put("oauthEnvelope", remoteMcpEnabled ? "SERVER_MANAGED" : "NONE");

        LinkedHashMap<String, Object> promptCache = new LinkedHashMap<>();
        promptCache.put("retention", promptCacheRetention);
        promptCache.put("strategy", petitionDetected ? "LEGAL_DRAFT_PREFIX_CACHE" : "LEGAL_QUERY_PREFIX_CACHE");
        promptCache.put("cacheKeyProfile", buildPromptCacheProfile(capability, ramo, procedureFamily, petitionModel));
        promptCache.put("reasoningEffort", reasoningEffort);
        promptCache.put("streamingCompaction", strict ? "STRICT_SUMMARY_FENCE" : "BALANCED_SUMMARY_FENCE");

        LinkedHashMap<String, Object> continuousReading = new LinkedHashMap<>();
        continuousReading.put("profile", petitionDetected ? "LEGAL_READING_STREAM_DRAFT_V3" : "LEGAL_READING_STREAM_QUERY_V2");
        continuousReading.put("refreshCadenceHours", petitionDetected || highComplexity ? 6 : 24);
        continuousReading.put("curriculumFocus", extractCurriculumFocus(request.knowledgeCadence()));
        continuousReading.put("petitionFocus", extractPetitionFocus(request.knowledgeCadence(), procedureFamily, petitionModel));
        continuousReading.put("precedentWindows", resolvePrecedentWindows(ramo, tipoJustica, procedureFamily));
        continuousReading.put("checklistRefreshPolicy", petitionDetected ? "ON_EVERY_EDIT_AND_PROTOCOL_STEP" : "ON_MAJOR_CONTEXT_CHANGE");

        LinkedHashMap<String, Object> verification = new LinkedHashMap<>();
        verification.put("enabled", verificationLane);
        verification.put("checks", verifierChecks);
        verification.put("authorityFloor", strict ? "TRIBUNAL_SUPERIOR_OR_EQUIVALENT" : "TRIBUNAL_OR_CURRICULUM");
        verification.put("contradictionPolicy", strict ? "ESCALATE_FAST_ON_CONFLICT" : "TRY_RESOLVE_THEN_ESCALATE");
        verification.put("citationPolicy", highStakes || petitionDetected ? "MANDATORY_EVIDENCE_TRAIL" : "STRONG_PREFERENCE");

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("profile", fusionProfile);
        out.put("executionMode", executionMode);
        out.put("reasoningEffort", reasoningEffort);
        out.put("retrieval", immutableMap(retrieval));
        out.put("mcp", immutableMap(mcp));
        out.put("agentMesh", immutableMap(agentMesh));
        out.put("promptCache", immutableMap(promptCache));
        out.put("continuousReading", immutableMap(continuousReading));
        out.put("verification", immutableMap(verification));
        out.put("decisionReasons", resolveDecisionReasons(strict, petitionDetected, highComplexity, remoteMcpEnabled, toolSearchEnabled, capability));
        out.put("issuedAt", Instant.now().toString());

        return new FusionPlan(
                fusionProfile,
                executionMode,
                reasoningEffort,
                immutableMap(retrieval),
                immutableMap(mcp),
                immutableMap(agentMesh),
                immutableMap(promptCache),
                immutableMap(continuousReading),
                immutableMap(verification),
                immutableList(querySeeds),
                Collections.unmodifiableMap(out)
        );
    }

    private String resolveFusionProfile(ApiVersion version,
                                        boolean strict,
                                        boolean petitionDetected,
                                        boolean highComplexity,
                                        boolean remoteMcpEnabled) {
        if (!remoteMcpEnabled) {
            return strict
                    ? policyTextCatalogService.ragProfileStrictNoMcp()
                    : version.name() + policyTextCatalogService.ragProfileLocalOnlySuffix();
        }
        if (strict || petitionDetected) return policyTextCatalogService.ragProfileStagedStrict();
        if (highComplexity) return policyTextCatalogService.ragProfileHybridComplex();
        return policyTextCatalogService.ragProfileHybridBalanced();
    }

    private String resolveExecutionMode(boolean strict,
                                        boolean petitionDetected,
                                        boolean highComplexity,
                                        boolean remoteMcpEnabled) {
        if (!remoteMcpEnabled) return strict ? policyTextCatalogService.executionModeLocked() : policyTextCatalogService.executionModeLocal();
        if (strict || petitionDetected) return policyTextCatalogService.executionModeStagedReadOnly();
        if (highComplexity) return policyTextCatalogService.executionModeHybridReadOnly();
        return policyTextCatalogService.executionModeDeferredDiscovery();
    }

    private String resolveReasoningEffort(ApiVersion version,
                                          int complexityScore,
                                          boolean strict,
                                          boolean highStakes) {
        if (strict || highStakes || complexityScore >= 85) return policyTextCatalogService.reasoningEffortXhigh();
        if (version != null && version.isAtLeast(ApiVersion.V3) && complexityScore >= 60) return policyTextCatalogService.reasoningEffortHigh();
        if (complexityScore >= 35) return policyTextCatalogService.reasoningEffortMedium();
        return policyTextCatalogService.reasoningEffortLow();
    }

    private String resolvePromptCacheRetention(boolean petitionDetected,
                                               boolean highComplexity,
                                               boolean strict) {
        if (petitionDetected || highComplexity || strict) return policyTextCatalogService.promptCacheRetentionLong();
        return policyTextCatalogService.promptCacheRetentionSession();
    }

    private List<String> resolveConnectorFamilies(String ramo,
                                                  String tipoJustica,
                                                  String procedureFamily,
                                                  String capability,
                                                  boolean remoteMcpEnabled) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.addAll(policyTextCatalogService.connectorBaseFamilies());
        if (capability.contains("PETICAO") || capability.contains("PROTOCOLO")) out.add(policyTextCatalogService.connectorProtocolGuard());
        if (capability.contains("CALC") || ramo.contains("TRABALHISTA") || ramo.contains("TRIBUT") || ramo.contains("PREVID")) out.add(policyTextCatalogService.connectorCalculator());
        if (tipoJustica.contains("FEDERAL") || ramo.contains("PREVID")) out.add(policyTextCatalogService.connectorFederalRecords());
        if (ramo.contains("PENAL") || procedureFamily.contains("CUSTODIA") || procedureFamily.contains("JURI")) out.add(policyTextCatalogService.connectorCriminalLane());
        if (ramo.contains("ELEITORAL")) out.add(policyTextCatalogService.connectorElectoralLane());
        if (ramo.contains("TRABALHISTA")) out.add(policyTextCatalogService.connectorLaborLane());
        if (ramo.contains("AMBIENTAL")) out.add(policyTextCatalogService.connectorEnvironmentalLane());
        if (ramo.contains("EMPRESARIAL")) out.add(policyTextCatalogService.connectorBusinessLane());
        if (remoteMcpEnabled) {
            out.addAll(policyTextCatalogService.connectorRemoteFamilies());
        }
        return List.copyOf(out);
    }

    private List<String> resolveAllowedToolClasses(String capability,
                                                   boolean petitionDetected,
                                                   boolean strict,
                                                   boolean remoteMcpEnabled) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.addAll(policyTextCatalogService.allowedToolClassBase());
        if (capability.contains("PROTOCOLO") || petitionDetected) out.add(policyTextCatalogService.allowedToolClassProtocolPrecheck());
        if (capability.contains("COMPETENCIA")) out.add(policyTextCatalogService.allowedToolClassCompetenceRouting());
        if (capability.contains("CALC")) out.add(policyTextCatalogService.allowedToolClassCalculatorLookup());
        if (strict || !remoteMcpEnabled) {
            out.remove(policyTextCatalogService.allowedToolClassProtocolPrecheck());
        }
        return List.copyOf(out);
    }

    private List<String> resolveEvidenceLanes(boolean petitionDetected,
                                              boolean toolSearchEnabled,
                                              boolean remoteMcpEnabled) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.addAll(policyTextCatalogService.evidenceLaneBase());
        if (petitionDetected) out.add(policyTextCatalogService.evidenceLanePetitionBlueprints());
        if (toolSearchEnabled) out.add(policyTextCatalogService.evidenceLaneToolSearch());
        if (remoteMcpEnabled) out.add(policyTextCatalogService.evidenceLaneRemoteReadonly());
        return List.copyOf(out);
    }

    private List<String> resolveQuerySeeds(Map<String, Object> knowledgeCadence,
                                           List<String> connectorFamilies,
                                           String ramo,
                                           String rito,
                                           String tipoJustica,
                                           String procedureFamily,
                                           String petitionModel,
                                           String capability) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        append(out, ramo, rito, tipoJustica, procedureFamily, petitionModel, capability);
        if (knowledgeCadence != null) {
            Object curriculumObj = knowledgeCadence.get("curriculum");
            if (curriculumObj instanceof Map<?, ?> curriculumMap) {
                append(out, stringValue(curriculumMap.get("ramoCodigo")));
                appendAll(out, curriculumMap.get("materiasPrioritarias"));
                appendAll(out, curriculumMap.get("legislacaoChave"));
            }
            Object petitionObj = knowledgeCadence.get("petitionBlueprint");
            if (petitionObj instanceof Map<?, ?> petitionMap) {
                append(out, stringValue(petitionMap.get("procedureFamily")), stringValue(petitionMap.get("recommendedModelCode")));
                appendAll(out, petitionMap.get("requiredDocuments"));
            }
            appendAll(out, knowledgeCadence.get("queryExpansionSeeds"));
        }
        for (String connectorFamily : connectorFamilies) {
            append(out, connectorFamily);
        }
        return out.stream().limit(24).toList();
    }

    private List<String> resolveVerifierChecks(ResolveRequest request,
                                               List<String> connectorFamilies,
                                               boolean strict,
                                               boolean verificationLane) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.addAll(policyTextCatalogService.verifierCheckBase());
        if (verificationLane) out.add(policyTextCatalogService.verifierCheckContradictionResolution());
        if (strict || request.sigilo()) out.add(policyTextCatalogService.verifierCheckPromptInjectionFence());
        if (request.petitionDetected()) out.add(policyTextCatalogService.verifierCheckPetitionStructure());
        if (connectorFamilies.contains(policyTextCatalogService.connectorProtocolGuard())) out.add(policyTextCatalogService.verifierCheckProtocolPrecheckAlignment());
        if (connectorFamilies.contains("PJB_COMPETENCE_MATRIX")) out.add(policyTextCatalogService.verifierCheckCompetenceRoutingAlignment());
        return List.copyOf(out);
    }

    private int resolveTopK(ApiVersion version,
                            int complexityScore,
                            boolean strict,
                            boolean remoteMcpEnabled) {
        int base = version != null && version.isAtLeast(ApiVersion.V3) ? 12 : 8;
        if (complexityScore >= 70) base += 4;
        if (strict) base += 2;
        if (remoteMcpEnabled) base += 2;
        return Math.max(4, Math.min(base, 24));
    }

    private int resolveEvidenceBudget(ApiVersion version,
                                      boolean petitionDetected,
                                      boolean strict,
                                      boolean remoteMcpEnabled) {
        int base = version != null && version.isAtLeast(ApiVersion.V3) ? 10 : 8;
        if (petitionDetected) base += 2;
        if (strict) base += 2;
        if (remoteMcpEnabled) base += 1;
        return Math.max(4, Math.min(base, 18));
    }

    private int resolveMinEvidence(ApiVersion version,
                                   boolean petitionDetected,
                                   boolean strict,
                                   boolean remoteMcpEnabled) {
        int base = version != null && version.isAtLeast(ApiVersion.V3) ? 3 : 2;
        if (petitionDetected || strict) base += 1;
        if (remoteMcpEnabled) base += 1;
        return Math.max(2, Math.min(base, 6));
    }

    private int resolveRequeryRounds(boolean highComplexity,
                                     boolean remoteMcpEnabled,
                                     boolean strict) {
        if (!remoteMcpEnabled) return strict ? 1 : 0;
        if (highComplexity || strict) return 2;
        return 1;
    }

    private String resolveApprovalMode(boolean strict,
                                       boolean remoteMcpEnabled,
                                       String capability) {
        if (!remoteMcpEnabled) return policyTextCatalogService.approvalModeOff();
        if (strict || capability.contains("PROTOCOLO")) return policyTextCatalogService.approvalModeAlways();
        return policyTextCatalogService.approvalModeReadOnlyAuto();
    }

    private String resolveSessionMode(boolean strict,
                                      boolean petitionDetected,
                                      boolean remoteMcpEnabled) {
        if (!remoteMcpEnabled) return policyTextCatalogService.sessionModeLocalState();
        if (strict || petitionDetected) return policyTextCatalogService.sessionModePinnedServer();
        return policyTextCatalogService.sessionModeDeferredServer();
    }

    private List<String> extractCurriculumFocus(Map<String, Object> knowledgeCadence) {
        if (knowledgeCadence == null) return List.of();
        Object curriculumObj = knowledgeCadence.get("curriculum");
        if (!(curriculumObj instanceof Map<?, ?> curriculumMap)) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        append(out, stringValue(curriculumMap.get("ramoCodigo")));
        appendAll(out, curriculumMap.get("materiasPrioritarias"));
        appendAll(out, curriculumMap.get("legislacaoChave"));
        appendAll(out, curriculumMap.get("prazosCriticos"));
        return out.stream().limit(18).toList();
    }

    private List<String> extractPetitionFocus(Map<String, Object> knowledgeCadence,
                                              String procedureFamily,
                                              String petitionModel) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        append(out, procedureFamily, petitionModel);
        if (knowledgeCadence != null) {
            Object petitionObj = knowledgeCadence.get("petitionBlueprint");
            if (petitionObj instanceof Map<?, ?> petitionMap) {
                append(out, stringValue(petitionMap.get("procedureFamily")), stringValue(petitionMap.get("recommendedModelCode")));
                appendAll(out, petitionMap.get("requiredDocuments"));
                appendAll(out, petitionMap.get("modelCatalog"));
                appendAll(out, petitionMap.get("questionBlocks"));
            }
        }
        return out.stream().limit(20).toList();
    }

    private List<String> resolvePrecedentWindows(String ramo,
                                                 String tipoJustica,
                                                 String procedureFamily) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.addAll(policyTextCatalogService.precedentWindowBase());
        if (tipoJustica.contains("TRABALH") || ramo.contains("TRABALHISTA")) out.add(policyTextCatalogService.precedentWindowLabor());
        if (tipoJustica.contains("ELEITORAL") || ramo.contains("ELEITORAL")) out.add(policyTextCatalogService.precedentWindowElectoral());
        if (tipoJustica.contains("MILITAR") || ramo.contains("MILITAR")) out.add(policyTextCatalogService.precedentWindowMilitary());
        if (procedureFamily.contains("JURI") || ramo.contains("PENAL")) out.add(policyTextCatalogService.precedentWindowCriminal());
        if (tipoJustica.contains("FEDERAL") || ramo.contains("PREVID")) out.add(policyTextCatalogService.precedentWindowFederal());
        return out.stream().limit(8).toList();
    }

    private String buildPromptCacheProfile(String capability,
                                           String ramo,
                                           String procedureFamily,
                                           String petitionModel) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        append(parts, capability, ramo, procedureFamily, petitionModel);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String part : parts) {
            if (count > 0) sb.append(':');
            sb.append(part);
            count++;
            if (count >= 4) break;
        }
        String value = sb.toString();
        return value.isBlank() ? policyTextCatalogService.generalPromptCacheProfile() : value;
    }

    private List<String> resolveDecisionReasons(boolean strict,
                                                boolean petitionDetected,
                                                boolean highComplexity,
                                                boolean remoteMcpEnabled,
                                                boolean toolSearchEnabled,
                                                String capability) {
        ArrayList<String> out = new ArrayList<>();
        out.add(policyTextCatalogService.decisionReasonCapabilityPrefix() + capability);
        if (strict) out.add(policyTextCatalogService.decisionReasonStrictGuardrails());
        if (petitionDetected) out.add(policyTextCatalogService.decisionReasonPetitionGroundedPipeline());
        if (highComplexity) out.add(policyTextCatalogService.decisionReasonComplexMultilane());
        if (remoteMcpEnabled) out.add(policyTextCatalogService.decisionReasonReadonlyMcpAllowed());
        if (toolSearchEnabled) out.add(policyTextCatalogService.decisionReasonToolSearchEnabled());
        if (!remoteMcpEnabled) out.add(policyTextCatalogService.decisionReasonLocalOnlyFallback());
        return List.copyOf(out);
    }

    private void append(LinkedHashSet<String> target, String... values) {
        if (values == null) return;
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized.isBlank()) continue;
            target.add(normalized);
        }
    }

    private void appendAll(LinkedHashSet<String> target, Object value) {
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                append(target, stringValue(item));
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalize(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.isBlank()) return "";
        String[] tokens = TOKEN_SPLIT.split(text);
        ArrayList<String> safe = new ArrayList<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) continue;
            safe.add(token.toUpperCase(Locale.ROOT));
            if (safe.size() >= 8) break;
        }
        return String.join("_", safe);
    }

    public record ResolveRequest(
            String capability,
            ApiVersion version,
            String ramo,
            String rito,
            String tipoJustica,
            String procedureFamily,
            String petitionModel,
            TipoUsuario tipoUsuario,
            int complexityScore,
            int injectionRiskScore,
            boolean sigilo,
            boolean petitionDetected,
            Map<String, Object> payload,
            Map<String, Object> toolPolicy,
            Map<String, Object> knowledgeCadence
    ) {
        public ResolveRequest {
            version = version == null ? ApiVersion.latest() : version;
            payload = payload == null ? Map.of() : Collections.unmodifiableMap(payload);
            toolPolicy = toolPolicy == null ? Map.of() : immutableMap(toolPolicy);
            knowledgeCadence = knowledgeCadence == null ? Map.of() : immutableMap(knowledgeCadence);
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

    private static List<String> immutableList(Collection<String> source) {
        if (source == null || source.isEmpty()) return List.of();
        ArrayList<String> out = new ArrayList<>();
        for (String item : source) {
            if (item == null || item.isBlank()) continue;
            out.add(item);
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    public record FusionPlan(
            String profile,
            String executionMode,
            String reasoningEffort,
            Map<String, Object> retrieval,
            Map<String, Object> mcp,
            Map<String, Object> agentMesh,
            Map<String, Object> promptCache,
            Map<String, Object> continuousReading,
            Map<String, Object> verification,
            List<String> queryExpansionSeeds,
            Map<String, Object> asMap
    ) {
        public FusionPlan {
            profile = profile == null ? "JURIDICA_RAG_LOCAL_ONLY" : profile;
            executionMode = executionMode == null ? "RAG_ONLY_LOCAL" : executionMode;
            reasoningEffort = reasoningEffort == null ? "medium" : reasoningEffort;
            retrieval = retrieval == null ? Map.of() : immutableMap(retrieval);
            mcp = mcp == null ? Map.of() : immutableMap(mcp);
            agentMesh = agentMesh == null ? Map.of() : immutableMap(agentMesh);
            promptCache = promptCache == null ? Map.of() : immutableMap(promptCache);
            continuousReading = continuousReading == null ? Map.of() : immutableMap(continuousReading);
            verification = verification == null ? Map.of() : immutableMap(verification);
            queryExpansionSeeds = immutableList(queryExpansionSeeds);
            asMap = asMap == null ? Map.of() : immutableMap(asMap);
        }
    }
}
