package com.tcc.pjb.backend.service.intelligence.surface;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaHallucinationGuardService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaResearchDossierService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaValidationEnvelopeService;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalTriageRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalTriageResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.surface.LegalAiStructuredSurfaceEvidenceBundle;
import com.tcc.pjb.backend.model.dto.ai.legal.surface.LegalAiStructuredSurfaceGovernanceSnapshot;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LegalAiSurfaceFacadeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiModelClient aiModelV1;
    private final AiModelClient aiModelV2;
    private final VectorSearchService vectorSearchService;
    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;
    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;
    private final JuridicaResearchDossierService juridicaResearchDossierService;
    private final JuridicaValidationEnvelopeService juridicaValidationEnvelopeService;
    private final JuridicaHallucinationGuardService juridicaHallucinationGuardService;
    private final JuridicaLegalAiConversationService juridicaLegalAiConversationService;
    private final LegalAiStructuredSurfaceGovernanceService surfacePromotionGovernanceService;
    private final LegalAiStructuredSurfaceEvidenceAssemblerService surfaceEvidenceAssemblerService;

    public LegalAiSurfaceFacadeService(@Qualifier("aiModelV1") AiModelClient aiModelV1,
                                       @Qualifier("aiModelV2") AiModelClient aiModelV2,
                                       VectorSearchService vectorSearchService,
                                       JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService,
                                       JuridicaLegalAiSpineService juridicaLegalAiSpineService,
                                       JuridicaResearchDossierService juridicaResearchDossierService,
                                       JuridicaValidationEnvelopeService juridicaValidationEnvelopeService,
                                       JuridicaHallucinationGuardService juridicaHallucinationGuardService,
                                       JuridicaLegalAiConversationService juridicaLegalAiConversationService,
                                       LegalAiStructuredSurfaceGovernanceService surfacePromotionGovernanceService,
                                       LegalAiStructuredSurfaceEvidenceAssemblerService surfaceEvidenceAssemblerService) {
        this.aiModelV1 = Objects.requireNonNull(aiModelV1);
        this.aiModelV2 = Objects.requireNonNull(aiModelV2);
        this.vectorSearchService = Objects.requireNonNull(vectorSearchService);
        this.juridicaUnifiedMeshProfileService = Objects.requireNonNull(juridicaUnifiedMeshProfileService);
        this.juridicaLegalAiSpineService = Objects.requireNonNull(juridicaLegalAiSpineService);
        this.juridicaResearchDossierService = Objects.requireNonNull(juridicaResearchDossierService);
        this.juridicaValidationEnvelopeService = Objects.requireNonNull(juridicaValidationEnvelopeService);
        this.juridicaHallucinationGuardService = Objects.requireNonNull(juridicaHallucinationGuardService);
        this.juridicaLegalAiConversationService = Objects.requireNonNull(juridicaLegalAiConversationService);
        this.surfacePromotionGovernanceService = Objects.requireNonNull(surfacePromotionGovernanceService);
        this.surfaceEvidenceAssemblerService = Objects.requireNonNull(surfaceEvidenceAssemblerService);
    }

    public LegalTriageResponse triage(LegalTriageRequest request) {
        String assunto = request != null ? request.assunto() : null;
        String materia = request != null ? request.materia() : null;
        String contexto = request != null ? request.contextoJuridico() : null;
        Map<String, Object> filtros = request != null ? request.filtros() : null;
        int topK = request != null && request.topK() != null && request.topK() > 0 ? request.topK() : 8;
        var similar = vectorSearchService.searchSimilarV1(Objects.toString(assunto, ""), filtros, topK);

        var mesh = juridicaUnifiedMeshProfileService.resolveForSurface("LEGAL_TRIAGE_V1");
        var spine = juridicaLegalAiSpineService.resolveForSurface("LEGAL_TRIAGE_V1", ApiVersion.V1);
        var hallucinationGuard = juridicaHallucinationGuardService.evaluate(new LegalHallucinationGuardRequest(
                contexto,
                materia,
                valueOrNull(filtros == null ? null : filtros.get("rito")),
                valueOrNull(filtros == null ? null : filtros.get("classe")),
                List.of(),
                filtros
        ));

        LinkedHashMap<String, Object> input = new LinkedHashMap<>();
        input.put("assunto", assunto);
        input.put("materia", materia);
        input.put("contextoJuridico", contexto);
        input.put("similarDocs", similar);
        input.put("juridicaMeshProfile", mesh.asMap());
        input.put("juridicaSpineProfile", spine.asMap());
        input.put("juridicaRetrievalStages", spine.retrieval().stages());
        input.put("juridicaMemoryScopes", spine.memory().enabledScopes());
        input.put("juridicaSymbolicEngines", spine.validation().symbolicEngines());
        input.put("juridicaGraphTraversalModes", spine.graph().traversalModes());
        input.put("juridicaMultimodalModalities", spine.multimodal().enabledModalities());
        input.put("juridicaEvalSuites", spine.evaluation().evalSuites());
        input.put("juridicaHallucinationGuard", spine.hallucinationGuard().asMap());
        input.put("juridicaHallucinationStatus", hallucinationGuard.status());

        String prompt = """
                Você é a IA jurídica V1 do PJB.
                Objetivo: classificar o caso, extrair keywords e sugerir documentos faltantes para seguir o rito correto.
                Observe a espinha jurídica, o pipeline de retrieval híbrido, a memória isolada, a validação simbólica e a politica anti-alucinacao antes de responder.
                Não invente artigo, tema, sumula, precedente ou jurisprudencia. Se algo não estiver confirmado no grounding, use [NAO_CONFIRMADO].
                Responda EXCLUSIVAMENTE em JSON válido, no seguinte formato:
                {
                  "classificacao": "...",
                  "keywords": ["..."],
                  "documentosFaltantesSugeridos": ["..."]
                }
                Entrada: %s
                """.formatted(toJson(input));

        return toTriageResponse(aiModelV1.generate(prompt));
    }

    public LegalDraftResponse minuta(LegalDraftRequest request) {
        var mesh = juridicaUnifiedMeshProfileService.resolveForSurface("LEGAL_DRAFT_V2", ApiVersion.V2);
        var spine = juridicaLegalAiSpineService.resolveForSurface("LEGAL_DRAFT_V2", ApiVersion.V2);
        var governance = surfacePromotionGovernanceService.inspect(
                toDraftConversationRequest(request),
                "LEGAL_DRAFT_V2",
                ApiVersion.V2.name(),
                mesh.tools()
        );
        var evidenceBundle = surfaceEvidenceAssemblerService.assembleForDraft(governance);
        var hallucinationGuard = juridicaHallucinationGuardService.evaluate(toDraftHallucinationGuardRequest(request, governance, evidenceBundle));
        if (governance.draftBlocked()
                || "BLOCKED".equalsIgnoreCase(hallucinationGuard.status())
                || (!evidenceBundle.anchored() && "PROMOTED".equalsIgnoreCase(evidenceBundle.promotionStatus()))) {
            return new LegalDraftResponse(
                    blockedDraft(governance),
                    "BLOCKED",
                    governance.nextSteps(),
                    enrichSafeguards(governance, hallucinationGuard, evidenceBundle)
            );
        }
        if (governance.draftStepUpRequired()
                || "REVIEW_REQUIRED".equalsIgnoreCase(hallucinationGuard.status())
                || "ESCALATED".equalsIgnoreCase(governance.surfaceStatus())
                || "ENFORCED".equalsIgnoreCase(governance.surfaceStatus())
                || !evidenceBundle.anchored()) {
            return new LegalDraftResponse(
                    governedDraftScaffold(request, governance),
                    "STEP_UP_REQUIRED",
                    governance.nextSteps(),
                    enrichSafeguards(governance, hallucinationGuard, evidenceBundle)
            );
        }
        String prompt = "Você é a IA jurídica V2 do PJB (minutas). "
                + "Não invente fatos. Se faltar informação, marque como [PENDENTE] e liste o que falta. "
                + "Gere um texto estruturado com: RELATÓRIO, FUNDAMENTAÇÃO, DISPOSITIVO, PROVIDÊNCIAS/ANDAMENTO (rito).\n\n"
                + "Análise V1: " + safe(request != null ? request.analiseV1() : null) + "\n"
                + "Petição Inicial (texto): " + safe(request != null ? request.peticaoInicialText() : null) + "\n"
                + "Instruções: " + safe(request != null ? request.instrucoes() : null) + "\n"
                + "Objetivo: " + safe(request != null ? request.objetivo() : null) + "\n"
                + "Perfil jurídico: " + safe(request != null ? request.userProfile() : null) + "\n"
                + "Governança soberana da superfície: " + toJson(governance.safeguards()) + "\n"
                + "Evidências promovidas para minuta: " + toJson(evidenceBundle.promotedEvidenceIds()) + "\n"
                + "Descritores soberanos de evidência: " + toJson(evidenceBundle.promotedEvidenceDescriptors()) + "\n"
                + "Próximos passos soberanos: " + toJson(governance.nextSteps()) + "\n"
                + "Malha jurídica governada: " + toJson(mesh.asMap()) + "\n"
                + "Espinha jurídica estruturada: " + toJson(spine.asMap()) + "\n"
                + "Anti-alucinacao jurídica: " + toJson(hallucinationGuard);
        return new LegalDraftResponse(
                aiModelV2.generate(prompt),
                "PROMOTED",
                governance.nextSteps(),
                enrichSafeguards(governance, hallucinationGuard, evidenceBundle)
        );
    }

    public LegalResearchDossierResponse researchDossier(LegalResearchDossierRequest request) {
        return juridicaResearchDossierService.build(request);
    }

    public LegalValidationResponse validate(LegalValidationRequest request) {
        return juridicaValidationEnvelopeService.validate(request);
    }

    public LegalHallucinationGuardResponse hallucinationGuard(LegalHallucinationGuardRequest request) {
        var mesh = juridicaUnifiedMeshProfileService.resolveForSurface("LEGAL_GROUNDING_CHECK_V3", ApiVersion.V3);
        var governance = surfacePromotionGovernanceService.inspect(
                toGroundingConversationRequest(request),
                "LEGAL_GROUNDING_CHECK_V3",
                ApiVersion.V3.name(),
                mesh.tools()
        );
        var evidenceBundle = surfaceEvidenceAssemblerService.assembleForGrounding(governance);
        var base = juridicaHallucinationGuardService.evaluate(request);
        List<String> suspiciousSignals = new ArrayList<>(base.suspiciousSignals() == null ? List.of() : base.suspiciousSignals());
        List<String> blockedReasons = new ArrayList<>(base.blockedReasons() == null ? List.of() : base.blockedReasons());
        String status = base.status();
        if (governance.groundingBlocked() || (!evidenceBundle.anchored() && "PROMOTED".equalsIgnoreCase(evidenceBundle.promotionStatus()))) {
            status = "BLOCKED";
            blockedReasons.add("Grounding estruturado foi bloqueado pela cerca soberana de proveniência antes da promoção para citações ou lastro material.");
        } else if (governance.groundingStepUpRequired() || "ESCALATED".equalsIgnoreCase(governance.surfaceStatus()) || "ENFORCED".equalsIgnoreCase(governance.surfaceStatus())) {
            if (!"BLOCKED".equalsIgnoreCase(status)) {
                status = "REVIEW_REQUIRED";
            }
            suspiciousSignals.add("A cadeia soberana exige confirmação oficial adicional antes de tratar o grounding como materialmente estável.");
        }
        LinkedHashMap<String, Object> trace = new LinkedHashMap<>(base.trace() == null ? Map.of() : base.trace());
        trace.putAll(governance.safeguards());
        trace.putAll(surfaceEvidenceAssemblerService.toSafeguards(evidenceBundle));
        return new LegalHallucinationGuardResponse(
                base.profileCode(),
                base.version(),
                base.capability(),
                status,
                base.articleReferenceVerificationRequired(),
                base.precedentVerificationRequired(),
                base.freeFormCitationBlocked(),
                base.citationEmissionMode(),
                base.unresolvedCitationPlaceholder(),
                value(governance.safeguards(), "evidenceProvenanceStatus"),
                value(governance.safeguards(), "evidenceProvenanceTier"),
                value(governance.safeguards(), "evidenceProvenanceMode"),
                value(governance.safeguards(), "groundingPromotionStatus"),
                List.copyOf(suspiciousSignals),
                List.copyOf(blockedReasons),
                Map.copyOf(trace)
        );
    }

    public LegalAiConversationResponse converse(LegalAiConversationRequest request) {
        return juridicaLegalAiConversationService.converse(request);
    }

    private LegalHallucinationGuardRequest toDraftHallucinationGuardRequest(LegalDraftRequest request,
                                                                            LegalAiStructuredSurfaceGovernanceSnapshot governance,
                                                                            LegalAiStructuredSurfaceEvidenceBundle evidenceBundle) {
        LinkedHashMap<String, Object> filters = copyWithoutNulls(request == null ? null : request.contexto());
        filters.putAll(governance.safeguards());
        filters.putAll(surfaceEvidenceAssemblerService.toSafeguards(evidenceBundle));
        putIfPresent(filters, "draftObjective", request == null ? null : request.objetivo());
        putIfPresent(filters, "draftInstructions", request == null ? null : request.instrucoes());
        return new LegalHallucinationGuardRequest(
                request == null ? null : request.peticaoInicialText(),
                request == null ? null : request.ramo(),
                request == null ? null : request.rito(),
                request == null ? null : request.classe(),
                List.of(),
                Map.copyOf(filters)
        );
    }

    private LegalAiConversationRequest toDraftConversationRequest(LegalDraftRequest request) {
        LinkedHashMap<String, Object> context = copyWithoutNulls(request == null ? null : request.contexto());
        putIfPresent(context, "objetivo", request == null ? null : request.objetivo());
        putIfPresent(context, "instrucoes", request == null ? null : request.instrucoes());
        putIfPresent(context, "analiseV1", request == null ? null : request.analiseV1());
        putIfPresent(context, "ramo", request == null ? null : request.ramo());
        putIfPresent(context, "rito", request == null ? null : request.rito());
        putIfPresent(context, "classe", request == null ? null : request.classe());
        return new LegalAiConversationRequest(
                null,
                request == null ? null : request.processoId(),
                joinMessage(request == null ? null : request.objetivo(), request == null ? null : request.instrucoes(), request == null ? null : request.peticaoInicialText()),
                request == null ? null : request.userProfile(),
                List.of(),
                request == null || request.attachments() == null ? List.of() : request.attachments(),
                Map.copyOf(context)
        );
    }

    private LegalAiConversationRequest toGroundingConversationRequest(LegalHallucinationGuardRequest request) {
        LinkedHashMap<String, Object> context = copyWithoutNulls(request == null ? null : request.filtros());
        if (request != null) {
            putIfPresent(context, "ramo", request.ramo());
            putIfPresent(context, "rito", request.rito());
            putIfPresent(context, "classe", request.classe());
            context.put("groundedCitations", request.groundedCitations() == null ? List.of() : request.groundedCitations());
        }
        return new LegalAiConversationRequest(
                null,
                valueOrNull(context.get("processoId")),
                request == null ? null : request.texto(),
                valueOrNull(context.get("userProfile")),
                List.of(),
                listValue(firstPresent(context, "attachments", "documentosAnexados")),
                Map.copyOf(context)
        );
    }

    private String blockedDraft(LegalAiStructuredSurfaceGovernanceSnapshot governance) {
        return "RELATÓRIO\n[PROMOCAO_DOCUMENTAL_BLOQUEADA]\n\n"
                + "FUNDAMENTAÇÃO\n[PENDENTE_CONFIRMACAO_SOBERANA]\n\n"
                + "DISPOSITIVO\n[PENDENTE_CONFIRMACAO_SOBERANA]\n\n"
                + "PROVIDÊNCIAS/ANDAMENTO\n"
                + governance.nextSteps().stream().map(item -> "- " + item).reduce("", (left, right) -> left + right + "\n").trim();
    }

    private String governedDraftScaffold(LegalDraftRequest request,
                                         LegalAiStructuredSurfaceGovernanceSnapshot governance) {
        String objetivo = safe(request == null ? null : request.objetivo());
        String instrucoes = safe(request == null ? null : request.instrucoes());
        String rito = safe(request == null ? null : request.rito());
        return "RELATÓRIO\nObjetivo declarado: " + objetivo + "\nInstruções soberanas: " + instrucoes + "\nRito informado: " + rito + "\n\n"
                + "FUNDAMENTAÇÃO\n[PENDENTE_CONFIRMACAO_SOBERANA]\n\n"
                + "DISPOSITIVO\n[PENDENTE_CONFIRMACAO_SOBERANA]\n\n"
                + "PROVIDÊNCIAS/ANDAMENTO\n"
                + governance.nextSteps().stream().map(item -> "- " + item).reduce("", (left, right) -> left + right + "\n").trim();
    }

    private Map<String, Object> enrichSafeguards(LegalAiStructuredSurfaceGovernanceSnapshot governance,
                                                 LegalHallucinationGuardResponse hallucinationGuard,
                                                 LegalAiStructuredSurfaceEvidenceBundle evidenceBundle) {
        LinkedHashMap<String, Object> safeguards = new LinkedHashMap<>(governance.safeguards());
        if (hallucinationGuard != null && hallucinationGuard.status() != null) {
            safeguards.put("hallucinationGuardStatus", hallucinationGuard.status());
        }
        safeguards.putAll(surfaceEvidenceAssemblerService.toSafeguards(evidenceBundle));
        safeguards.put("promotedGroundingEvidenceIds", governance.safeguards().getOrDefault("promotedGroundingEvidenceIds", List.of()));
        safeguards.put("promotedDraftEvidenceIds", governance.safeguards().getOrDefault("promotedDraftEvidenceIds", List.of()));
        safeguards.put("hallucinationBlockedReasons", hallucinationGuard == null || hallucinationGuard.blockedReasons() == null ? List.of() : hallucinationGuard.blockedReasons());
        safeguards.put("hallucinationSuspiciousSignals", hallucinationGuard == null || hallucinationGuard.suspiciousSignals() == null ? List.of() : hallucinationGuard.suspiciousSignals());
        return Map.copyOf(safeguards);
    }

    private String joinMessage(String... values) {
        String joined = java.util.Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
        return joined == null || joined.isBlank() ? null : joined;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        target.put(key, value);
    }

    private LinkedHashMap<String, Object> copyWithoutNulls(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out;
    }

    private Object firstPresent(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
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

    private String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private LegalTriageResponse toTriageResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LegalTriageResponse(null, List.of(), List.of(), "");
        }
        try {
            Map<String, Object> parsed = MAPPER.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
            return new LegalTriageResponse(
                    stringValue(parsed.get("classificacao")),
                    stringList(parsed.get("keywords")),
                    stringList(parsed.get("documentosFaltantesSugeridos")),
                    raw
            );
        } catch (Exception ignored) {
            return new LegalTriageResponse(null, List.of(), List.of(), raw);
        }
    }

    private static List<String> stringList(Object value) {
        if (value instanceof Iterable<?> iterable) {
            return java.util.stream.StreamSupport.stream(iterable.spliterator(), false)
                    .map(item -> item == null ? null : String.valueOf(item).trim())
                    .filter(item -> item != null && !item.isBlank())
                    .toList();
        }
        return List.of();
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "[PENDENTE]" : value;
    }

    private static String valueOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }
}
