package com.tcc.pjb.backend.controller.tutela;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.tutela.TutelaProvisioriaChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tutela/provisoria")
public class TutelaProvisioriaChecklistController {

    private final TutelaProvisioriaChecklistService service;
    private final CapabilityRateLimiter rateLimiter;

    public TutelaProvisioriaChecklistController(TutelaProvisioriaChecklistService service,
                                               CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<TutelaProvisioriaChecklistService.TutelaProvisioriaResult> avaliar(
            @Valid @RequestBody TutelaProvisioriaChecklistService.TutelaProvisioriaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "tutela_provisoria_checklist", ApiVersion.V1);
        return ResponseEntity.ok(service.avaliar(input));
    }
}
