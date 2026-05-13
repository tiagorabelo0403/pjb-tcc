package com.tcc.pjb.backend.controller.integration;

import com.tcc.pjb.backend.integration.n8n.N8nInboundWorkItemService;
import com.tcc.pjb.backend.integration.n8n.N8nIntegrationProperties;
import com.tcc.pjb.backend.integration.n8n.N8nWorkflowDispatchService;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nDispatchRequest;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nDispatchResponse;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nIntegrationProfileResponse;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nWorkItemTriggerResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/n8n")
@Validated
public class N8nIntegrationController {

    private final N8nWorkflowDispatchService dispatchService;
    private final N8nInboundWorkItemService inboundWorkItemService;
    private final N8nIntegrationProperties properties;

    public N8nIntegrationController(N8nWorkflowDispatchService dispatchService,
                                    N8nInboundWorkItemService inboundWorkItemService,
                                    N8nIntegrationProperties properties) {
        this.dispatchService = dispatchService;
        this.inboundWorkItemService = inboundWorkItemService;
        this.properties = properties;
    }

    @PostMapping("/events/dispatch")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<N8nDispatchResponse> dispatch(@Valid @RequestBody N8nDispatchRequest request) {
        N8nDispatchResponse response = dispatchService.dispatch(request);
        return ResponseEntity.status(response.accepted() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(response);
    }

    @PostMapping(value = "/workitems/generate", consumes = "application/json", produces = "application/json")
    @PreAuthorize("permitAll()")
    public ResponseEntity<N8nWorkItemTriggerResponse> generateWorkItemsFromN8n(
            @RequestBody String rawBody,
            @RequestHeader(name = "X-PJB-Signature", required = false) String signature,
            @RequestHeader(name = "X-PJB-Trace-Id", required = false) String traceId) {
        return ResponseEntity.ok(inboundWorkItemService.handleSignedWorkItemGeneration(rawBody, signature, traceId));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<N8nIntegrationProfileResponse> profile() {
        return ResponseEntity.ok(new N8nIntegrationProfileResponse(
                properties.isEnabled(),
                properties.getTenant(),
                properties.getBaseUrl(),
                properties.getDispatchPath(),
                properties.getRequestTimeout(),
                properties.getMaxPayloadBytes(),
                properties.isRequireHttps(),
                properties.isAllowLocalHttp(),
                properties.getInboundSecret() != null && !properties.getInboundSecret().isBlank(),
                properties.getDispatchSecret() != null && !properties.getDispatchSecret().isBlank(),
                List.of("queue-mode", "signed-webhook-ingress", "signed-event-egress"),
                List.of("reverse-proxy", "metrics-healthz", "external-secrets", "git-environments")
        ));
    }
}
