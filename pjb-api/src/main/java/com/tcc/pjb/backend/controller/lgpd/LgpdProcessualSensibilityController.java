package com.tcc.pjb.backend.controller.lgpd;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.lgpd.surface.LgpdProcessualSurfaceFacadeService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lgpd/processual")
public class LgpdProcessualSensibilityController {

    private final LgpdProcessualSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public LgpdProcessualSensibilityController(LgpdProcessualSurfaceFacadeService facadeService,
                                               CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/processos/{processoId}/classificacao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> classificar(@PathVariable Long processoId,
                                                               Authentication authentication) {
        enforce(authentication, "lgpd_processual_classificacao");
        return ResponseEntity.ok(facadeService.classificar(processoId));
    }

    @GetMapping("/processos/{processoId}/retencao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> politicaRetencao(@PathVariable Long processoId,
                                                                    Authentication authentication) {
        enforce(authentication, "lgpd_processual_retencao");
        return ResponseEntity.ok(facadeService.politicaRetencao(processoId));
    }

    @GetMapping("/processos/{processoId}/acessos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> auditarAcessos(@PathVariable Long processoId,
                                                                  Authentication authentication) {
        enforce(authentication, "lgpd_processual_acessos");
        return ResponseEntity.ok(facadeService.auditarAcessos(processoId));
    }

    @GetMapping("/processos/{processoId}/relatorio-impacto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> relatorioImpacto(@PathVariable Long processoId,
                                                                    Authentication authentication) {
        enforce(authentication, "lgpd_processual_relatorio");
        return ResponseEntity.ok(facadeService.relatorioImpacto(processoId));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
