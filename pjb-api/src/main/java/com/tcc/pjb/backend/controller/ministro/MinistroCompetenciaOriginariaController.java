package com.tcc.pjb.backend.controller.ministro;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.ministro.surface.MinistroCourtSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ministro/competencias-originarias")
public class MinistroCompetenciaOriginariaController {

    private final MinistroCourtSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public MinistroCompetenciaOriginariaController(MinistroCourtSurfaceFacadeService facadeService,
                                                   CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/catalogo")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> catalogo(Authentication authentication) {
        enforce(authentication, "ministro_competencia_originaria_catalogo");
        return ResponseEntity.ok(facadeService.catalogoCompetenciasOriginarias());
    }

    @GetMapping("/sugerir")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> sugerir(@RequestParam String classe,
                                                           Authentication authentication) {
        enforce(authentication, "ministro_competencia_originaria_sugerir");
        return ResponseEntity.ok(facadeService.sugerirCompetenciaOriginaria(classe));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
