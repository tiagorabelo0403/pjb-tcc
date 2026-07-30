package com.tcc.pjb.backend.controller.procuradoria;

import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaContestacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaExecucaoFiscalRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaParecerRequest;
import com.tcc.pjb.backend.controller.recursal.RecursalLegacyDeprecationHeaders;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.procuradoria.surface.ProcuradoriaOperationalSurfaceFacadeService;
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
@RequestMapping("/api/v1/procuradoria/operacional")
@Validated
public class ProcuradoriaOperacionalController {

    private static final String ROLES = "hasAnyRole('PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','PROCURADOR_GERAL_REPUBLICA')";
    private final ProcuradoriaOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public ProcuradoriaOperacionalController(ProcuradoriaOperationalSurfaceFacadeService facadeService,
                                             CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/snapshot")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "procuradoria_snapshot");
        return ResponseEntity.ok(facadeService.snapshot());
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        enforce(authentication, "procuradoria_malha");
        return ResponseEntity.ok(facadeService.malhaProcesso(processoId));
    }

    @PostMapping("/processos/{processoId}/contestacao")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> apresentarContestacao(@PathVariable Long processoId,
                                                                       @Valid @RequestBody ProcuradoriaContestacaoRequest request,
                                                                       Authentication authentication) {
        enforce(authentication, "procuradoria_contestacao");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.apresentarContestacao(processoId, request));
    }

    @PostMapping("/execucao-fiscal")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> ajuizarExecucaoFiscal(@Valid @RequestBody ProcuradoriaExecucaoFiscalRequest request,
                                                                       Authentication authentication) {
        enforce(authentication, "procuradoria_execucao_fiscal");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.ajuizarExecucaoFiscal(request.devedorCpfCnpj(), request.valorDivida(), request.descricao(), request.comarca()));
    }

    @PostMapping("/processos/{processoId}/recurso")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> interporRecurso(@PathVariable Long processoId,
                                                                 @Valid @RequestBody InstitutionalRecursoRequest request,
                                                                 Authentication authentication) {
        enforce(authentication, "procuradoria_recurso");
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(RecursalLegacyDeprecationHeaders.forProcesso(processoId))
                .body(facadeService.interporRecurso(
                        processoId,
                        request.tipoRecurso(),
                        request.razoes(),
                        request.fundamentacao(),
                        request.pedidoEfeitoSuspensivo(),
                        request.preparoDispensado(),
                        request.observacoes()
                ));
    }

    @PostMapping("/processos/{processoId}/parecer")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> emitirParecer(@PathVariable Long processoId,
                                                               @Valid @RequestBody ProcuradoriaParecerRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "procuradoria_parecer");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.emitirParecer(processoId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
