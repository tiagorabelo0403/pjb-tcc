package com.tcc.pjb.backend.controller.curadoria;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.curadoria.CuradorAusentesPainelService;
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
@RequestMapping("/api/v1/curadoria")
public class CuradorAusentesPainelController {

    private final CuradorAusentesPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public CuradorAusentesPainelController(CuradorAusentesPainelService service,
                                           InstitutionalPainelSurfaceFacadeService facadeService,
                                           CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasRole('CURADOR_AUSENTES')")
    public ResponseEntity<PerfilDashboardPayload.CuradorAusentesPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "curadoria_painel", ApiVersion.V1);
        PerfilDashboardPayload.CuradorAusentesPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }



    @GetMapping("/bens")
    @PreAuthorize("hasRole('CURADOR_AUSENTES')")
    public ResponseEntity<SurfaceCollectionResponse> bens(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "curadoria_bens", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.curadoriaBensSobCuradoria());
    }

    @GetMapping("/prestacoes-contas")
    @PreAuthorize("hasRole('CURADOR_AUSENTES')")
    public ResponseEntity<SurfaceCollectionResponse> prestacoesContas(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "curadoria_prestacoes_contas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.curadoriaPrestacoesContas());
    }

    @GetMapping("/risco-patrimonial")
    @PreAuthorize("hasRole('CURADOR_AUSENTES')")
    public ResponseEntity<SurfaceSnapshotResponse> riscoPatrimonial(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "curadoria_risco_patrimonial", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.curadoriaResumoRiscoPatrimonial());
    }

    @GetMapping("/expedientes")
    @PreAuthorize("hasRole('CURADOR_AUSENTES')")
    public ResponseEntity<SurfaceCollectionResponse> expedientes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "curadoria_expedientes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.curadoriaExpedientes());
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasRole('CURADOR_AUSENTES')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "curador_ausentes_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("CURADOR_AUSENTES"));
    }

}
