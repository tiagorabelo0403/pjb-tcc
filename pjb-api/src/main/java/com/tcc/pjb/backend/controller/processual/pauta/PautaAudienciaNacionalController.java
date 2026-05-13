package com.tcc.pjb.backend.controller.processual.pauta;

import com.tcc.pjb.backend.model.dto.processual.pauta.PautaAudienciaRequest;
import com.tcc.pjb.backend.model.dto.processual.pauta.PautaAudienciaResponse;
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
@RequestMapping("/api/v1/processual/pauta-audiencia")
public class PautaAudienciaNacionalController {

    private final ProcessualOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public PautaAudienciaNacionalController(ProcessualOperationalSurfaceFacadeService facadeService,
                                            CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/avaliar")
    @PreAuthorize("hasAnyRole('JUIZ','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ADMINISTRADOR','ADMIN')")
    public ResponseEntity<PautaAudienciaResponse> avaliar(@Valid @RequestBody PautaAudienciaRequest request,
                                                          Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pauta_audiencia_avaliar", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.avaliarPauta(request));
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasAnyRole('JUIZ','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ADMINISTRADOR','ADMIN')")
    public ResponseEntity<PautaAudienciaResponse> registrar(@Valid @RequestBody PautaAudienciaRequest request,
                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pauta_audiencia_registrar", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.registrarPauta(request));
    }
}
