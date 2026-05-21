package com.tcc.pjb.backend.controller.recuperacaojudicial;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.recuperacaojudicial.RecuperacaoJudicialReadinessService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recuperacao-judicial")
public class RecuperacaoJudicialController {

    private final RecuperacaoJudicialReadinessService readinessService;
    private final CapabilityRateLimiter rateLimiter;

    public RecuperacaoJudicialController(RecuperacaoJudicialReadinessService readinessService,
                                         CapabilityRateLimiter rateLimiter) {
        this.readinessService = Objects.requireNonNull(readinessService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/readiness")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','ADVOGADO','SERVIDOR_FORUM')")
    public ResponseEntity<RecuperacaoJudicialReadinessService.RecuperacaoJudicialResult> readiness(
            @Valid @RequestBody RecuperacaoJudicialReadinessService.RecuperacaoJudicialInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "recuperacao_judicial_readiness", ApiVersion.V1);
        return ResponseEntity.ok(readinessService.avaliar(input));
    }
}
