package com.tcc.pjb.backend.controller.perito;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.perito.LaudoPericialReadinessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/perito")
public class LaudoPericialController {

    private final LaudoPericialReadinessService readinessService;
    private final CapabilityRateLimiter rateLimiter;

    public LaudoPericialController(LaudoPericialReadinessService readinessService,
                                   CapabilityRateLimiter rateLimiter) {
        this.readinessService = readinessService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/laudo-readiness")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<LaudoPericialReadinessService.LaudoReadiness> laudoReadiness(
            @Valid @RequestBody LaudoPericialReadinessService.LaudoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "perito_laudo_readiness", ApiVersion.V1);
        return ResponseEntity.ok(readinessService.avaliar(input));
    }
}
