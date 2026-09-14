package com.tcc.pjb.backend.controller.criminal;

import com.tcc.pjb.backend.model.dto.criminal.InqueritoCadastroRequest;
import com.tcc.pjb.backend.model.dto.criminal.InqueritoMovimentacaoRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceExecutionReconciliationRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceExecutionRetryRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceLocalSnapshotRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceNativeCautelarDispatchRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceNativeIntimationMirrorRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceTransactionalContingencyRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeExecutionService;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeToolbeltService;
import com.tcc.pjb.backend.service.criminal.PoliceInteroperabilityAdapterBlueprintService;
import com.tcc.pjb.backend.service.criminal.PoliceInvestigationSystemLandscapeService;
import com.tcc.pjb.backend.service.criminal.PoliceSovereignOperationalWorkbenchService;
import com.tcc.pjb.backend.service.criminal.PoliceTraceableExecutionLedgerService;
import com.tcc.pjb.backend.service.criminal.PoliceTransactionalAdapterMeshService;
import com.tcc.pjb.backend.service.criminal.surface.InqueritoPolicialDigitalSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inqueritos-digitais")
public class InqueritoPolicialDigitalController {

    private final InqueritoPolicialDigitalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService;
    private final PoliceInteroperabilityAdapterBlueprintService policeInteroperabilityAdapterBlueprintService;
    private final PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService;
    private final PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService;
    private final PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService;
    private final PjbPoliceNativeExecutionService pjbPoliceNativeExecutionService;
    private final PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService;

    public InqueritoPolicialDigitalController(InqueritoPolicialDigitalSurfaceFacadeService facadeService,
                                              CapabilityRateLimiter rateLimiter,
                                              PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService,
                                              PoliceInteroperabilityAdapterBlueprintService policeInteroperabilityAdapterBlueprintService,
                                              PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService,
                                              PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService,
                                              PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService,
                                              PjbPoliceNativeExecutionService pjbPoliceNativeExecutionService,
                                              PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.policeInvestigationSystemLandscapeService = policeInvestigationSystemLandscapeService;
        this.policeInteroperabilityAdapterBlueprintService = policeInteroperabilityAdapterBlueprintService;
        this.pjbPoliceNativeToolbeltService = pjbPoliceNativeToolbeltService;
        this.policeTransactionalAdapterMeshService = policeTransactionalAdapterMeshService;
        this.policeSovereignOperationalWorkbenchService = policeSovereignOperationalWorkbenchService;
        this.pjbPoliceNativeExecutionService = pjbPoliceNativeExecutionService;
        this.policeTraceableExecutionLedgerService = policeTraceableExecutionLedgerService;
    }

    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> meus(@RequestParam(required = false) String status,
                                                          Authentication authentication) {
        enforce(authentication, "inquerito_digital_listar");
        return ResponseEntity.ok(facadeService.listarMeus(status));
    }

    @GetMapping("/processos/{processoId}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> porProcesso(@PathVariable Long processoId,
                                                                 Authentication authentication) {
        enforce(authentication, "inquerito_digital_processo");
        return ResponseEntity.ok(facadeService.listarPorProcesso(processoId));
    }

