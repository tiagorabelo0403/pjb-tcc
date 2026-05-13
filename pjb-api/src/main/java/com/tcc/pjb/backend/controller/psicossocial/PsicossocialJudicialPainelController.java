package com.tcc.pjb.backend.controller.psicossocial;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.PsicossocialParecerRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PsicossocialRelatorioRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import com.tcc.pjb.backend.service.psicossocial.PsicossocialJudicialPainelService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/psicossocial")
public class PsicossocialJudicialPainelController {

    private final PsicossocialJudicialPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public PsicossocialJudicialPainelController(PsicossocialJudicialPainelService service,
                                                InstitutionalPainelSurfaceFacadeService facadeService,
                                                CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<PerfilDashboardPayload.PsicossocialJudicialPayload> painel(Authentication authentication,
                                                                                     @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_painel", ApiVersion.V1);
        PerfilDashboardPayload.PsicossocialJudicialPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }



    @GetMapping("/agenda-tecnica")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> agendaTecnica(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_agenda_tecnica", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.psicossocialAgendaTecnica());
    }

    @GetMapping("/visitas-domiciliares")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> visitasDomiciliares(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_visitas_domiciliares", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.psicossocialVisitasDomiciliares());
    }

    @GetMapping("/resumo-sensibilidade")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<SurfaceSnapshotResponse> resumoSensibilidade(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_resumo_sensibilidade", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.psicossocialResumoSensibilidade());
    }

    @GetMapping("/casos-prioritarios")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> casos(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_casos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.psicossocialCasosPrioritarios());
    }

    @PostMapping("/processos/{processoId}/parecer")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<SurfaceActionResponse> registrarParecer(@PathVariable Long processoId,
                                                                  @Valid @RequestBody PsicossocialParecerRequest request,
                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_parecer", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.psicossocialRegistrarParecer(processoId, request));
    }

    @PostMapping("/processos/{processoId}/relatorio")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<SurfaceActionResponse> entregarRelatorio(@PathVariable Long processoId,
                                                                   @Valid @RequestBody PsicossocialRelatorioRequest request,
                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_relatorio", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.psicossocialEntregarRelatorio(processoId, request));
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasAnyRole('PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "psicossocial_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("PSICOSSOCIAL"));
    }

}
