package com.tcc.pjb.backend.controller.processual.integration.intertribunal;

import com.tcc.pjb.backend.model.dto.processual.integration.intertribunal.LitispendenciaIntertribunalRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
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
@RequestMapping("/api/v1/processual/litispendencia")
public class LitispendenciaIntertribunalController {

    private final ProcessualOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public LitispendenciaIntertribunalController(ProcessualOperationalSurfaceFacadeService facadeService,
                                                 CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/analisar")
    @PreAuthorize("hasAnyRole('ADVOGADO','SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO','PROCURADOR','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceSnapshotResponse> analisar(@Valid @RequestBody LitispendenciaIntertribunalRequest request,
                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "litispendencia_intertribunal_analisar", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.analisarLitispendencia(request));
    }
}
