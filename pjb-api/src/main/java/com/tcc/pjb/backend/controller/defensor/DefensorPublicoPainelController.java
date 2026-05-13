package com.tcc.pjb.backend.controller.defensor;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.defensor.DefensorPublicoPainelService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/defensor")
public class DefensorPublicoPainelController {

    private static final String DEFENSOR_ROLES = "hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_DISTRITAL')";
    private final DefensorPublicoPainelService service;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public DefensorPublicoPainelController(DefensorPublicoPainelService service,
                                           InstitutionalPainelSurfaceFacadeService facadeService,
                                           CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<PerfilDashboardPayload.DefensorPublicoPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_painel", ApiVersion.V1);
        PerfilDashboardPayload.DefensorPublicoPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/assistidos")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> assistidos(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_assistidos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.defensorAssistidos());
    }

    @GetMapping("/assistidos/{assistidoId}/processos")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> processosAssistido(@PathVariable Long assistidoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_processos_assistido", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.defensorProcessosAssistido(assistidoId));
    }

    @PostMapping("/peticao/{processoId}")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarPeticao(@PathVariable Long processoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_peticao", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.defensorRegistrarPeticao(processoId, request));
    }

    @PostMapping("/recurso/{processoId}")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<SurfaceActionResponse> interporRecurso(@PathVariable Long processoId,
                                                                 @Valid @RequestBody InstitutionalRecursoRequest request,
                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_recurso", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                facadeService.defensorInterporRecurso(
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

    @GetMapping("/audiencias/hoje")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> audienciasHoje(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_audiencias", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.defensorAudienciasHoje());
    }

    @GetMapping("/prazos/criticos")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> prazosCriticos(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_prazos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.defensorPrazosCriticos());
    }

    @PostMapping("/gratuidade/{processoId}/requerimento")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<SurfaceActionResponse> requerimentoGratuidade(@PathVariable Long processoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_gratuidade", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.defensorRequerimentoGratuidade(processoId, request));
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_publico_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("DEFENSOR_PUBLICO"));
    }

}
