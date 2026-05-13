package com.tcc.pjb.backend.controller.perito;

import com.tcc.pjb.backend.model.dto.profile.operational.PeritoHonorariosRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PeritoLaudoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PeritoQuesitosRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.perito.surface.PeritoOperationalSurfaceFacadeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
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
@RequestMapping("/api/v1/perito/operacional")
@Validated
public class PeritoOperacionalEnhancedController {

    private static final String ROLES = "hasAnyRole('PERITO','PERITO_CRIMINAL','PERITO_AMBIENTAL','PERITO_CONTABIL','PERITO_ENGENHARIA','PERITO_DIGITAL','PERITO_INSS','PERITO_MEDICO','PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL','ASSISTENTE_TECNICO')";
    private final PeritoOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public PeritoOperacionalEnhancedController(PeritoOperationalSurfaceFacadeService facadeService,
                                               CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/snapshot")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "perito_operacional_snapshot");
        return ResponseEntity.ok(facadeService.snapshot());
    }

    @PostMapping("/processos/{processoId}/aceite")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> aceitarNomeacao(@PathVariable Long processoId,
                                                                 Authentication authentication) {
        enforce(authentication, "perito_operacional_aceite");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.aceitarNomeacao(processoId));
    }

    @PostMapping("/processos/{processoId}/laudo")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> apresentarLaudo(@PathVariable Long processoId,
                                                                 @Valid @RequestBody PeritoLaudoRequest request,
                                                                 Authentication authentication) {
        enforce(authentication, "perito_operacional_laudo");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.apresentarLaudo(processoId, request));
    }

    @PostMapping("/processos/{processoId}/quesitos")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> responderQuesitos(@PathVariable Long processoId,
                                                                   @Valid @RequestBody PeritoQuesitosRequest request,
                                                                   Authentication authentication) {
        enforce(authentication, "perito_operacional_quesitos");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.responderQuesitos(processoId, request));
    }

    @PostMapping("/processos/{processoId}/honorarios")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> solicitarHonorarios(@PathVariable Long processoId,
                                                                     @Valid @RequestBody PeritoHonorariosRequest request,
                                                                     Authentication authentication) {
        enforce(authentication, "perito_operacional_honorarios");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.solicitarHonorarios(processoId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
