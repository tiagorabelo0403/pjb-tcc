package com.tcc.pjb.backend.controller.processo;

import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceAdminPlanRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceAdminSubscriptionRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceAdminWebhookRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.api.surface.MarketplaceSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace/v1/admin")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM')")
public class ApiMarketplaceAdminController {

    private final MarketplaceSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public ApiMarketplaceAdminController(MarketplaceSurfaceFacadeService facadeService,
                                         CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/plans")
    public ResponseEntity<SurfaceCollectionResponse> listarPlanos(Authentication authentication) {
        enforce(authentication, "marketplace_admin_planos_listar");
        return ResponseEntity.ok(facadeService.listarPlanos());
    }

    @PostMapping("/plans")
    public ResponseEntity<SurfaceSnapshotResponse> criarPlano(@Valid @RequestBody MarketplaceAdminPlanRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "marketplace_admin_planos_salvar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.criarPlano(request));
    }

    @GetMapping("/clients/{clientId}/subscriptions")
    public ResponseEntity<SurfaceCollectionResponse> listarAssinaturas(@PathVariable String clientId,
                                                                       Authentication authentication) {
        enforce(authentication, "marketplace_admin_assinaturas_listar");
        return ResponseEntity.ok(facadeService.listarAssinaturas(clientId));
    }

    @PostMapping("/clients/{clientId}/subscriptions")
    public ResponseEntity<SurfaceSnapshotResponse> vincularAssinatura(@PathVariable String clientId,
                                                                      @Valid @RequestBody MarketplaceAdminSubscriptionRequest request,
                                                                      Authentication authentication) {
        enforce(authentication, "marketplace_admin_assinaturas_salvar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.vincularAssinatura(clientId, request));
    }

    @GetMapping("/clients/{clientId}/webhooks")
    public ResponseEntity<SurfaceCollectionResponse> listarWebhooks(@PathVariable String clientId,
                                                                    Authentication authentication) {
        enforce(authentication, "marketplace_admin_webhooks_listar");
        return ResponseEntity.ok(facadeService.listarWebhooks(clientId));
    }

    @PostMapping("/clients/{clientId}/webhooks")
    public ResponseEntity<SurfaceSnapshotResponse> registrarWebhook(@PathVariable String clientId,
                                                                    @Valid @RequestBody MarketplaceAdminWebhookRequest request,
                                                                    Authentication authentication) {
        enforce(authentication, "marketplace_admin_webhooks_registrar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarWebhook(clientId, request));
    }

    @GetMapping("/clients/{clientId}/webhook-deliveries")
    public ResponseEntity<SurfaceCollectionResponse> listarEntregas(@PathVariable String clientId,
                                                                    Authentication authentication) {
        enforce(authentication, "marketplace_admin_webhooks_entregas");
        return ResponseEntity.ok(facadeService.listarEntregas(clientId));
    }

    @PostMapping("/webhook-deliveries/dispatch")
    public ResponseEntity<SurfaceActionResponse> dispatchPendentes(@RequestParam(defaultValue = "50") int limit,
                                                                   Authentication authentication) {
        enforce(authentication, "marketplace_admin_webhooks_dispatch");
        return ResponseEntity.ok(facadeService.dispatchPendentes(limit));
    }

    @PostMapping("/clients/{clientId}/webhook-deliveries/{deliveryId}/redispatch")
    public ResponseEntity<SurfaceActionResponse> redispatch(@PathVariable String clientId,
                                                            @PathVariable Long deliveryId,
                                                            Authentication authentication) {
        enforce(authentication, "marketplace_admin_webhooks_redispatch");
        return ResponseEntity.ok(facadeService.redispatch(clientId, deliveryId));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
