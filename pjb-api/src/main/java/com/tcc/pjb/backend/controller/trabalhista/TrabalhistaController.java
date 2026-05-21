package com.tcc.pjb.backend.controller.trabalhista;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaDejtPublicationReadinessService;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaExecucaoFastTrackService;
import com.tcc.pjb.backend.service.trabalhista.VerbaRescisoriaCltChecklistService;
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
@RequestMapping("/api/v1/trabalhista")
public class TrabalhistaController {

    private final TrabalhistaDejtPublicationReadinessService dejtService;
    private final TrabalhistaExecucaoFastTrackService fastTrackService;
    private final VerbaRescisoriaCltChecklistService verbaRescisoriaCltChecklistService;
    private final CapabilityRateLimiter rateLimiter;

    public TrabalhistaController(TrabalhistaDejtPublicationReadinessService dejtService,
                                 TrabalhistaExecucaoFastTrackService fastTrackService,
                                 VerbaRescisoriaCltChecklistService verbaRescisoriaCltChecklistService,
                                 CapabilityRateLimiter rateLimiter) {
        this.dejtService = Objects.requireNonNull(dejtService);
        this.fastTrackService = Objects.requireNonNull(fastTrackService);
        this.verbaRescisoriaCltChecklistService = Objects.requireNonNull(verbaRescisoriaCltChecklistService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
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

    @PostMapping("/verbas-rescisorias")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltResult> verbas(
            @Valid @RequestBody VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "trabalhista_verbas_rescisoriias", ApiVersion.V1);
        return ResponseEntity.ok(verbaRescisoriaCltChecklistService.avaliar(input));
    }
}
