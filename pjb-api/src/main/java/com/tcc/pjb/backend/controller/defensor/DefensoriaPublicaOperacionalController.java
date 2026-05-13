package com.tcc.pjb.backend.controller.defensor;

import com.tcc.pjb.backend.model.dto.profile.operational.DefensoriaAssistenciaGratuitaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DefensoriaDefesaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DefensoriaHabeasCorpusRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.defensor.surface.DefensoriaOperationalSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/defensor/operacional")
@Validated
public class DefensoriaPublicaOperacionalController {

    private static final String ROLES = "hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL')";
    private final DefensoriaOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public DefensoriaPublicaOperacionalController(DefensoriaOperationalSurfaceFacadeService facadeService,
                                                  CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/snapshot")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "defensoria_snapshot");
        return ResponseEntity.ok(facadeService.snapshot());
    }

    @PostMapping("/processos/{processoId}/defesa")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> apresentarDefesa(@PathVariable Long processoId,
                                                                  @Valid @RequestBody DefensoriaDefesaRequest request,
                                                                  Authentication authentication) {
        enforce(authentication, "defensoria_defesa");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.apresentarDefesa(processoId, request.defesa(), request.fundamentacao()));
    }

    @PostMapping("/processos/{processoId}/habeas-corpus")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> impetrarHabeasCorpus(@PathVariable Long processoId,
                                                                      @Valid @RequestBody DefensoriaHabeasCorpusRequest request,
                                                                      Authentication authentication) {
        enforce(authentication, "defensoria_hc");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.impetrarHabeasCorpus(processoId, request.impetrante(), request.paciente(), request.fundamentacao()));
    }


    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        enforce(authentication, "defensoria_malha");
        return ResponseEntity.ok(facadeService.malhaProcesso(processoId));
    }
    @PostMapping("/processos/{processoId}/assistencia-gratuita")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> solicitarAssistenciaGratuita(@PathVariable Long processoId,
                                                                              @Valid @RequestBody DefensoriaAssistenciaGratuitaRequest request,
                                                                              Authentication authentication) {
        enforce(authentication, "defensoria_ajg");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.solicitarAssistenciaGratuita(processoId, request.renda(), request.justificativa()));
    }

    @GetMapping("/assistidos/sem-advogado")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceCollectionResponse> listarAssistidosSemAdvogado(Authentication authentication) {
        enforce(authentication, "defensoria_assistidos_sem_advogado");
        return ResponseEntity.ok(facadeService.listarAssistidosSemAdvogado());
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
