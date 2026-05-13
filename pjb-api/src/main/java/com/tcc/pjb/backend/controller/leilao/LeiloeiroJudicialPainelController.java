package com.tcc.pjb.backend.controller.leilao;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.leilao.LeiloeiroJudicialPainelService;
import com.tcc.pjb.backend.service.leilao.surface.LeiloeiroPainelSurfaceFacadeService;

@RestController
@RequestMapping("/api/v1/leilao")
public class LeiloeiroJudicialPainelController {

    private final LeiloeiroJudicialPainelService service;
    private final LeiloeiroPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public LeiloeiroJudicialPainelController(LeiloeiroJudicialPainelService service,
                                             LeiloeiroPainelSurfaceFacadeService facadeService,
                                             CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasRole('LEILOEIRO_JUDICIAL')")
    public ResponseEntity<PerfilDashboardPayload.LeiloeiroJudicialPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leilao_painel", ApiVersion.V1);
        PerfilDashboardPayload.LeiloeiroJudicialPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }



    @GetMapping("/editais/pendentes")
    @PreAuthorize("hasRole('LEILOEIRO_JUDICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> editais(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leilao_editais_pendentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.editaisPendentes());
    }

    @GetMapping("/prestacoes-contas")
    @PreAuthorize("hasRole('LEILOEIRO_JUDICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> prestacoesContas(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leilao_prestacoes_contas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.prestacoesContas());
    }

    @GetMapping("/resumo-operacional")
    @PreAuthorize("hasRole('LEILOEIRO_JUDICIAL')")
    public ResponseEntity<SurfaceSnapshotResponse> resumoOperacional(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leilao_resumo_operacional", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.resumoOperacional());
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('LEILOEIRO_JUDICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> pendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leilao_pendentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.pendentes());
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasRole('LEILOEIRO_JUDICIAL')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "leiloeiro_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("LEILOEIRO"));
    }

}
