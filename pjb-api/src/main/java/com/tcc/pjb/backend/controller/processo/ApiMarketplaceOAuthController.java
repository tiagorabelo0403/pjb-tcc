package com.tcc.pjb.backend.controller.processo;

import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthClientRegistrationRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthIntrospectionRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthRevocationRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthTokenRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.api.surface.MarketplaceSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace/oauth/v1")
public class ApiMarketplaceOAuthController {

    private final MarketplaceSurfaceFacadeService facadeService;

    public ApiMarketplaceOAuthController(MarketplaceSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceCollectionResponse> listarClientes() {
        return ResponseEntity.ok(facadeService.listarClientesOauth());
    }

    @PostMapping("/clients")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceSnapshotResponse> registrarCliente(@Valid @RequestBody MarketplaceOauthClientRegistrationRequest request,
                                                                    HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarClienteOauth(request, resolveIp(servletRequest)));
    }

    @PostMapping("/token")
    public ResponseEntity<SurfaceSnapshotResponse> token(@Valid @RequestBody MarketplaceOauthTokenRequest request,
                                                         HttpServletRequest servletRequest) {
        return ResponseEntity.ok(facadeService.emitirTokenOauth(request, resolveIp(servletRequest)));
    }

    @PostMapping("/introspect")
    public ResponseEntity<SurfaceSnapshotResponse> introspect(@RequestBody MarketplaceOauthIntrospectionRequest request,
                                                              HttpServletRequest servletRequest) {
        return ResponseEntity.ok(facadeService.introspectOauth(request, resolveIp(servletRequest)));
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceActionResponse> revoke(@RequestBody MarketplaceOauthRevocationRequest request,
                                                        HttpServletRequest servletRequest) {
        return ResponseEntity.ok(facadeService.revokeOauth(request, resolveIp(servletRequest)));
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
