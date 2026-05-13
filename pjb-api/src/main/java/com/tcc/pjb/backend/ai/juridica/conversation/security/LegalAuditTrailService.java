package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer.LegalConversationSanitizationResult;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAuditTrailService {

    public Map<String, Object> openDiagnostics(LegalConversationSanitizationResult sanitization,
                                               LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                               LegalAiConversationToolScopeSnapshot toolScope) {
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("sanitizationStatus", sanitization == null || sanitization.snapshot() == null ? null : sanitization.snapshot().status());
        diagnostics.put("promptInjectionDetected", sanitization != null && sanitization.snapshot() != null && sanitization.snapshot().promptInjectionDetected());
        diagnostics.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        diagnostics.put("toolScopeStatus", toolScope == null ? null : toolScope.status());
        diagnostics.put("quarantinedAttachmentCount", documentSecurity == null || documentSecurity.quarantinedAttachments() == null ? 0 : documentSecurity.quarantinedAttachments().size());
        diagnostics.put("blockedSourceCount", documentSecurity == null || documentSecurity.blockedSources() == null ? 0 : documentSecurity.blockedSources().size());
        return ImmutableViewSupport.map(diagnostics);
    }

    public List<Map<String, Object>> securityCheckpoints(LegalConversationSanitizationResult sanitization,
                                                         LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                         LegalAiConversationToolScopeSnapshot toolScope,
                                                         LegalAiConversationApprovalSnapshot approval,
                                                         Instant occurredAt) {
        return List.of(
                checkpoint(
                        "CONTEXT_SANITIZATION",
                        sanitization == null || sanitization.snapshot() == null ? "NOT_EXECUTED" : sanitization.snapshot().status(),
                        sanitization == null || sanitization.snapshot() == null ? List.of() : sanitization.snapshot().alerts(),
                        occurredAt
                ),
                checkpoint(
                        "DOCUMENT_QUARANTINE",
                        documentSecurity == null ? "NOT_EXECUTED" : documentSecurity.status(),
                        documentSecurity == null ? List.of() : documentSecurity.alerts(),
                        occurredAt
                ),
                checkpoint(
                        "TOOL_SCOPE_POLICY",
                        toolScope == null ? "NOT_EXECUTED" : toolScope.status(),
                        toolScope == null ? List.of() : toolScope.reasons(),
                        occurredAt
                ),
                checkpoint(
                        "SENSITIVE_ACTION_APPROVAL",
                        approval == null ? "AUTO_READONLY" : approval.status(),
                        approval == null ? List.of() : approval.checkpoints(),
                        occurredAt
                )
        );
    }

    private Map<String, Object> checkpoint(String step, String status, List<String> signals, Instant occurredAt) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("step", step);
        out.put("status", status);
        out.put("signals", signals == null ? List.of() : List.copyOf(signals));
        out.put("occurredAt", occurredAt == null ? null : occurredAt.toString());
        return ImmutableViewSupport.map(out);
    }
}
