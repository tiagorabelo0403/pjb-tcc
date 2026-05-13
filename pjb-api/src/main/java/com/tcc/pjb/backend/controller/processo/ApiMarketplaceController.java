package com.tcc.pjb.backend.controller.processo;

import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceProtocoloRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceProtocoloResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.api.oauth.MarketplaceOAuth2Service;
import com.tcc.pjb.backend.service.api.surface.MarketplaceSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace/v1")
@PreAuthorize("isAuthenticated()")
public class ApiMarketplaceController {

    private final MarketplaceSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final MarketplaceOAuth2Service marketplaceOAuth2Service;

    public ApiMarketplaceController(MarketplaceSurfaceFacadeService facadeService,
                                    CapabilityRateLimiter rateLimiter,
                                    MarketplaceOAuth2Service marketplaceOAuth2Service) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.marketplaceOAuth2Service = marketplaceOAuth2Service;
    }

    @PostMapping("/processos")
    public ResponseEntity<MarketplaceProtocoloResponse> protocolar(@Valid @RequestBody MarketplaceProtocoloRequest request,
                                                                   Authentication authentication,
                                                                   HttpServletRequest servletRequest) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "marketplace_protocolar_processo", ApiVersion.V1);
        String clientId = authentication != null && authentication.getName() != null ? authentication.getName() : null;
        if (clientId == null || clientId.isBlank()) {
            clientId = marketplaceOAuth2Service.authorizeHttpRequest(servletRequest, "processos:protocolar").clientId();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.protocolar(request, clientId));
    }
}
