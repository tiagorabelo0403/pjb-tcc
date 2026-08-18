package com.tcc.pjb.backend.controller.eleitoral;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.acceleration.eleitoral.EleitoralPrazoCriticoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eleitoral")
public class EleitoralController {

    private final EleitoralPrazoCriticoService prazoCriticoService;
    private final CapabilityRateLimiter rateLimiter;

    public EleitoralController(EleitoralPrazoCriticoService prazoCriticoService,
                               CapabilityRateLimiter rateLimiter) {
        this.prazoCriticoService = prazoCriticoService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/prazo-critico")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<List<EleitoralPrazoCriticoService.AlertaEleitoral>> prazoCritico(
            @Valid @RequestBody List<EleitoralPrazoCriticoService.ProcessoEleitoralItem> processos,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "eleitoral_prazo_critico", ApiVersion.V1);
        return ResponseEntity.ok(prazoCriticoService.identificarCriticos(processos));
    }
}
