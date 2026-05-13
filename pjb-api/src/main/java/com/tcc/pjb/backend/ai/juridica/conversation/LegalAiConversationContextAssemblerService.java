package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSanitizationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTraceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiSchemaDefinition;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationContextAssemblerService {

    private final JuridicaUnifiedMeshProfileService meshProfileService;
    private final JuridicaLegalAiSpineService spineService;
    private final LegalAiStructuredSchemaCatalog structuredSchemaCatalog;

    public LegalAiConversationContextAssemblerService(JuridicaUnifiedMeshProfileService meshProfileService,
                                                      JuridicaLegalAiSpineService spineService,
                                                      LegalAiStructuredSchemaCatalog structuredSchemaCatalog) {
        this.meshProfileService = Objects.requireNonNull(meshProfileService, "meshProfileService");
        this.spineService = Objects.requireNonNull(spineService, "spineService");
        this.structuredSchemaCatalog = Objects.requireNonNull(structuredSchemaCatalog, "structuredSchemaCatalog");
    }

    public LegalAiSpineProfileResponse resolveSpineProfile(String capability, ApiVersion version) {
        return spineService.resolveForSurface(capability, version);
    }

    public ConversationBundle assemble(LegalAiConversationRequest request,
                                       ApiVersion version,
                                       String capability,
                                       LegalResearchDossierResponse dossier,
                                       LegalValidationResponse validation,
                                       LegalHallucinationGuardResponse guard,
                                       LegalAiConversationMemorySnapshot memory,
                                       LegalAiConversationTraceSnapshot trace,
                                       LegalAiConversationApprovalSnapshot approval,
                                       LegalAiConversationSanitizationSnapshot sanitization,
                                       LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                       LegalAiConversationToolScopeSnapshot toolScope) {
        var mesh = meshProfileService.resolveForSurface(capability);
        var spine = spineService.resolveForSurface(capability, version);
        var availableSchemas = structuredSchemaCatalog.resolve(version);
        var recommendedSchema = structuredSchemaCatalog.recommend(version, capability, request);
        LinkedHashMap<String, Object> context = new LinkedHashMap<>();
        context.put("conversationId", request == null ? null : request.conversationId());
        context.put("processoId", request == null ? null : request.processoId());
        context.put("userProfile", request == null ? null : request.userProfile());
        context.put("message", request == null ? null : request.message());
        context.put("history", request == null || request.history() == null ? List.of() : request.history());
        context.put("attachments", request == null || request.attachments() == null ? List.of() : request.attachments());
        context.put("externalContext", request == null || request.context() == null ? Map.of() : request.context());
        context.put("juridicaMeshProfile", mesh.asMap());
        context.put("juridicaMcpPlan", mesh.mcp().getOrDefault("plan", Map.of()));
        context.put("juridicaMcpDoctor", mesh.mcp().getOrDefault("doctorStatus", null));
        context.put("juridicaMcpTranscript", mesh.mcp().getOrDefault("transcriptId", null));
        context.put("juridicaSpineProfile", spine.asMap());
        context.put("juridicaResearchDossier", dossier);
        context.put("juridicaValidationEnvelope", validation);
        context.put("juridicaHallucinationGuard", guard);
        context.put("juridicaConversationMemory", memory == null ? Map.of() : memory.asMap());
        context.put("juridicaConversationTrace", trace == null ? Map.of() : trace.asMap());
        context.put("juridicaConversationApproval", approval == null ? Map.of() : approval.asMap());
        context.put("juridicaConversationSanitization", sanitization == null ? Map.of() : sanitization.asMap());
        context.put("juridicaConversationDocumentSecurity", documentSecurity == null ? Map.of() : documentSecurity.asMap());
        context.put("juridicaConversationToolScope", toolScope == null ? Map.of() : toolScope.asMap());
        context.put("juridicaStructuredSchemaCatalog", availableSchemas.stream().map(LegalAiSchemaDefinition::asMap).toList());
        context.put("juridicaRecommendedSchema", recommendedSchema == null ? Map.of() : recommendedSchema.asMap());
        return new ConversationBundle(mesh.asMap(), spine.asMap(), ImmutableViewSupport.map(context), dossier, validation, guard, recommendedSchema);
    }

    public LegalResearchDossierRequest dossierRequest(LegalAiConversationRequest request, ApiVersion version, String capability) {
        Map<String, Object> ctx = request == null || request.context() == null ? Map.of() : request.context();
        return new LegalResearchDossierRequest(
                request == null ? null : request.message(),
                stringValue(ctx.get("materia")),
                request == null ? null : request.message(),
                stringValue(ctx.get("ramo")),
                stringValue(ctx.get("rito")),
                ctx,
                8
        );
    }

    public LegalValidationRequest validationRequest(LegalAiConversationRequest request,
                                                    ApiVersion version,
                                                    String capability,
                                                    LegalResearchDossierResponse dossier) {
        Map<String, Object> ctx = request == null || request.context() == null ? Map.of() : request.context();
        return new LegalValidationRequest(
                request == null ? null : request.message(),
                stringValue(ctx.get("ramo")),
                stringValue(ctx.get("rito")),
                stringValue(ctx.get("classe")),
                capability,
                stringValue(ctx.get("sigilo")),
                validationFilters(dossier, ctx)
        );
    }

    public LegalHallucinationGuardRequest guardRequest(LegalAiConversationRequest request,
                                                       String capability,
                                                       ApiVersion version) {
        Map<String, Object> ctx = request == null || request.context() == null ? Map.of() : request.context();
        return new LegalHallucinationGuardRequest(
                request == null ? null : request.message(),
                stringValue(ctx.get("ramo")),
                stringValue(ctx.get("rito")),
                stringValue(ctx.get("classe")),
                request == null || request.history() == null ? List.of() : request.history(),
                ctx
        );
    }

    private Map<String, Object> validationFilters(LegalResearchDossierResponse dossier, Map<String, Object> context) {
        LinkedHashMap<String, Object> filters = new LinkedHashMap<>();
        filters.putAll(context == null ? Map.of() : context);
        if (dossier != null) {
            filters.put("retrievalStages", dossier.retrievalStages());
            filters.put("authorityLanes", dossier.authorityLanes());
            filters.put("graphTraversals", dossier.graphTraversals());
            filters.put("toolIds", dossier.toolIds());
        }
        return ImmutableViewSupport.map(filters);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    public record ConversationBundle(
            Map<String, Object> mesh,
            Map<String, Object> spine,
            Map<String, Object> conversationContext,
            LegalResearchDossierResponse dossier,
            LegalValidationResponse validation,
            LegalHallucinationGuardResponse guard,
            LegalAiSchemaDefinition recommendedSchema
    ) {
    }
}
