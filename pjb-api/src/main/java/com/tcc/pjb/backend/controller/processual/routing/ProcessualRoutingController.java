package com.tcc.pjb.backend.controller.processual.routing;

import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingRequest;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/routing")
public class ProcessualRoutingController {

    private final ProcessualOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public ProcessualRoutingController(ProcessualOperationalSurfaceFacadeService facadeService,
                                       CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/diagnostico")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalProcessRoutingResponse> diagnosticar(@Valid @RequestBody NationalProcessRoutingRequest request,
                                                                       Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "processual_routing_diagnostico", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.diagnosticarRouting(request));
    }
}
