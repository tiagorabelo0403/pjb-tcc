package com.tcc.pjb.backend.controller.psicossocial;

import com.tcc.pjb.backend.model.dto.psicossocial.PsicossocialAnaliseLaudoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.psicossocial.surface.PsicossocialSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/psicossocial/riscos")
public class PsicossocialRiscoController {

    private final PsicossocialSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public PsicossocialRiscoController(PsicossocialSurfaceFacadeService facadeService,
                                       CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/analisar")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> analisar(@Valid @RequestBody PsicossocialAnaliseLaudoRequest request,
                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_riscos_analisar", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.analisar(request));
    }
}
