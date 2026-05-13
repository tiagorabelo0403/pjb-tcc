package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAuditTrailService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer.LegalConversationSanitizationResult;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTraceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiTraceDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationTraceService {

    private final LegalAuditTrailService auditTrailService;

    public LegalAiConversationTraceService(LegalAuditTrailService auditTrailService) {
        this.auditTrailService = Objects.requireNonNull(auditTrailService, "auditTrailService");
    }

    public LegalAiConversationTraceSnapshot open(String conversationId,
                                                 LegalAiConversationRequest request,
                                                 String version,
                                                 String capability,
                                                 LegalAiTraceDescriptor descriptor,
                                                 LegalAiConversationMemorySnapshot memorySnapshot,
                                                 LegalConversationSanitizationResult sanitization,
                                                 LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                 LegalAiConversationToolScopeSnapshot toolScope) {
        Instant startedAt = Instant.now();
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("conversationId", conversationId);
        diagnostics.put("startedAt", startedAt.toString());
        diagnostics.put("version", version);
        diagnostics.put("capability", capability);
        diagnostics.put("messageLength", request == null || request.message() == null ? 0 : request.message().length());
        diagnostics.put("attachmentCount", request == null || request.attachments() == null ? 0 : request.attachments().size());
        diagnostics.put("historyCount", request == null || request.history() == null ? 0 : request.history().size());
        diagnostics.put("retainedTurnCount", memorySnapshot == null || memorySnapshot.retainedTurns() == null ? 0 : memorySnapshot.retainedTurns().size());
        diagnostics.put("memoryScopes", memorySnapshot == null || memorySnapshot.scopedMemory() == null ? List.of() : List.copyOf(memorySnapshot.scopedMemory().keySet()));
        diagnostics.putAll(auditTrailService.openDiagnostics(sanitization, documentSecurity, toolScope));
        List<Map<String, Object>> executionTrail = List.of(checkpoint(
                "ORCHESTRATION_OPEN",
                "STARTED",
                List.of("routing", "memory", "trace", "documentSecurity"),
                startedAt
        ));
        return new LegalAiConversationTraceSnapshot(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                descriptor == null ? null : descriptor.lane(),
                "STARTED",
                descriptor == null || descriptor.requiredAuditFields() == null ? List.of() : List.copyOf(descriptor.requiredAuditFields()),
                ImmutableViewSupport.map(diagnostics),
                executionTrail
        );
    }

    public LegalAiConversationTraceSnapshot close(LegalAiConversationTraceSnapshot opened,
                                                  LegalValidationResponse validation,
                                                  LegalHallucinationGuardResponse guard,
                                                  List<Map<String, Object>> council,
                                                  LegalAiConversationApprovalSnapshot approval,
                                                  LegalConversationSanitizationResult sanitization,
                                                  LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                  LegalAiConversationToolScopeSnapshot toolScope) {
        if (opened == null) {
            return null;
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(opened.diagnostics() == null ? Map.of() : opened.diagnostics());
        Instant finishedAt = Instant.now();
        diagnostics.put("finishedAt", finishedAt.toString());
        diagnostics.put("durationMillis", elapsedMillis(opened));
        diagnostics.put("approvalStatus", approval == null ? null : approval.status());
        diagnostics.put("validationStatus", validation == null ? null : validation.status());
        diagnostics.put("hallucinationStatus", guard == null ? null : guard.status());
        diagnostics.put("symbolicExecutionStatus", validation == null || validation.trace() == null ? null : validation.trace().get("symbolicExecutionStatus"));
        diagnostics.put("councilSize", council == null ? 0 : council.size());
        diagnostics.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        diagnostics.put("toolScopeStatus", toolScope == null ? null : toolScope.status());
        List<Map<String, Object>> trail = new ArrayList<>(opened.executionTrail() == null ? List.of() : opened.executionTrail());
        trail.addAll(auditTrailService.securityCheckpoints(sanitization, documentSecurity, toolScope, approval, finishedAt));
        trail.add(checkpoint(
                "SYMBOLIC_VALIDATION",
                validation == null ? "NOT_EXECUTED" : validation.status(),
                validation == null ? List.of() : validation.symbolicEngines(),
                finishedAt
        ));
        trail.add(checkpoint(
                "GROUNDING_GUARD",
                guard == null ? "NOT_EXECUTED" : guard.status(),
                guard == null ? List.of() : guard.blockedReasons(),
                finishedAt
        ));
        trail.add(checkpoint(
                "VIRTUAL_TRENDS_COUNCIL",
                council == null || council.isEmpty() ? "EMPTY" : "RESOLVED",
                council == null ? List.of() : council.stream().map(item -> String.valueOf(item.get("virtualTrend"))).toList(),
                finishedAt
        ));
        return new LegalAiConversationTraceSnapshot(
                opened.traceId(),
                opened.turnId(),
                opened.lane(),
                "COMPLETED",
                opened.auditFields(),
                ImmutableViewSupport.map(diagnostics),
                List.copyOf(trail)
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

    private long elapsedMillis(LegalAiConversationTraceSnapshot opened) {
        if (opened.diagnostics() == null) {
            return 0L;
        }
        Object startedAt = opened.diagnostics().get("startedAt");
        if (startedAt == null) {
            return 0L;
        }
        Instant start = Instant.parse(String.valueOf(startedAt));
        return Duration.between(start, Instant.now()).toMillis();
    }
}
