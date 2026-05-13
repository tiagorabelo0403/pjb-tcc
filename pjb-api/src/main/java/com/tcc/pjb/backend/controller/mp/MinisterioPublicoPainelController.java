package com.tcc.pjb.backend.controller.mp;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.MinisterioPublicoParecerRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.mp.MinisterioPublicoPainelService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import java.time.Duration;
import java.util.Map;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/mp")
public class MinisterioPublicoPainelController {

    private static final String MP_ROLES = "hasAnyRole('MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA')";
    private final MinisterioPublicoPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public MinisterioPublicoPainelController(MinisterioPublicoPainelService service,
                                             InstitutionalPainelSurfaceFacadeService facadeService,
                                             CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<PerfilDashboardPayload.MinisterioPublicoPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_painel", ApiVersion.V1);
        PerfilDashboardPayload.MinisterioPublicoPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(15)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_malha", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.ministerioPublicoMalhaProcesso(processoId));
    }

    @GetMapping("/manifestacoes/pendentes")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> manifestacoesPendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_manifestacoes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.ministerioPublicoManifestacoesPendentes());
    }

    @PostMapping("/manifestacao/{processoId}")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarManifestacao(@PathVariable Long processoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_manifestacao_post", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.ministerioPublicoRegistrarManifestacao(processoId, request));
    }

    @PostMapping("/parecer/{processoId}")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarParecer(@PathVariable Long processoId,
                                                                  @Valid @RequestBody MinisterioPublicoParecerRequest request,
                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_parecer_post", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.ministerioPublicoRegistrarParecer(processoId, request));
    }

    @PostMapping("/recurso/{processoId}")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceActionResponse> interporRecurso(@PathVariable Long processoId,
                                                                 @Valid @RequestBody InstitutionalRecursoRequest request,
                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_recurso", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                facadeService.ministerioPublicoInterporRecurso(
                        processoId,
                        request.tipoRecurso(),
                        request.razoes(),
                        request.fundamentacao(),
                        request.pedidoEfeitoSuspensivo(),
                        request.preparoDispensado(),
                        request.observacoes()
                )
        );
    }

    @GetMapping("/inqueritos/acompanhamento")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> inqueritosAcompanhamento(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_inqueritos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.ministerioPublicoInqueritosAcompanhamento());
    }

    @PostMapping("/requisicao/diligencia/{processoId}")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceActionResponse> requisitarDiligencia(@PathVariable Long processoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_diligencia", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.ministerioPublicoRequisitarDiligencia(processoId, request));
    }

    @GetMapping("/prazos/criticos")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> prazosCriticos(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "mp_prazos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.ministerioPublicoPrazosCriticos());
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize(MP_ROLES)
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "ministerio_publico_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("MINISTERIO_PUBLICO"));
    }

}
