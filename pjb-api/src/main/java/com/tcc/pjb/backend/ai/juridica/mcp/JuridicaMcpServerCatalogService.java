package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.ai.juridica.eval.LegalEvalReplayRunner;
import com.tcc.pjb.backend.ai.juridica.mcp.support.LegalMcpTextCatalogService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JuridicaMcpServerCatalogService {

    private final List<LegalMcpServerProfile> servers;
    private final LegalAiStructuredSchemaCatalog structuredSchemaCatalog;
    private final LegalEvalReplayRunner evalReplayRunner;
    private final LegalMcpSkillCatalogService skillCatalogService;
    private final LegalMcpToolExampleRegistry toolExampleRegistry;
    private final LegalMcpDeliberationCheckpointService deliberationCheckpointService;
    private final LegalMcpContextCompactionService contextCompactionService;
    private final LegalMcpExecutionTranscriptService executionTranscriptService;
    private final LegalMcpDoctorService doctorService;
    private final LegalMcpEvidencePromotionService evidencePromotionService;
    private final LegalMcpTextCatalogService textCatalogService;

    public JuridicaMcpServerCatalogService(List<LegalMcpServerProfile> servers,
                                           LegalAiStructuredSchemaCatalog structuredSchemaCatalog,
                                           LegalEvalReplayRunner evalReplayRunner,
                                           LegalMcpSkillCatalogService skillCatalogService,
                                           LegalMcpToolExampleRegistry toolExampleRegistry,
                                           LegalMcpDeliberationCheckpointService deliberationCheckpointService,
                                           LegalMcpContextCompactionService contextCompactionService,
                                           LegalMcpExecutionTranscriptService executionTranscriptService,
                                           LegalMcpDoctorService doctorService,
                                           LegalMcpEvidencePromotionService evidencePromotionService,
                                           LegalMcpTextCatalogService textCatalogService) {
        this.servers = List.copyOf(Objects.requireNonNull(servers, "servers"));
        this.structuredSchemaCatalog = Objects.requireNonNull(structuredSchemaCatalog, "structuredSchemaCatalog");
        this.evalReplayRunner = Objects.requireNonNull(evalReplayRunner, "evalReplayRunner");
        this.skillCatalogService = Objects.requireNonNull(skillCatalogService, "skillCatalogService");
        this.toolExampleRegistry = Objects.requireNonNull(toolExampleRegistry, "toolExampleRegistry");
        this.deliberationCheckpointService = Objects.requireNonNull(deliberationCheckpointService, "deliberationCheckpointService");
        this.contextCompactionService = Objects.requireNonNull(contextCompactionService, "contextCompactionService");
        this.executionTranscriptService = Objects.requireNonNull(executionTranscriptService, "executionTranscriptService");
        this.doctorService = Objects.requireNonNull(doctorService, "doctorService");
        this.evidencePromotionService = Objects.requireNonNull(evidencePromotionService, "evidencePromotionService");
        this.textCatalogService = Objects.requireNonNull(textCatalogService, "textCatalogService");
    }

    public List<LegalMcpServerDescriptor> resolveDescriptors() {
        return servers.stream().map(LegalMcpServerProfile::descriptor).toList();
    }

    public LegalMcpExecutionPlan resolvePlan(ApiVersion version,
                                             String capability,
                                             Map<String, Object> payload,
                                             List<LegalAiToolDescriptor> routedTools) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        var syntheticRequest = new LegalAiConversationRequest(
                null,
                stringValue(safePayload.get("processoId")),
                stringValue(safePayload.get("message")),
                stringValue(safePayload.get("userProfile")),
                listValue(safePayload.get("history")),
                listValue(safePayload.get("attachments")),
                safePayload
        );
        var recommendedSchema = structuredSchemaCatalog.recommend(effectiveVersion, capability, syntheticRequest);
        return resolvePlan(
                new LegalMcpServerProfile.ResolveRequest(
                        normalize(capability),
                        effectiveVersion,
                        stringValue(safePayload.get("userProfile")),
                        stringValue(firstPresent(safePayload, "ramo", "ramoDireito")),
                        stringValue(firstPresent(safePayload, "rito", "ritoProcessual")),
                        stringValue(firstPresent(safePayload, "processoId", "numeroProcesso")),
                        isStrictSigilo(firstPresent(safePayload, "sigilo", "isSigiloso")),
                        riskFlag(safePayload),
                        quarantinedFlag(safePayload),
                        listValue(firstPresent(safePayload, "attachments", "documentosAnexados")),
                        listValue(safePayload.get("history")),
                        routedTools == null ? List.of() : List.copyOf(routedTools),
                        recommendedSchema,
                        safePayload
                )
        );
    }

    public LegalMcpExecutionPlan resolvePlan(LegalMcpServerProfile.ResolveRequest request) {
        List<ServerCandidate> ranked = servers.stream()
                .map(server -> new ServerCandidate(server.descriptor(), server.score(request)))
                .sorted(Comparator.comparingInt(ServerCandidate::score).reversed())
                .toList();
        int serverBudget = resolveServerBudget(request);
        List<LegalMcpServerDescriptor> pinned = ranked.stream()
                .filter(candidate -> candidate.score() > 0)
                .limit(serverBudget)
                .map(ServerCandidate::descriptor)
                .toList();
        List<LegalMcpServerDescriptor> fallback = ranked.stream()
                .filter(candidate -> candidate.score() > 0)
                .skip(Math.min(serverBudget, ranked.size()))
                .limit(2)
                .map(ServerCandidate::descriptor)
                .toList();
        if (pinned.isEmpty()) {
            pinned = servers.stream().map(LegalMcpServerProfile::descriptor)
                    .filter(descriptor -> descriptor.serverId().equals("MCP_LEGISLACAO") || descriptor.serverId().equals("MCP_PROCESSUAL"))
                    .toList();
        }
        LinkedHashSet<String> pinnedToolIds = new LinkedHashSet<>();
        pinned.forEach(server -> server.tools().forEach(tool -> pinnedToolIds.add(tool.toolId())));
        String selectionMode = selectionMode(request, pinned);
        String transportProfile = pinned.stream().allMatch(server -> server.transportMode().contains("STREAMABLE_HTTP"))
                ? "STREAMABLE_HTTP_BATCHED_READONLY"
                : "HYBRID_GOVERNED_TRANSPORT";
        String authorizationProfile = pinned.stream().allMatch(server -> server.authorizationMode().contains("OPTIONAL"))
                ? "PUBLIC_READONLY_OR_OPTIONAL_OAUTH"
                : "OAUTH2_1_GOVERNED";
        String batchingStrategy = pinned.size() > 1 ? "JSON_RPC_BATCH_SAFE_READONLY" : "SINGLE_SERVER_PIN";
        String completionStrategy = pinned.stream().anyMatch(LegalMcpServerDescriptor::completionsEnabled)
                ? "ARGUMENT_COMPLETIONS_AND_PINNED_PROMPTS"
                : "PROMPT_PIN_ONLY";
        List<String> reasons = decisionReasons(request, pinned);
        List<String> safeguards = safeguards(request, pinned);
        int evidenceBudget = resolveEvidenceBudget(request);
        var preliminaryPlan = new LegalMcpExecutionPlan(
                "LEGAL_MCP_PLAN_" + UUID.randomUUID(),
                selectionMode,
                !request.sigilo() && !request.promptInjectionDetected(),
                transportProfile,
                authorizationProfile,
                batchingStrategy,
                completionStrategy,
                request.sigilo() ? "SIGNED_CONTEXT_AND_AUTHORITY_FROZEN" : "READONLY_CHAINED_EVIDENCE",
                evidenceBudget,
                serverBudget,
                List.copyOf(pinned),
                List.copyOf(fallback),
                List.copyOf(pinnedToolIds),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                List.copyOf(reasons),
                List.copyOf(safeguards),
                null
        );
        var evaluation = evalReplayRunner.run(request, preliminaryPlan);
        var pinnedSkills = skillCatalogService.resolve(request.version(), request, evaluation, pinned);
        var pinnedToolExamples = toolExampleRegistry.resolve(request, pinnedSkills, pinned);
        var deliberation = deliberationCheckpointService.resolve(request, evaluation, pinnedSkills, pinnedToolExamples);
        var contextCompaction = contextCompactionService.resolve(request, evaluation);
        var transcript = executionTranscriptService.capture(request, preliminaryPlan, evaluation, pinnedSkills, pinnedToolExamples, deliberation);
        var doctor = doctorService.inspect(request, preliminaryPlan, evaluation, deliberation, contextCompaction);
        var evidencePromotion = evidencePromotionService.resolve(request, evaluation, transcript, doctor, pinnedToolExamples);
        List<String> finalReasons = new ArrayList<>(reasons);
        finalReasons.add(textCatalogService.decisionReasonSkillsPrefix() + pinnedSkills.stream().map(skill -> skill.skillId()).toList());
        finalReasons.add(textCatalogService.decisionReasonToolExamplesPrefix() + pinnedToolExamples.stream().map(example -> example.exampleId()).toList());
        finalReasons.add(textCatalogService.decisionReasonEvidencePromotionPrefix() + evidencePromotion.status() + ":" + evidencePromotion.promotedToolExampleIds());
        List<String> finalSafeguards = new ArrayList<>(safeguards);
        if (deliberation.required()) {
            finalSafeguards.add(textCatalogService.safeguardDeliberationCheckpointRequired());
        }
        if (contextCompaction.retainedHistoryBudget() < (request.history() == null ? 0 : request.history().size())) {
            finalSafeguards.add(textCatalogService.safeguardContextWindowCompactionRequired());
        }
        if (!doctor.ready()) {
            finalSafeguards.add(textCatalogService.safeguardMcpDoctorReviewRequired());
        }
        finalSafeguards.addAll(evidencePromotion.safeguards());
        return new LegalMcpExecutionPlan(
                preliminaryPlan.planId(),
                preliminaryPlan.selectionMode(),
                preliminaryPlan.discoveryEnabled(),
                preliminaryPlan.transportProfile(),
                preliminaryPlan.authorizationProfile(),
                preliminaryPlan.batchingStrategy(),
                preliminaryPlan.completionStrategy(),
                preliminaryPlan.trustMode(),
                preliminaryPlan.evidenceBudget(),
                preliminaryPlan.serverBudget(),
                preliminaryPlan.pinnedServers(),
                preliminaryPlan.fallbackServers(),
                preliminaryPlan.pinnedToolIds(),
                pinnedSkills,
                pinnedToolExamples,
                deliberation,
                contextCompaction,
                transcript,
                doctor,
                evidencePromotion,
                List.copyOf(finalReasons),
                List.copyOf(finalSafeguards),
                evaluation
        );
    }

    private int resolveServerBudget(LegalMcpServerProfile.ResolveRequest request) {
        if (request.promptInjectionDetected() || request.quarantinedContext()) return 2;
        if (request.sigilo()) return 3;
        if (textCatalogService.isHighImpactCapability(request.capability()) || normalize(request.capability()).contains("VALIDATE")) return 4;
        return 3;
    }

    private int resolveEvidenceBudget(LegalMcpServerProfile.ResolveRequest request) {
        int budget = 4;
        if (request.recommendedSchema() != null && request.recommendedSchema().citationFirst()) budget += 2;
        if (request.sigilo()) budget += 1;
        if (request.attachments() != null && !request.attachments().isEmpty()) budget += 1;
        if (request.promptInjectionDetected()) budget = Math.max(2, budget - 2);
        return budget;
    }

    private String selectionMode(LegalMcpServerProfile.ResolveRequest request, List<LegalMcpServerDescriptor> pinned) {
        if (request.promptInjectionDetected() || request.quarantinedContext()) return textCatalogService.selectionModeIsolatedDocumentalFence();
        if (request.sigilo()) return textCatalogService.selectionModePinnedStrictTrustChain();
        if (pinned.size() >= 3) return textCatalogService.selectionModeDiscoveryThenPin();
        return textCatalogService.selectionModePinnedOnly();
    }

    private List<String> decisionReasons(LegalMcpServerProfile.ResolveRequest request,
                                         List<LegalMcpServerDescriptor> pinned) {
        List<String> reasons = new ArrayList<>();
        reasons.add(textCatalogService.decisionReasonCapabilityPrefix() + request.capability());
        if (request.recommendedSchema() != null) reasons.add(textCatalogService.decisionReasonSchemaPrefix() + request.recommendedSchema().schemaId());
        if (request.sigilo()) reasons.add(textCatalogService.decisionReasonSigiloStrictTrustChain());
        if (request.attachments() != null && !request.attachments().isEmpty()) reasons.add(textCatalogService.decisionReasonAttachmentsPresent());
        if (request.promptInjectionDetected()) reasons.add(textCatalogService.decisionReasonPromptInjectionDetected());
        reasons.add(textCatalogService.decisionReasonPinnedServersPrefix() + pinned.stream().map(LegalMcpServerDescriptor::serverId).toList());
        return List.copyOf(reasons);
    }

    private List<String> safeguards(LegalMcpServerProfile.ResolveRequest request,
                                    List<LegalMcpServerDescriptor> pinned) {
        List<String> safeguards = new ArrayList<>();
        safeguards.add(textCatalogService.safeguardReadOnlyOnly());
        safeguards.add(textCatalogService.safeguardToolAnnotationsRequired());
        if (request.sigilo()) safeguards.add(textCatalogService.safeguardSignedContextRequired());
        if (request.attachments() != null && !request.attachments().isEmpty()) safeguards.add(textCatalogService.safeguardCitationFirstEvidenceTrail());
        if (request.promptInjectionDetected() || request.quarantinedContext()) safeguards.add(textCatalogService.safeguardDocumentalQuarantineFence());
        if (pinned.stream().anyMatch(server -> server.serverId().equals("MCP_INTEROPERABILIDADE"))) safeguards.add(textCatalogService.safeguardFederatedAccessPolicy());
        return List.copyOf(safeguards);
    }

    private Object firstPresent(Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty()) return null;
        for (String key : keys) {
            if (source.containsKey(key)) return source.get(key);
        }
        return null;
    }

    private boolean isStrictSigilo(Object value) {
        if (value instanceof Boolean bool) return bool;
        String normalized = normalize(stringValue(value));
        return normalized.contains("SIGIL") || normalized.contains("RESTRIT") || normalized.contains("SEGREDO");
    }

    private boolean riskFlag(Map<String, Object> payload) {
        String message = normalize(stringValue(payload.get("message")));
        if (message.contains("IGNORE PREVIOUS") || message.contains("SYSTEM PROMPT") || message.contains("INSTRUCOES ANTERIORES")) return true;
        Object explicit = payload.get("promptInjectionDetected");
        return explicit instanceof Boolean bool && bool;
    }

    private boolean quarantinedFlag(Map<String, Object> payload) {
        Object explicit = payload.get("quarantinedContext");
        if (explicit instanceof Boolean bool) return bool;
        Object source = payload.get("documentSecurityStatus");
        return normalize(stringValue(source)).contains("QUARANTIN");
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record ServerCandidate(LegalMcpServerDescriptor descriptor, int score) {
    }
}
