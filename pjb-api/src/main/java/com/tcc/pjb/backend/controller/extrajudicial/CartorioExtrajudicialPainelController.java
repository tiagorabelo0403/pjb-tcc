package com.tcc.pjb.backend.controller.extrajudicial;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.extrajudicial.CartorioExtrajudicialPainelService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
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

@RestController
@RequestMapping("/api/v1/extrajudicial")
public class CartorioExtrajudicialPainelController {

    private final CartorioExtrajudicialPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public CartorioExtrajudicialPainelController(CartorioExtrajudicialPainelService service,
                                                 InstitutionalPainelSurfaceFacadeService facadeService,
                                                 CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<PerfilDashboardPayload.CartorioExtrajudicialPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "extrajudicial_painel", ApiVersion.V1);
        PerfilDashboardPayload.CartorioExtrajudicialPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }



    @GetMapping("/certidoes/pendentes")
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<SurfaceCollectionResponse> certidoes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "extrajudicial_certidoes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.extrajudicialCertidoesPendentes());
    }

    @GetMapping("/indisponibilidades/pendentes")
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<SurfaceCollectionResponse> indisponibilidades(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "extrajudicial_indisponibilidades", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.extrajudicialIndisponibilidadesPendentes());
    }

    @GetMapping("/monitoramento-operacional")
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<SurfaceSnapshotResponse> monitoramento(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "extrajudicial_monitoramento_operacional", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.extrajudicialMonitoramentoOperacional());
    }

    @GetMapping("/atos/pendentes")
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<SurfaceCollectionResponse> atos(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "extrajudicial_atos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.extrajudicialAtosPendentes());
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasRole('CARTORIO_EXTRAJUDICIAL')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "cartorio_extrajudicial_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("CARTORIO_EXTRAJUDICIAL"));
    }

}
