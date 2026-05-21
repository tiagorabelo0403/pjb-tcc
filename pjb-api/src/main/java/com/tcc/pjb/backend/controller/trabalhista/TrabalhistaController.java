package com.tcc.pjb.backend.controller.trabalhista;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaDejtPublicationReadinessService;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaExecucaoFastTrackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trabalhista")
public class TrabalhistaController {

    private final TrabalhistaDejtPublicationReadinessService dejtService;
    private final TrabalhistaExecucaoFastTrackService fastTrackService;
    private final CapabilityRateLimiter rateLimiter;

    public TrabalhistaController(TrabalhistaDejtPublicationReadinessService dejtService,
                                 TrabalhistaExecucaoFastTrackService fastTrackService,
                                 CapabilityRateLimiter rateLimiter) {
        this.dejtService = dejtService;
        this.fastTrackService = fastTrackService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/dejt-readiness")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<TrabalhistaDejtPublicationReadinessService.DejtReadiness> dejtReadiness(
            @Valid @RequestBody TrabalhistaDejtPublicationReadinessService.DejtInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "trabalhista_dejt_readiness", ApiVersion.V1);
        return ResponseEntity.ok(dejtService.avaliar(input));
    }

    @PostMapping("/execucao-fast-track")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<TrabalhistaExecucaoFastTrackService.FastTrackElegibilidade> execucaoFastTrack(
            @Valid @RequestBody TrabalhistaExecucaoFastTrackService.ExecucaoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "trabalhista_execucao_fast_track", ApiVersion.V1);
        return ResponseEntity.ok(fastTrackService.avaliar(input));
    }
}
