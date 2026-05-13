package com.tcc.pjb.backend.controller.leilao;

import com.tcc.pjb.backend.model.dto.leilao.LeilaoAnaliseLanceRequest;
import com.tcc.pjb.backend.model.dto.leilao.LeilaoAvaliacaoBemRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.leilao.surface.LeilaoAnalyticsSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leilao/analytics")
public class LeilaoJudicialAnalyticsController {

    private final LeilaoAnalyticsSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public LeilaoJudicialAnalyticsController(LeilaoAnalyticsSurfaceFacadeService facadeService,
                                             CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/avaliar-bem")
    @PreAuthorize("hasAnyRole('LEILOEIRO_JUDICIAL','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarBem(@Valid @RequestBody LeilaoAvaliacaoBemRequest request,
                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leilao_analytics_avaliar_bem", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.avaliarBem(request));
    }

    @PostMapping("/analisar-lance")
    @PreAuthorize("hasAnyRole('LEILOEIRO_JUDICIAL','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> analisarLance(@Valid @RequestBody LeilaoAnaliseLanceRequest request,
                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leilao_analytics_analisar_lance", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.analisarLance(request));
    }
}
