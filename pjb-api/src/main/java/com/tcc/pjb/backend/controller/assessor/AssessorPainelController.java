package com.tcc.pjb.backend.controller.assessor;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.assessor.AssessorPainelService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessor")
public class AssessorPainelController {

    private final AssessorPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public AssessorPainelController(AssessorPainelService service,
                                    InstitutionalPainelSurfaceFacadeService facadeService,
                                    CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<PerfilDashboardPayload.AssessorPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_painel", ApiVersion.V1);
        PerfilDashboardPayload.AssessorPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(15)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/gabinete/minutas")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceCollectionResponse> minutasGabinete(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_minutas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.assessorMinutasGabinete());
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceCollectionResponse> agenda(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_agenda", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.assessorAgenda());
    }

    @GetMapping("/processos/despacho-pendente")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceCollectionResponse> processosDespachoPendente(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_despacho", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.assessorDespachosPendentes());
    }


    @GetMapping("/gabinete/processos/{processoId}/matriz")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse> matrizProcesso(@PathVariable Long processoId,
                                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_matriz_processo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.assessorMatrizProcesso(processoId));
    }

    @GetMapping("/gabinete/processos/{processoId}/handoff")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse> handoffProcesso(@PathVariable Long processoId,
                                                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_handoff_processo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.assessorHandoffProcesso(processoId));
    }

    @PostMapping("/gabinete/processos/{processoId}/devolver-gabinete")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse> devolverParaGabinete(@PathVariable Long processoId,
                                                                                                                    @RequestHeader(name = "X-Observacao", required = false) String observacao,
                                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_devolver_gabinete", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.assessorDevolverParaGabinete(processoId, observacao));
    }

    @GetMapping("/gabinete/processos/{processoId}/guardrails")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse> guardrailsProcesso(@PathVariable Long processoId,
                                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "assessor_guardrails_processo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.assessorGuardrailsProcesso(processoId));
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "assistant_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("ASSESSOR"));
    }

}
