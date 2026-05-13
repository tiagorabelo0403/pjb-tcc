package com.tcc.pjb.backend.service.intelligence.surface;

import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationEvidenceProvenanceService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationTrustZoneService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDocumentQuarantineService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSourceAllowlist;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalToolScopePolicy;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.surface.LegalAiStructuredSurfaceGovernanceSnapshot;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalAiStructuredSurfaceGovernanceService {

    private final LegalContextSanitizer contextSanitizer;
    private final LegalSourceAllowlist sourceAllowlist;
    private final LegalDocumentQuarantineService documentQuarantineService;
    private final LegalToolScopePolicy toolScopePolicy;
    private final LegalAiConversationTrustZoneService trustZoneService;
    private final LegalAiConversationEvidenceProvenanceService evidenceProvenanceService;

    public LegalAiStructuredSurfaceGovernanceService(LegalContextSanitizer contextSanitizer,
                                                     LegalSourceAllowlist sourceAllowlist,
                                                     LegalDocumentQuarantineService documentQuarantineService,
                                                     LegalToolScopePolicy toolScopePolicy,
                                                     LegalAiConversationTrustZoneService trustZoneService,
                                                     LegalAiConversationEvidenceProvenanceService evidenceProvenanceService) {
        this.contextSanitizer = Objects.requireNonNull(contextSanitizer, "contextSanitizer");
        this.sourceAllowlist = Objects.requireNonNull(sourceAllowlist, "sourceAllowlist");
        this.documentQuarantineService = Objects.requireNonNull(documentQuarantineService, "documentQuarantineService");
        this.toolScopePolicy = Objects.requireNonNull(toolScopePolicy, "toolScopePolicy");
        this.trustZoneService = Objects.requireNonNull(trustZoneService, "trustZoneService");
        this.evidenceProvenanceService = Objects.requireNonNull(evidenceProvenanceService, "evidenceProvenanceService");
    }

    public LegalAiStructuredSurfaceGovernanceSnapshot inspect(LegalAiConversationRequest request,
                                                              String capability,
                                                              String version,
                                                              List<LegalAiToolDescriptor> tools) {
        var sanitization = contextSanitizer.sanitize(request);
        var effectiveRequest = sanitization.request();
        var sourceDecision = sourceAllowlist.evaluate(effectiveRequest);
        var documentSecurity = documentQuarantineService.inspect(effectiveRequest, sanitization, sourceDecision);
        var toolScope = toolScopePolicy.evaluate(effectiveRequest, capability, version, tools, documentSecurity);
        var trustZone = trustZoneService.inspect(effectiveRequest, capability, version, documentSecurity, toolScope, null, null, null);
        var toolScopeWithTrustZone = toolScopePolicy.enrichWithTrustZone(toolScope, trustZone);
        var evidenceProvenance = evidenceProvenanceService.inspect(effectiveRequest, capability, version, documentSecurity, trustZone, toolScopeWithTrustZone);
        var finalToolScope = toolScopePolicy.enrichWithEvidenceProvenance(toolScopeWithTrustZone, evidenceProvenance);
        return new LegalAiStructuredSurfaceGovernanceSnapshot(
                effectiveRequest,
                sanitization.snapshot(),
                documentSecurity,
                finalToolScope,
                trustZone,
                evidenceProvenance
        );
    }
}
