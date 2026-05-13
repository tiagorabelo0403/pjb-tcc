package com.tcc.pjb.backend.controller.processual.recursal.ia;

import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.recursal.ia.RecursalIaConferenciaService;
import com.tcc.pjb.backend.service.recursal.RecursalContextualAccessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/recursal/ia/conferencia")
public class RecursalIaConferenciaController {

    private final RecursalIaConferenciaService service;
    private final CapabilityRateLimiter rateLimiter;
    private final RecursalContextualAccessService accessService;

    public RecursalIaConferenciaController(RecursalIaConferenciaService service,
                                           CapabilityRateLimiter rateLimiter,
                                           RecursalContextualAccessService accessService) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.accessService = accessService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecursalIaConferenciaResponse> conferir(@Valid @RequestBody RecursalIaConferenciaRequest request,
                                                                  Authentication authentication) {
        accessService.requireIaConferenciaAccess(request);
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "recursal_ia_conferencia", ApiVersion.V1);
        return ResponseEntity.ok(service.conferir(request));
    }
}
