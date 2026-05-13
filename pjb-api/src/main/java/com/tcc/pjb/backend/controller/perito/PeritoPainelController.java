package com.tcc.pjb.backend.controller.perito;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import com.tcc.pjb.backend.service.perito.PeritoPainelService;
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
@RequestMapping("/api/v1/perito")
public class PeritoPainelController {

    private static final String PERITO_ROLES = "hasAnyRole('PERITO','PERITO_CRIMINAL','PERITO_AMBIENTAL','PERITO_CONTABIL','PERITO_ENGENHARIA','PERITO_DIGITAL','PERITO_INSS','PERITO_MEDICO','ASSISTENTE_TECNICO','PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')";
    private final PeritoPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public PeritoPainelController(PeritoPainelService service,
                                  InstitutionalPainelSurfaceFacadeService facadeService,
                                  CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<PerfilDashboardPayload.PeritoPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_painel", ApiVersion.V1);
        PerfilDashboardPayload.PeritoPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/nomeacoes")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> nomeacoes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_nomeacoes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.peritoNomeacoes());
    }

    @PostMapping("/nomeacoes/{nomeacaoId}/aceite")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<SurfaceActionResponse> aceitarNomeacao(@PathVariable Long nomeacaoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_aceite", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.peritoAceitarNomeacao(nomeacaoId));
    }

    @PostMapping("/nomeacoes/{nomeacaoId}/recusa")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<SurfaceActionResponse> recusarNomeacao(@PathVariable Long nomeacaoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_recusa", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.peritoRecusarNomeacao(nomeacaoId, request));
    }

    @GetMapping("/laudos/pendentes")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> laudosPendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_laudos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.peritoLaudosPendentes());
    }

    @PostMapping("/laudos/{processoId}/entrega")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<SurfaceActionResponse> entregarLaudo(@PathVariable Long processoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_entrega_laudo", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.peritoEntregarLaudo(processoId, request));
    }

    @GetMapping("/prazos/vencendo")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> prazosVencendo(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_prazos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.peritoPrazosVencendo());
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize(PERITO_ROLES)
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "perito_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("PERITO"));
    }

}
