package com.tcc.pjb.backend.controller.secretariat.security;


import com.tcc.pjb.backend.model.dto.profile.operational.BreakGlassAcessoRequest;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.security.breakglass.InstitutionalBreakGlassAccessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE)
@Validated
public class SecretariatBreakGlassController {

    private static final String ROLES = "hasAnyRole('SERVIDOR','SERVIDOR_FORUM')";

    private final InstitutionalBreakGlassAccessService service;
    private final CapabilityRateLimiter rateLimiter;

    public SecretariatBreakGlassController(InstitutionalBreakGlassAccessService service,
                                           CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_BREAK_GLASS)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(@PathVariable Long processoId,
                                                            Authentication authentication) {
        enforce(authentication, "secretaria_break_glass_snapshot");
        return ResponseEntity.ok(service.snapshot(processoId));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_BREAK_GLASS)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> activate(@PathVariable Long processoId,
                                                          @Valid @RequestBody BreakGlassAcessoRequest request,
                                                          Authentication authentication) {
        enforce(authentication, "secretaria_break_glass_ativar");
        return ResponseEntity.ok(service.activate(processoId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, capability, ApiVersion.V1);
    }
}
