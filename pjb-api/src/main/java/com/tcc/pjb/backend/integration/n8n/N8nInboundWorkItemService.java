package com.tcc.pjb.backend.integration.n8n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyBeginResult;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nWorkItemTriggerRequest;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nWorkItemTriggerResponse;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationRequest;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationResponse;
import com.tcc.pjb.backend.service.workitem.ProcessWorkItemAutomationService;
import jakarta.validation.Validator;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class N8nInboundWorkItemService {

    private final N8nIntegrationProperties properties;
    private final N8nSignatureService signatureService;
    private final ProcessWorkItemAutomationService automationService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider;
    private final ObjectProvider<AuditLedgerService> auditLedgerServiceProvider;

    public N8nInboundWorkItemService(N8nIntegrationProperties properties,
                                     N8nSignatureService signatureService,
                                     ProcessWorkItemAutomationService automationService,
                                     ObjectMapper objectMapper,
                                     Validator validator,
                                     ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider,
                                     ObjectProvider<AuditLedgerService> auditLedgerServiceProvider) {
        this.properties = Objects.requireNonNull(properties);
        this.signatureService = Objects.requireNonNull(signatureService);
        this.automationService = Objects.requireNonNull(automationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.validator = Objects.requireNonNull(validator);
        this.requestIdempotencyServiceProvider = Objects.requireNonNull(requestIdempotencyServiceProvider);
        this.auditLedgerServiceProvider = Objects.requireNonNull(auditLedgerServiceProvider);
    }

    public N8nWorkItemTriggerResponse handleSignedWorkItemGeneration(String rawBody,
                                                                     String signature,
                                                                     String traceIdHeader) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Integração n8n desabilitada.");
        }
        String payload = rawBody == null ? "" : rawBody;
        if (!signatureService.matches(properties.getInboundSecret(), payload, signature)) {
            throw new IllegalArgumentException("Assinatura HMAC do n8n inválida.");
        }
        N8nWorkItemTriggerRequest request = readRequest(payload);
        validate(request);
        String traceId = traceIdHeader != null && !traceIdHeader.isBlank() ? traceIdHeader.trim() : request.requestId().trim();
        String requestHash = Hashes.sha256Hex("n8n:workitems:" + payload);
        RequestIdempotencyService idempotencyService = requestIdempotencyServiceProvider.getIfAvailable();
        if (idempotencyService != null) {
            RequestIdempotencyBeginResult begin = idempotencyService.begin("N8N_WORKITEM_GENERATE", requestHash, Duration.ofMinutes(5));
            if (!begin.created() && begin.isCompleted() && begin.responseJson() != null && !begin.responseJson().isBlank()) {
                return readResponse(begin.responseJson());
            }
        }
        try {
            WorkItemGenerationResponse generation = automationService.generate(new WorkItemGenerationRequest(
                    request.processoId(),
                    Boolean.TRUE.equals(request.force()),
                    request.fase()
            ));
            N8nWorkItemTriggerResponse response = new N8nWorkItemTriggerResponse(
                    request.requestId().trim(),
                    traceId,
                    request.workflowKey(),
                    Instant.now(),
                    generation
            );
            String responseJson = objectMapper.writeValueAsString(response);
            if (idempotencyService != null) {
                idempotencyService.complete(requestHash, "N8N_WORKITEM_GENERATION", String.valueOf(request.processoId()), Hashes.sha256Hex(responseJson), responseJson);
            }
            AuditLedgerService ledger = auditLedgerServiceProvider.getIfAvailable();
            if (ledger != null) {
                ledger.appendSafely("N8N_WORKITEM_GENERATED", "N8N", request.requestId(), requestHash, "processoId=" + request.processoId());
            }
            return response;
        } catch (RuntimeException e) {
            if (idempotencyService != null) {
                idempotencyService.fail(requestHash);
            }
            throw e;
        } catch (Exception e) {
            if (idempotencyService != null) {
                idempotencyService.fail(requestHash);
            }
            throw new IllegalStateException("Falha ao processar requisição n8n para workitems.", e);
        }
    }

    private N8nWorkItemTriggerRequest readRequest(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, N8nWorkItemTriggerRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload do n8n inválido para geração de workitems.", e);
        }
    }

    private N8nWorkItemTriggerResponse readResponse(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, N8nWorkItemTriggerResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao restaurar resposta idempotente do n8n.", e);
        }
    }

    private void validate(N8nWorkItemTriggerRequest request) {
        Set<String> violations = validator.validate(request).stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.toSet());
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Payload n8n inválido: " + String.join("; ", violations));
        }
    }
}
