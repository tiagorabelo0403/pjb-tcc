package com.tcc.pjb.backend.controller.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.trabalhista.TrabalhistaVerbaRescisoriaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialApiObservabilityService;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(value = "/api/v1/processual/trabalhista", produces = MediaType.APPLICATION_JSON_VALUE)
public class TrabalhistaCalculoLegacyController {

    private final ProcessualOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final CalculoJudicialApiObservabilityService observabilityService;

    public TrabalhistaCalculoLegacyController(ProcessualOperationalSurfaceFacadeService facadeService,
                                              CapabilityRateLimiter rateLimiter,
                                              CalculoJudicialApiObservabilityService observabilityService) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.observabilityService = observabilityService;
    }

    @PostMapping("/verbas-rescisorias")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','DEFENSOR_PUBLICO','JUIZ_TRABALHISTA','MAGISTRADO','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceSnapshotResponse> calcular(@Valid @RequestBody TrabalhistaVerbaRescisoriaRequest request,
                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "trabalhista_verbas_rescisorias", ApiVersion.V1);
        HttpHeaders headers = new HttpHeaders();
        observabilityService.apply(headers, observabilityService.legacyTrabalhista());
        return ResponseEntity.ok().headers(headers).body(facadeService.calcularVerbasRescisorias(request));
    }
}
