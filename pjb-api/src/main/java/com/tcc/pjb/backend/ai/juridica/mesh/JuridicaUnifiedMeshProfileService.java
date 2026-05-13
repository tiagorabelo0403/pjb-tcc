package com.tcc.pjb.backend.ai.juridica.mesh;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.juridica.mcp.JuridicaMcpServerCatalogService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiMeshProfileResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaUnifiedMeshProfileService {

    private final JuridicaLegalToolCatalogService toolCatalogService;
    private final JuridicaMcpServerCatalogService mcpServerCatalogService;
    private final LegalAiStructuredSchemaCatalog structuredSchemaCatalog;

    public JuridicaUnifiedMeshProfileService(JuridicaLegalToolCatalogService toolCatalogService,
                                             JuridicaMcpServerCatalogService mcpServerCatalogService,
                                             LegalAiStructuredSchemaCatalog structuredSchemaCatalog) {
        this.toolCatalogService = Objects.requireNonNull(toolCatalogService, "toolCatalogService");
        this.mcpServerCatalogService = Objects.requireNonNull(mcpServerCatalogService, "mcpServerCatalogService");
        this.structuredSchemaCatalog = Objects.requireNonNull(structuredSchemaCatalog, "structuredSchemaCatalog");
    }

    public LegalAiMeshProfileResponse resolveForIa(IARequest request,
                                                   ApiVersion version,
                                                   String capability,
                                                   Map<String, Object> governance,
                                                   Map<String, Object> knowledgeCadence,
                                                   Map<String, Object> toolPolicy) {
        Map<String, Object> payload = request != null && request.getPayload() != null ? request.getPayload() : Map.of();
        return resolve(version, capability, payload, governance, knowledgeCadence, toolPolicy);
    }

    public LegalAiMeshProfileResponse resolveForSkill(String skill,
                                                      ApiVersion version,
                                                      Map<String, Object> payload,
                                                      Map<String, Object> context) {
        LinkedHashMap<String, Object> governance = new LinkedHashMap<>();
        governance.put("profile", profileCode(skill, payload));
        governance.put("source", "LEGAL_SKILLS");
        if (context != null && !context.isEmpty()) {
            governance.put("contextKeys", context.keySet().stream().sorted().toList());
        }
        return resolve(version, skill, payload, governance, Map.of(), Map.of("effectiveMode", "READ_ONLY_SKILL_CONTEXT"));
    }

    public LegalAiMeshProfileResponse resolveForSurface(String capability) {
        return resolveForSurface(capability, ApiVersion.latest());
    }

    public LegalAiMeshProfileResponse resolveForSurface(String capability, ApiVersion version) {
        return resolve(version, capability, Map.of(), Map.of("profile", JuridicaMeshLabels.PROFILE_BALANCED), Map.of(), Map.of("effectiveMode", "READ_ONLY"));
    }

    private LegalAiMeshProfileResponse resolve(ApiVersion version,
                                               String capability,
                                               Map<String, Object> payload,
                                               Map<String, Object> governance,
                                               Map<String, Object> knowledgeCadence,
                                               Map<String, Object> toolPolicy) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        String normalizedCapability = capability == null || capability.isBlank() ? "LEGAL_GENERAL_ASSIST_V3" : capability.trim().toUpperCase(Locale.ROOT);
        boolean petitionDetected = detectPetition(payload, normalizedCapability);
        boolean strict = detectStrict(payload, governance, normalizedCapability);
        List<LegalAiToolDescriptor> tools = toolCatalogService.resolve(normalizedCapability, effectiveVersion, strict, petitionDetected);
        var recommendedSchema = structuredSchemaCatalog.recommend(effectiveVersion, normalizedCapability, syntheticConversationRequest(payload));
        var mcpPlan = mcpServerCatalogService.resolvePlan(effectiveVersion, normalizedCapability, payload, tools);

        LinkedHashMap<String, Object> rag = new LinkedHashMap<>();
        rag.put("mode", JuridicaMeshLabels.RAG_MODE);
        rag.put("lexicalSearch", true);
        rag.put("denseSearch", true);
        rag.put("crossEncoderRerank", true);
        rag.put("hierarchicalMerge", true);
        rag.put("authorityAware", true);
        rag.put("temporalAware", true);
        rag.put("taxonomyAware", true);
        rag.put("knowledgeCadence", knowledgeCadence == null ? Map.of() : immutableMap(knowledgeCadence));

        LinkedHashMap<String, Object> mcp = new LinkedHashMap<>();
        mcp.put("sessionMode", JuridicaMeshLabels.MCP_SESSION_MODE);
        mcp.put("serverIds", mcpPlan.pinnedServers().stream().map(server -> server.serverId()).toList());
        mcp.put("servers", mcpPlan.pinnedServers().stream().map(server -> server.asMap()).toList());
        mcp.put("fallbackServers", mcpPlan.fallbackServers().stream().map(server -> server.asMap()).toList());
        mcp.put("readOnlyDefault", true);
        mcp.put("mutatingToolsBlockedByDefault", true);
        mcp.put("toolCount", tools.size());
        mcp.put("effectiveMode", toolPolicy == null ? "READ_ONLY" : String.valueOf(toolPolicy.getOrDefault("effectiveMode", "READ_ONLY")));
        mcp.put("selectionMode", mcpPlan.selectionMode());
        mcp.put("authorizationProfile", mcpPlan.authorizationProfile());
        mcp.put("transportProfile", mcpPlan.transportProfile());
        mcp.put("batchingStrategy", mcpPlan.batchingStrategy());
        mcp.put("completionStrategy", mcpPlan.completionStrategy());
        mcp.put("trustMode", mcpPlan.trustMode());
        mcp.put("evidenceBudget", mcpPlan.evidenceBudget());
        mcp.put("benchmarkSuiteId", mcpPlan.evaluation() == null ? null : mcpPlan.evaluation().suiteId());
        mcp.put("benchmarkPassed", mcpPlan.evaluation() != null && mcpPlan.evaluation().passed());
        mcp.put("qualityScore", mcpPlan.evaluation() == null ? null : mcpPlan.evaluation().qualityScore());
        mcp.put("promotionCandidates", mcpPlan.evaluation() == null ? List.of() : mcpPlan.evaluation().promotionCandidates());
        mcp.put("demotionCandidates", mcpPlan.evaluation() == null ? List.of() : mcpPlan.evaluation().demotionCandidates());
        mcp.put("adaptationHints", mcpPlan.evaluation() == null ? Map.of() : mcpPlan.evaluation().adaptationHints());
        mcp.put("skillIds", mcpPlan.pinnedSkills() == null ? List.of() : mcpPlan.pinnedSkills().stream().map(skill -> skill.skillId()).toList());
        mcp.put("toolExampleIds", mcpPlan.pinnedToolExamples() == null ? List.of() : mcpPlan.pinnedToolExamples().stream().map(example -> example.exampleId()).toList());
        mcp.put("deliberation", mcpPlan.deliberation() == null ? Map.of() : mcpPlan.deliberation().asMap());
        mcp.put("contextCompaction", mcpPlan.contextCompaction() == null ? Map.of() : mcpPlan.contextCompaction().asMap());
        mcp.put("transcriptId", mcpPlan.transcript() == null ? null : mcpPlan.transcript().transcriptId());
        mcp.put("transcriptReplayReady", mcpPlan.transcript() != null && mcpPlan.transcript().replayReady());
        mcp.put("doctorStatus", mcpPlan.doctor() == null ? null : mcpPlan.doctor().status());
        mcp.put("doctorReady", mcpPlan.doctor() != null && mcpPlan.doctor().ready());
        mcp.put("doctorOperationalMode", mcpPlan.doctor() == null ? null : mcpPlan.doctor().operationalMode());
        mcp.put("evidencePromotionStatus", mcpPlan.evidencePromotion() == null ? null : mcpPlan.evidencePromotion().status());
        mcp.put("promotedToolExampleIds", mcpPlan.evidencePromotion() == null ? List.of() : mcpPlan.evidencePromotion().promotedToolExampleIds());
        mcp.put("evidenceApprovalLane", mcpPlan.evidencePromotion() == null ? null : mcpPlan.evidencePromotion().approvalLane());
        mcp.put("evidenceScore", mcpPlan.evidencePromotion() == null ? null : mcpPlan.evidencePromotion().evidenceScore());
        mcp.put("plan", mcpPlan.asMap());

        LinkedHashMap<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("virtualThreadSpine", "PjbVirtualThreadSpine");
        runtime.put("executionGovernance", "PjbExecutionOrchestrator");
        runtime.put("runtimePolicy", JuridicaMeshLabels.RUNTIME_EXECUTION_GOVERNANCE);
        runtime.put("virtualThreadPolicy", JuridicaMeshLabels.RUNTIME_VIRTUAL_THREAD_SPINE);
        runtime.put("allowAdHocExecutors", false);
        runtime.put("allowParallelSchedulers", false);

        LinkedHashMap<String, Object> versions = new LinkedHashMap<>();
        versions.put("v1", Map.of(
                "role", "ADMISSIBILIDADE_E_SUFICIENCIA",
                "filter", "LEGAL_INPUT_QUALITY_FILTER",
                "juridicalDepth", "FOUNDATIONAL"
        ));
        versions.put("v2", Map.of(
                "role", "RITO_PROVA_E_PLANO_PROCESSUAL",
                "filter", "PROCEDURAL_COMPATIBILITY_FILTER",
                "juridicalDepth", "STRUCTURED"
        ));
        versions.put("v3", Map.of(
                "role", "CONSOLIDACAO_HERMENEUTICA_E_CITATION_FIRST",
                "filter", "HERMENEUTIC_AND_CONTRADICTION_FILTER",
                "juridicalDepth", "DEEP"
        ));

        LinkedHashMap<String, Object> legalDepth = new LinkedHashMap<>();
        legalDepth.put("sources", JuridicaMeshLabels.legalDepthSources());
        legalDepth.put("citationFirst", true);
        legalDepth.put("precedentWindows", true);
        legalDepth.put("proceduralOntology", true);
        legalDepth.put("legacyAware", true);
        legalDepth.put("documentAuthenticityAware", true);
        legalDepth.put("recommendedSchemaId", recommendedSchema == null ? null : recommendedSchema.schemaId());

        return new LegalAiMeshProfileResponse(
                profileCode(normalizedCapability, payload),
                effectiveVersion.name(),
                normalizedCapability,
                JuridicaMeshLabels.qualityFilters(),
                JuridicaMeshLabels.memoryScopes(),
                safeMap(rag),
                safeMap(mcp),
                safeMap(runtime),
                safeMap(versions),
                safeMap(legalDepth),
                tools
        );
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

    private boolean detectPetition(Map<String, Object> payload, String capability) {
        String normalized = capability == null ? "" : capability.toUpperCase(Locale.ROOT);
        if (normalized.contains("PETICAO") || normalized.contains("MINUTA") || normalized.contains("PROTOCOLO")) {

            return true;
        }
        Object petition = payload == null ? null : payload.get("peticaoDetectada");
        if (petition instanceof Boolean detected) {
            return detected;
        }
        Object text = payload == null ? null : payload.get("textoPeticaoLivre");
        return text instanceof String value && !value.isBlank();
    }

    private Map<String, Object> safeMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>(values);
        safe.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return java.util.Collections.unmodifiableMap(safe);
    }

    private boolean detectStrict(Map<String, Object> payload, Map<String, Object> governance, String capability) {
        if (payload != null) {
            Object sigilo = payload.get("sigilo");
            if (sigilo instanceof Boolean strict && strict) {
                return true;
            }
        }
        if (governance != null) {
            Object complexity = governance.get("complexityScore");
            if (complexity instanceof Number number && number.intValue() >= 70) {
                return true;
            }
            Object risk = governance.get("injectionRiskScore");
            if (risk instanceof Number number && number.intValue() >= 35) {
                return true;
            }
        }
        String normalized = capability == null ? "" : capability.toUpperCase(Locale.ROOT);
        return normalized.contains("PROTOCOLO") || normalized.contains("SENTENCA") || normalized.contains("PARECER");
    }

    private LegalAiConversationRequest syntheticConversationRequest(Map<String, Object> payload) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        return new LegalAiConversationRequest(
                null,
                stringValue(firstPresent(safePayload, "processoId", "numeroProcesso")),
                stringValue(firstPresent(safePayload, "message", "pergunta")),
                stringValue(safePayload.get("userProfile")),
                listValue(safePayload.get("history")),
                listValue(firstPresent(safePayload, "attachments", "documentosAnexados")),
                safePayload
        );
    }

    private Object firstPresent(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (payload.containsKey(key) && payload.get(key) != null) {
                return payload.get(key);
            }
        }
        return null;
    }

    private List<String> listValue(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        if (value == null) {
            return List.of();
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? List.of() : List.of(text);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String profileCode(String capability, Map<String, Object> payload) {
        boolean petitionDetected = detectPetition(payload, capability);
        boolean strict = detectStrict(payload, Map.of(), capability);
        if (petitionDetected) {
            return JuridicaMeshLabels.PROFILE_PROTOCOL;
        }
        if (strict) {
            return JuridicaMeshLabels.PROFILE_STRICT;
        }
        return JuridicaMeshLabels.PROFILE_BALANCED;
    }
}
