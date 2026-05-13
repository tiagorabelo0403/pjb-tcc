package com.tcc.pjb.backend.controller.processual.prazos;

import com.tcc.pjb.backend.model.dto.processual.prazo.DiaForenseRequest;
import com.tcc.pjb.backend.model.dto.processual.prazo.DiaForenseResponse;
import com.tcc.pjb.backend.model.dto.processual.prazo.PrazoProcessualCalculoRequest;
import com.tcc.pjb.backend.model.dto.processual.prazo.PrazoProcessualCalculoResponse;
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
@RequestMapping("/api/v1/processual/prazos")
public class PrazoProcessualNacionalController {

    private final ProcessualOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public PrazoProcessualNacionalController(ProcessualOperationalSurfaceFacadeService facadeService,
                                             CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/calcular")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrazoProcessualCalculoResponse> calcular(@Valid @RequestBody PrazoProcessualCalculoRequest request,
                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "processual_prazo_calcular", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.calcularPrazo(request));
    }

    @PostMapping("/dia-forense")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiaForenseResponse> analisarDia(@Valid @RequestBody DiaForenseRequest request,
                                                          Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "processual_prazo_dia_forense", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.analisarDiaForense(request));
    }
}
