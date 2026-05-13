package com.tcc.pjb.backend.controller.conciliacao;

import com.tcc.pjb.backend.model.dto.profile.operational.ConciliacaoAgendamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ConciliacaoResultadoSessaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ConciliacaoTermoAcordoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.conciliacao.surface.ConciliacaoOperationalSurfaceFacadeService;
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
@RequestMapping("/api/v1/conciliacao/operacional")
@Validated
public class ConciliadorMediadorEnhancedController {

    private static final String ROLES = "hasAnyRole('CONCILIADOR_CEJUSC','MEDIADOR','ARBITRO')";
    private final ConciliacaoOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public ConciliadorMediadorEnhancedController(ConciliacaoOperationalSurfaceFacadeService facadeService,
                                                 CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/snapshot")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "conciliacao_operacional_snapshot");
        return ResponseEntity.ok(facadeService.snapshot());
    }

    @PostMapping("/processos/{processoId}/resultado")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarResultado(@PathVariable Long processoId,
                                                                    @Valid @RequestBody ConciliacaoResultadoSessaoRequest request,
                                                                    Authentication authentication) {
        enforce(authentication, "conciliacao_operacional_resultado");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.registrarResultado(processoId, request.resultado(), request.observacoes(), request.acordoFirmado()));
    }

    @PostMapping("/processos/{processoId}/agendamento")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> agendarSessao(@PathVariable Long processoId,
                                                               @Valid @RequestBody ConciliacaoAgendamentoSessaoRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "conciliacao_operacional_agendamento");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.agendarSessao(processoId, request.dataHora(), request.sala(), request.modalidade()));
    }

    @PostMapping("/processos/{processoId}/termo-acordo")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> lavrarTermoAcordo(@PathVariable Long processoId,
                                                                   @Valid @RequestBody ConciliacaoTermoAcordoRequest request,
                                                                   Authentication authentication) {
        enforce(authentication, "conciliacao_operacional_termo");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.lavrarTermoAcordo(processoId, request.clausulas(), request.partes(), request.valor()));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
