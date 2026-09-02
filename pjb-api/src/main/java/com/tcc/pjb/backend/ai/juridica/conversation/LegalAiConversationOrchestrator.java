package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDocumentQuarantineService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSourceAllowlist;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalToolScopePolicy;
import com.tcc.pjb.backend.ai.juridica.knowledge.LegalKnowledgeCoverageService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiPreConsciousFrameSnapshot;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationOrchestrator {

    private final LegalAiConversationRoutingService routingService;
    private final LegalAiConversationContextAssemblerService contextAssemblerService;
    private final LegalAiJuridicaSpineOrchestrator juridicaSpine;
    private final JuridicaVirtualTrendsCouncilService virtualTrendsCouncilService;
    private final LegalAiConversationResponseComposerService responseComposerService;
    private final LegalAiConversationMemoryService memoryService;
    private final LegalAiConversationTraceService traceService;
    private final LegalAiConversationApprovalService approvalService;
    private final LegalContextSanitizer contextSanitizer;
    private final LegalSourceAllowlist sourceAllowlist;
    private final LegalDocumentQuarantineService documentQuarantineService;
    private final LegalToolScopePolicy toolScopePolicy;
    private final LegalAiConversationSessionInspectionOrchestrator sessionInspection;
    private final LegalAiCapabilityLifecycleOrchestrator capabilityLifecycle;
    private final LegalAiConversationTrustZoneService trustZoneService;
    private final LegalAiConversationEvidenceProvenanceService evidenceProvenanceService;
    private final LegalKnowledgeCoverageService knowledgeCoverageService;
    private final LegalAiPreConsciousFrameService preConsciousFrameService;
    private final LegalAiPreConsciousToolScopeEnricher preConsciousToolScopeEnricher;

    public LegalAiConversationOrchestrator(LegalAiConversationRoutingService routingService,
                                           LegalAiConversationContextAssemblerService contextAssemblerService,
                                           LegalAiJuridicaSpineOrchestrator juridicaSpine,
                                           JuridicaVirtualTrendsCouncilService virtualTrendsCouncilService,
                                           LegalAiConversationResponseComposerService responseComposerService,
                                           LegalAiConversationMemoryService memoryService,
                                           LegalAiConversationTraceService traceService,
                                           LegalAiConversationApprovalService approvalService,
                                           LegalContextSanitizer contextSanitizer,
                                           LegalSourceAllowlist sourceAllowlist,
                                           LegalDocumentQuarantineService documentQuarantineService,
                                           LegalToolScopePolicy toolScopePolicy,
                                           LegalAiConversationSessionInspectionOrchestrator sessionInspection,
                                           LegalAiCapabilityLifecycleOrchestrator capabilityLifecycle,
                                           LegalAiConversationTrustZoneService trustZoneService,
                                           LegalAiConversationEvidenceProvenanceService evidenceProvenanceService,
                                           LegalKnowledgeCoverageService knowledgeCoverageService,
                                           LegalAiPreConsciousFrameService preConsciousFrameService,
                                           LegalAiPreConsciousToolScopeEnricher preConsciousToolScopeEnricher) {
        this.routingService = Objects.requireNonNull(routingService, "routingService");
        this.contextAssemblerService = Objects.requireNonNull(contextAssemblerService, "contextAssemblerService");
        this.juridicaSpine = Objects.requireNonNull(juridicaSpine, "juridicaSpine");
        this.virtualTrendsCouncilService = Objects.requireNonNull(virtualTrendsCouncilService, "virtualTrendsCouncilService");
        this.responseComposerService = Objects.requireNonNull(responseComposerService, "responseComposerService");
        this.memoryService = Objects.requireNonNull(memoryService, "memoryService");
        this.traceService = Objects.requireNonNull(traceService, "traceService");
        this.approvalService = Objects.requireNonNull(approvalService, "approvalService");
        this.contextSanitizer = Objects.requireNonNull(contextSanitizer, "contextSanitizer");
        this.sourceAllowlist = Objects.requireNonNull(sourceAllowlist, "sourceAllowlist");
        this.documentQuarantineService = Objects.requireNonNull(documentQuarantineService, "documentQuarantineService");
        this.toolScopePolicy = Objects.requireNonNull(toolScopePolicy, "toolScopePolicy");
        this.sessionInspection = Objects.requireNonNull(sessionInspection, "sessionInspection");
        this.capabilityLifecycle = Objects.requireNonNull(capabilityLifecycle, "capabilityLifecycle");
        this.trustZoneService = Objects.requireNonNull(trustZoneService, "trustZoneService");
        this.evidenceProvenanceService = Objects.requireNonNull(evidenceProvenanceService, "evidenceProvenanceService");
        this.knowledgeCoverageService = Objects.requireNonNull(knowledgeCoverageService, "knowledgeCoverageService");
        this.preConsciousFrameService = Objects.requireNonNull(preConsciousFrameService, "preConsciousFrameService");
        this.preConsciousToolScopeEnricher = Objects.requireNonNull(preConsciousToolScopeEnricher, "preConsciousToolScopeEnricher");
    }

    public LegalAiConversationResponse converse(LegalAiConversationRequest request) {
        String conversationId = resolveConversationId(request);
        var sanitization = contextSanitizer.sanitize(request);
        LegalAiConversationRequest effectiveRequest = sanitization.request();
        ApiVersion version = routingService.resolveVersion(effectiveRequest);
        String capability = routingService.resolveCapability(effectiveRequest, version);
        var spine = contextAssemblerService.resolveSpineProfile(capability, version);
        var memoryBefore = memoryService.snapshot(conversationId, effectiveRequest, spine.memory());
        var sourceDecision = sourceAllowlist.evaluate(effectiveRequest);
        var documentSecurity = documentQuarantineService.inspect(effectiveRequest, sanitization, sourceDecision);
        var toolScope = toolScopePolicy.evaluate(effectiveRequest, capability, version.name(), spine.routedTools(), documentSecurity);
        var traceOpen = traceService.open(conversationId, effectiveRequest, version.name(), capability, spine.trace(), memoryBefore, sanitization, documentSecurity, toolScope);
        var preliminaryApproval = approvalService.evaluate(effectiveRequest, capability, version.name(), spine.approval(), spine.routedTools(), memoryBefore, traceOpen, documentSecurity, toolScope, sanitization);
        var dossier = juridicaSpine.buildDossier(contextAssemblerService.dossierRequest(effectiveRequest, version, capability));
        var validation = juridicaSpine.validate(contextAssemblerService.validationRequest(effectiveRequest, version, capability, dossier));
        var guard = juridicaSpine.evaluateGuard(contextAssemblerService.guardRequest(effectiveRequest, capability, version));
        var bundle = contextAssemblerService.assemble(effectiveRequest, version, capability, dossier, validation, guard, memoryBefore, traceOpen, preliminaryApproval, sanitization.snapshot(), documentSecurity, toolScope);
        var enrichedToolScope = toolScopePolicy.enrichWithMcpPlan(toolScope, nestedMap(bundle.conversationContext().get("juridicaMcpPlan")));
        var sessionDoctor = sessionInspection.inspectDoctor(effectiveRequest, capability, version.name(), memoryBefore, documentSecurity, enrichedToolScope, validation, guard);
        var toolScopeWithDoctor = toolScopePolicy.enrichWithSessionDoctor(enrichedToolScope, sessionDoctor);
        var sessionBootstrap = sessionInspection.inspectBootstrap(effectiveRequest, capability, version.name(), memoryBefore, documentSecurity, toolScopeWithDoctor, sessionDoctor);
        var toolScopeWithBootstrap = toolScopePolicy.enrichWithSessionBootstrap(toolScopeWithDoctor, sessionBootstrap);
        var capabilityRecovery = capabilityLifecycle.inspectRecovery(effectiveRequest, capability, version.name(), memoryBefore, documentSecurity, toolScopeWithBootstrap, sessionDoctor, sessionBootstrap);
        var toolScopeWithRecovery = toolScopePolicy.enrichWithCapabilityRecovery(toolScopeWithBootstrap, capabilityRecovery);
        var capabilityCooldown = capabilityLifecycle.inspectCooldown(effectiveRequest, capability, version.name(), memoryBefore, documentSecurity, toolScopeWithRecovery, sessionDoctor, sessionBootstrap, capabilityRecovery);
        var toolScopeWithCooldown = toolScopePolicy.enrichWithCapabilityCooldown(toolScopeWithRecovery, capabilityCooldown);
        var capabilityRehabilitation = capabilityLifecycle.inspectRehabilitation(effectiveRequest, capability, version.name(), memoryBefore, documentSecurity, toolScopeWithCooldown, sessionDoctor, sessionBootstrap, capabilityRecovery, capabilityCooldown);
        var toolScopeWithRehabilitation = toolScopePolicy.enrichWithCapabilityRehabilitation(toolScopeWithCooldown, capabilityRehabilitation);
        var capabilityRecurrence = capabilityLifecycle.inspectRecurrence(effectiveRequest, capability, version.name(), memoryBefore, documentSecurity, toolScopeWithRehabilitation, sessionDoctor, sessionBootstrap, capabilityRecovery, capabilityCooldown, capabilityRehabilitation);
        var toolScopeWithRecurrence = toolScopePolicy.enrichWithCapabilityRecurrence(toolScopeWithRehabilitation, capabilityRecurrence);
        var capabilitySuppression = capabilityLifecycle.inspectSuppression(effectiveRequest, capability, version.name(), documentSecurity, toolScopeWithRecurrence, sessionDoctor, sessionBootstrap, capabilityRecurrence);
        var toolScopeWithSuppression = toolScopePolicy.enrichWithCapabilitySuppression(toolScopeWithRecurrence, capabilitySuppression);
        var trustZone = trustZoneService.inspect(effectiveRequest, capability, version.name(), documentSecurity, toolScopeWithSuppression, sessionDoctor, sessionBootstrap, capabilitySuppression);
        var toolScopeWithTrustZone = toolScopePolicy.enrichWithTrustZone(toolScopeWithSuppression, trustZone);
        var evidenceProvenance = evidenceProvenanceService.inspect(effectiveRequest, capability, version.name(), documentSecurity, trustZone, toolScopeWithTrustZone);
        var finalToolScope = toolScopePolicy.enrichWithEvidenceProvenance(toolScopeWithTrustZone, evidenceProvenance);
        var knowledgeCoverage = knowledgeCoverageService.inspect(effectiveRequest, capability, version.name());
        var preConsciousFrame = preConsciousFrameService.inspect(effectiveRequest, capability, version.name(), memoryBefore, documentSecurity, finalToolScope, trustZone, evidenceProvenance, knowledgeCoverage, validation, guard);
        var governedToolScope = preConsciousToolScopeEnricher.enrich(finalToolScope, preConsciousFrame);
        var approval = approvalService.evaluate(effectiveRequest, capability, version.name(), spine.approval(), spine.routedTools(), memoryBefore, traceOpen, documentSecurity, governedToolScope, sanitization);
        Map<String, Object> councilContext = new LinkedHashMap<>(bundle.conversationContext());
        councilContext.put("juridicaConversationApproval", approval.asMap());
        councilContext.put("juridicaConversationToolScope", governedToolScope.asMap());
        councilContext.put("juridicaConversationSessionDoctor", sessionDoctor.asMap());
        councilContext.put("juridicaConversationSessionBootstrap", sessionBootstrap.asMap());
        councilContext.put("juridicaConversationCapabilityRecovery", capabilityRecovery.asMap());
        councilContext.put("juridicaConversationCapabilityCooldown", capabilityCooldown.asMap());
        councilContext.put("juridicaConversationCapabilityRehabilitation", capabilityRehabilitation.asMap());
        councilContext.put("juridicaConversationCapabilityRecurrence", capabilityRecurrence.asMap());
        councilContext.put("juridicaConversationCapabilitySuppression", capabilitySuppression.asMap());
        councilContext.put("juridicaConversationTrustZone", trustZone.asMap());
        councilContext.put("juridicaConversationEvidenceProvenance", evidenceProvenance.asMap());
        councilContext.put("juridicaKnowledgeCoverage", knowledgeCoverage.asMap());
        councilContext.put("juridicaPreConsciousFrame", preConsciousFrame.asMap());
        var council = virtualTrendsCouncilService.resolveCouncil(version, capability, effectiveRequest == null ? null : effectiveRequest.message(), dossier, validation, guard, councilContext);
        String councilHeadline = virtualTrendsCouncilService.synthesizeHeadline(council);
        var traceClosed = traceService.close(traceOpen, validation, guard, council, approval, sanitization, documentSecurity, governedToolScope);
        String answer = responseComposerService.compose(effectiveRequest, capability, version.name(), councilHeadline, council, validation, guard, approval, traceClosed, memoryBefore, sanitization.snapshot(), documentSecurity, governedToolScope, bundle.recommendedSchema());
        var memoryAfter = memoryService.registerTurn(conversationId, effectiveRequest, capability, version.name(), answer, validation, guard, approval, traceClosed, council, spine.memory());
        Map<String, Object> conversationContext = new LinkedHashMap<>(councilContext);
        conversationContext.put("conversationId", conversationId);
        conversationContext.put("virtualTrendsCouncilHeadline", councilHeadline);
        conversationContext.put("selectedVersion", version.name());
        conversationContext.put("selectedCapability", capability);
        conversationContext.put("conversationMemory", memoryAfter.asMap());
        conversationContext.put("conversationTrace", traceClosed.asMap());
        conversationContext.put("conversationApproval", approval.asMap());
        conversationContext.put("conversationSanitization", sanitization.snapshot().asMap());
        conversationContext.put("conversationDocumentSecurity", documentSecurity.asMap());
        conversationContext.put("conversationToolScope", governedToolScope.asMap());
        conversationContext.put("conversationSessionDoctor", sessionDoctor.asMap());
        conversationContext.put("conversationSessionBootstrap", sessionBootstrap.asMap());
        conversationContext.put("conversationCapabilityRecovery", capabilityRecovery.asMap());
        conversationContext.put("conversationCapabilityCooldown", capabilityCooldown.asMap());
        conversationContext.put("conversationCapabilityRehabilitation", capabilityRehabilitation.asMap());
        conversationContext.put("conversationCapabilityRecurrence", capabilityRecurrence.asMap());
        conversationContext.put("conversationCapabilitySuppression", capabilitySuppression.asMap());
        conversationContext.put("conversationTrustZone", trustZone.asMap());
        conversationContext.put("conversationEvidenceProvenance", evidenceProvenance.asMap());
        conversationContext.put("conversationKnowledgeCoverage", knowledgeCoverage.asMap());
        conversationContext.put("conversationPreConsciousFrame", preConsciousFrame.asMap());
        conversationContext.put("conversationMcpPlan", bundle.conversationContext().getOrDefault("juridicaMcpPlan", Map.of()));
        return new LegalAiConversationResponse(
                conversationId,
                version.name(),
                capability,
                answer,
                buildNextSteps(council, validation, guard, approval, documentSecurity, sessionDoctor, capabilityCooldown, capabilityRehabilitation, capabilityRecurrence, capabilitySuppression, trustZone, evidenceProvenance, knowledgeCoverage, preConsciousFrame),
                council,
                ImmutableViewSupport.map(conversationContext),
                responseComposerService.safeguards(validation, guard, approval, traceClosed, memoryAfter, sanitization.snapshot(), documentSecurity, governedToolScope, bundle.recommendedSchema())
        );
    }

    private List<String> buildNextSteps(List<Map<String, Object>> council,
                                        com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse validation,
                                        com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse guard,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot approval,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot capabilityCooldown,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRehabilitationSnapshot capabilityRehabilitation,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecurrenceSnapshot capabilityRecurrence,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilitySuppressionSnapshot capabilitySuppression,
                                        com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot trustZone,
                                        LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance,
                                        com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot knowledgeCoverage,
                                        LegalAiPreConsciousFrameSnapshot preConsciousFrame) {
        LinkedHashMap<String, String> steps = new LinkedHashMap<>();
        if (documentSecurity != null && documentSecurity.alerts() != null) {
            documentSecurity.alerts().forEach(item -> steps.put("DOC_" + item.hashCode(), item));
        }
        if (sessionDoctor != null && sessionDoctor.reasons() != null) {
            sessionDoctor.reasons().forEach(item -> steps.put("SESSION_DOCTOR_" + item.hashCode(), item));
        }
        if (approval != null && approval.checkpoints() != null) {
            approval.checkpoints().forEach(item -> steps.put("APPROVAL_" + item.hashCode(), item));
        }
        if (capabilityCooldown != null && capabilityCooldown.reasons() != null) {
            capabilityCooldown.reasons().forEach(item -> steps.put("CAPABILITY_COOLDOWN_" + item.hashCode(), item));
        }
        if (capabilityRehabilitation != null && capabilityRehabilitation.reasons() != null) {
            capabilityRehabilitation.reasons().forEach(item -> steps.put("CAPABILITY_REHABILITATION_" + item.hashCode(), item));
        }
        if (capabilityRecurrence != null && capabilityRecurrence.reasons() != null) {
            capabilityRecurrence.reasons().forEach(item -> steps.put("CAPABILITY_RECURRENCE_" + item.hashCode(), item));
        }
        if (capabilitySuppression != null && capabilitySuppression.reasons() != null) {
            capabilitySuppression.reasons().forEach(item -> steps.put("CAPABILITY_SUPPRESSION_" + item.hashCode(), item));
        }
        if (trustZone != null && trustZone.reasons() != null) {
            trustZone.reasons().forEach(item -> steps.put("TRUST_ZONE_" + item.hashCode(), item));
        }
        if (evidenceProvenance != null && evidenceProvenance.reasons() != null) {
            evidenceProvenance.reasons().forEach(item -> steps.put("EVIDENCE_PROVENANCE_" + item.hashCode(), item));
        }
        if (knowledgeCoverage != null && knowledgeCoverage.reasons() != null) {
            knowledgeCoverage.reasons().forEach(item -> steps.put("KNOWLEDGE_" + item.hashCode(), item));
        }
        if (preConsciousFrame != null && preConsciousFrame.nextActions() != null) {
            preConsciousFrame.nextActions().forEach(item -> steps.put("PRE_CONSCIOUS_" + item.hashCode(), item));
        }
        if (council != null) {
            council.forEach(item -> steps.put(String.valueOf(item.get("virtualTrend")), String.valueOf(item.get("action"))));
        }
        if (validation != null && validation.missingEvidence() != null) {
            validation.missingEvidence().forEach(item -> steps.put("MISSING_" + item.hashCode(), "Sanear evidência faltante: " + item));
        }
        if (guard != null && "BLOCKED".equalsIgnoreCase(guard.status())) {
            steps.put("GROUNDING", "Reforçar grounding antes de emitir artigo, precedente, súmula ou tema.");
        }
        return List.copyOf(steps.values());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String resolveConversationId(LegalAiConversationRequest request) {
        String value = request == null ? null : request.conversationId();
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
