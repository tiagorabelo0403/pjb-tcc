package com.tcc.pjb.backend.controller.mandadoseguranca;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.acaoconstitucional.MandadoSegurancaReadinessService;
import com.tcc.pjb.backend.service.mandadoseguranca.MandadoSegurancaChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mandadoseguranca")
public class MandadoSegurancaController {

    private final MandadoSegurancaReadinessService readinessService;
    private final MandadoSegurancaChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public MandadoSegurancaController(MandadoSegurancaReadinessService readinessService,
                                      MandadoSegurancaChecklistService checklistService,
                                      CapabilityRateLimiter rateLimiter) {
        this.readinessService = readinessService;
        this.checklistService = checklistService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/readiness")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<MandadoSegurancaReadinessService.MandadoSegurancaResult> readiness(
            @Valid @RequestBody MandadoSegurancaReadinessService.MandadoSegurancaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "ms_readiness", ApiVersion.V1);
        return ResponseEntity.ok(readinessService.avaliar(input));
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<MandadoSegurancaChecklistService.MandadoSegurancaResult> checklist(
            @Valid @RequestBody MandadoSegurancaChecklistService.MandadoSegurancaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "ms_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
