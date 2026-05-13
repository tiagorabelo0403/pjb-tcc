package com.tcc.pjb.backend.controller.procuradoria;

import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvCalculoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.procuradoria.surface.ProcuradoriaOperationalSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/procuradoria/financeiro")
public class PrecatorioRpvController {

    private final ProcuradoriaOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public PrecatorioRpvController(ProcuradoriaOperationalSurfaceFacadeService facadeService,
                                   CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/precatorio-rpv/calcular")
    @PreAuthorize("hasAnyRole('PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','PROCURADOR_GERAL_REPUBLICA')")
    public ResponseEntity<SurfaceSnapshotResponse> calcular(@Valid @RequestBody PrecatorioRpvCalculoRequest request,
                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "procuradoria_precatorio_rpv_calcular", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.calcularPrecatorioRpv(request));
    }
}
