package com.tcc.pjb.backend.controller.conciliacao;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.conciliacao.ConciliadorMediadorPainelService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conciliacao")
public class ConciliadorMediadorPainelController {

    private static final String CONCILIACAO_ROLES = "hasAnyRole('CONCILIADOR_CEJUSC','MEDIADOR','ARBITRO')";
    private final ConciliadorMediadorPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public ConciliadorMediadorPainelController(ConciliadorMediadorPainelService service,
                                               InstitutionalPainelSurfaceFacadeService facadeService,
                                               CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<PerfilDashboardPayload.ConciliadorMediadorPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliacao_painel", ApiVersion.V1);
        PerfilDashboardPayload.ConciliadorMediadorPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/sessoes/hoje")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> sessoesHoje(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliacao_sessoes_hoje", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.conciliacaoSessoesHoje());
    }

    @GetMapping("/sessoes/pendentes")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> sessoesPendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliacao_sessoes_pendentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.conciliacaoSessoesPendentes());
    }

    @PostMapping("/sessao/{sessaoId}/acordo")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarAcordo(@PathVariable String sessaoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliacao_acordo", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.conciliacaoRegistrarAcordo(sessaoId, request));
    }

    @PostMapping("/sessao/{sessaoId}/encerramento-sem-acordo")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<SurfaceActionResponse> encerrarSemAcordo(@PathVariable String sessaoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliacao_sem_acordo", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.conciliacaoEncerrarSemAcordo(sessaoId, request));
    }

    @GetMapping("/acordos/pendentes-homologacao")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> acordosPendentesHomologacao(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliacao_homologacao", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.conciliacaoAcordosPendentes());
    }

    @GetMapping("/metricas/mes")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> metricasMes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliacao_metricas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.conciliacaoMetricasMes());
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize(CONCILIACAO_ROLES)
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "conciliador_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("CONCILIADOR"));
    }

}