    @GetMapping("/catalog/sistemas-policiais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> catalogoSistemas(Authentication authentication) {
        enforce(authentication, "inquerito_digital_catalogo_sistemas");
        return ResponseEntity.ok(policeInvestigationSystemLandscapeService.landscapeFor(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/sistemas-policiais/{codigo}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> detalheSistema(@PathVariable String codigo,
                                                 Authentication authentication) {
        enforce(authentication, "inquerito_digital_catalogo_sistemas");
        return ResponseEntity.ok(policeInvestigationSystemLandscapeService.detail(codigo));
    }

    @GetMapping("/catalog/workstation-investigativa")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> workstationInvestigativa(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeInvestigationSystemLandscapeService.workstationBlueprint(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/adapters-operacionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> catalogoAdaptersOperacionais(Authentication authentication) {
        enforce(authentication, "inquerito_digital_catalogo_sistemas");
        return ResponseEntity.ok(policeInteroperabilityAdapterBlueprintService.adapterCatalog(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/adapters-operacionais/{codigo}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> detalheAdapterOperacional(@PathVariable String codigo,
                                                            Authentication authentication) {
        enforce(authentication, "inquerito_digital_catalogo_sistemas");
        return ResponseEntity.ok(policeInteroperabilityAdapterBlueprintService.detail(codigo));
    }

    @GetMapping("/catalog/malha-interoperabilidade")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> malhaInteroperabilidade(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeInteroperabilityAdapterBlueprintService.operationalMesh(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/ferramentas-proprias")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> catalogoFerramentasProprias(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeToolbeltService.toolCatalog(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/ferramentas-proprias/{codigo}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> detalheFerramentaPropria(@PathVariable String codigo,
                                                           Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeToolbeltService.detail(codigo));
    }

    @GetMapping("/catalog/adapters-transacionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> catalogoAdaptersTransacionais(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeTransactionalAdapterMeshService.transactionalCatalog(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/adapters-transacionais/{codigo}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> detalheAdapterTransacional(@PathVariable String codigo,
                                                             Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeTransactionalAdapterMeshService.detail(codigo));
    }

    @GetMapping("/catalog/malha-soberana")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> malhaSoberana(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeSovereignOperationalWorkbenchService.compose(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/ferramentas-executaveis")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> catalogoFerramentasExecutaveis(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeExecutionService.executableCatalog(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/ferramentas-executaveis/{codigo}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> detalheFerramentaExecutavel(@PathVariable String codigo,
                                                              Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeExecutionService.detail(codigo, resolveTipoUsuario(authentication)));
    }

    @GetMapping("/catalog/workbench-executavel")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> workbenchExecutavel(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeExecutionService.nativeExecutionWorkbench(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/native/executions/ledger")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> ledgerOperacional(Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeTraceableExecutionLedgerService.operationalLedgerBlueprint(resolveTipoUsuario(authentication)));
    }

    @GetMapping("/native/executions")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> execucoesRecentes(@RequestParam(defaultValue = "20") int limit,
                                                    Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeTraceableExecutionLedgerService.recentExecutions(resolveTipoUsuario(authentication), limit));
    }

    @GetMapping("/native/executions/{executionId}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<Object> statusExecucao(@PathVariable String executionId,
                                                 Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeTraceableExecutionLedgerService.executionStatus(executionId));
    }

    @PostMapping("/native/executions/{executionId}/reconciliacao")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<Object> reconciliarExecucao(@PathVariable String executionId,
                                                      @Valid @RequestBody PoliceExecutionReconciliationRequest request,
                                                      Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeTraceableExecutionLedgerService.reconcileExecution(resolveTipoUsuario(authentication), executionId, request));
    }

    @PostMapping("/native/executions/{executionId}/retentativa")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<Object> retentarExecucao(@PathVariable String executionId,
                                                   @Valid @RequestBody PoliceExecutionRetryRequest request,
                                                   Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(policeTraceableExecutionLedgerService.retryExecution(resolveTipoUsuario(authentication), executionId, request));
    }

    @PostMapping("/native/cautelares/remessa")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<Object> remessaCautelarNativa(@Valid @RequestBody PoliceNativeCautelarDispatchRequest request,
                                                        Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeExecutionService.dispatchCautelar(resolveTipoUsuario(authentication), request));
    }

    @PostMapping("/native/intimacoes-espelho")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<Object> espelhoSoberanoIntimacoes(@Valid @RequestBody PoliceNativeIntimationMirrorRequest request,
                                                            Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeExecutionService.mirrorIntimacoesEventos(resolveTipoUsuario(authentication), request));
    }

    @PostMapping("/native/contingencia-transacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<Object> contingenciaTransacional(@Valid @RequestBody PoliceTransactionalContingencyRequest request,
                                                           Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeExecutionService.enqueueContingencia(resolveTipoUsuario(authentication), request));
    }

    @PostMapping("/native/snapshot-local")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<Object> snapshotLocal(@Valid @RequestBody PoliceLocalSnapshotRequest request,
                                                Authentication authentication) {
        enforce(authentication, "inquerito_digital_workstation");
        return ResponseEntity.ok(pjbPoliceNativeExecutionService.buildSnapshot(resolveTipoUsuario(authentication), request));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<SurfaceSnapshotResponse> registrar(@Valid @RequestBody InqueritoCadastroRequest request,
                                                             Authentication authentication,
                                                             HttpServletRequest servletRequest) {
        enforce(authentication, "inquerito_digital_registrar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrar(request, servletRequest));
    }

    @PatchMapping("/{inqueritoId}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> movimentar(@PathVariable Long inqueritoId,
                                                              @Valid @RequestBody InqueritoMovimentacaoRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "inquerito_digital_movimentar");
        return ResponseEntity.ok(facadeService.movimentar(inqueritoId, request));
    }

    @PostMapping("/{inqueritoId}/vincular-processo/{processoId}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','MEMBRO_MINISTERIO_PUBLICO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_MILITAR','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> vincularProcesso(@PathVariable Long inqueritoId,
                                                                    @PathVariable Long processoId,
                                                                    Authentication authentication) {
        enforce(authentication, "inquerito_digital_vincular");
        return ResponseEntity.ok(facadeService.vincularProcesso(inqueritoId, processoId));
    }

    private com.tcc.pjb.backend.model.entity.enums.TipoUsuario resolveTipoUsuario(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return com.tcc.pjb.backend.model.entity.enums.TipoUsuario.DELEGADO_POLICIA;
        }
        boolean federal = authentication.getAuthorities().stream().anyMatch(authority -> authority != null && "ROLE_DELEGADO_POLICIA_FEDERAL".equalsIgnoreCase(authority.getAuthority()));
        if (federal) {
            return com.tcc.pjb.backend.model.entity.enums.TipoUsuario.DELEGADO_POLICIA_FEDERAL;
        }
        return com.tcc.pjb.backend.model.entity.enums.TipoUsuario.DELEGADO_POLICIA;
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
