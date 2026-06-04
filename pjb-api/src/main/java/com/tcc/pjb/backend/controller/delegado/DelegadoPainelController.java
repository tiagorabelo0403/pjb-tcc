package com.tcc.pjb.backend.controller.delegado;

import java.time.Duration;
import java.util.Map;
import java.util.List;
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
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncBundleResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncEventResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncExportRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceArrivalCheckpointRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalAnnexationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalAnnexationResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshAckRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshAckResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshDispatchRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshDispatchResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshReplayResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalCommandCenterResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalAnalyticsResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutoCertificateRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalLinkResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalTimelineEntryResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCheckpointResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoDiligenciaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoInqueritoMultimidiaRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryBatchSyncRequest;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryBatchSyncResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetrySnapshotResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryUpsertRequest;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.delegado.DelegadoPainelService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainLedgerService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainSyncService;
import com.tcc.pjb.backend.service.profile.DiligenceCheckpointService;
import com.tcc.pjb.backend.service.profile.DiligenceCertificateEvidenceService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalClosureService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalCertificateService;
import com.tcc.pjb.backend.service.profile.DiligenceProcessFormalizationService;
import com.tcc.pjb.backend.service.profile.DiligenceReferenceResolverService;
import com.tcc.pjb.backend.service.profile.DiligenceRouteOptimizationService;
import com.tcc.pjb.backend.service.profile.DiligenceTelemetryService;
import com.tcc.pjb.backend.service.profile.DiligenceAutomaticFilingService;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalAnnexationService;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalMeshDispatchService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalAnalyticsService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalCommandCenterService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalTimelineService;

@RestController
@RequestMapping("/api/v1/delegado")
public class DelegadoPainelController {

    private final DelegadoPainelService service;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final DiligenceRouteOptimizationService diligenceRouteOptimizationService;
    private final DigitalCustodyChainService digitalCustodyChainService;
    private final DigitalCustodyChainLedgerService digitalCustodyChainLedgerService;
    private final DiligenceTelemetryService diligenceTelemetryService;
    private final DiligenceCheckpointService diligenceCheckpointService;
    private final DiligenceReferenceResolverService diligenceReferenceResolverService;
    private final DiligenceOperationalCertificateService diligenceOperationalCertificateService;
    private final DiligenceOperationalClosureService diligenceOperationalClosureService;
    private final DiligenceCertificateEvidenceService diligenceCertificateEvidenceService;
    private final DiligenceProcessFormalizationService diligenceProcessFormalizationService;
    private final DiligenceAutomaticFilingService diligenceAutomaticFilingService;
    private final DiligenceOperationalTimelineService diligenceOperationalTimelineService;
    private final DiligenceInstitutionalAnnexationService diligenceInstitutionalAnnexationService;
    private final DiligenceOperationalAnalyticsService diligenceOperationalAnalyticsService;
    private final DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService;
    private final DiligenceOperationalCommandCenterService diligenceOperationalCommandCenterService;
    private final DigitalCustodyChainSyncService digitalCustodyChainSyncService;

    public DelegadoPainelController(DelegadoPainelService service,
                                    CapabilityRateLimiter rateLimiter,
                                    InstitutionalPainelSurfaceFacadeService facadeService,
                                    DiligenceRouteOptimizationService diligenceRouteOptimizationService,
                                    DigitalCustodyChainService digitalCustodyChainService,
                                    DigitalCustodyChainLedgerService digitalCustodyChainLedgerService,
                                    DiligenceTelemetryService diligenceTelemetryService,
                                    DiligenceCheckpointService diligenceCheckpointService,
                                    DiligenceReferenceResolverService diligenceReferenceResolverService,
                                    DiligenceOperationalCertificateService diligenceOperationalCertificateService,
                                    DiligenceOperationalClosureService diligenceOperationalClosureService,
                                    DiligenceCertificateEvidenceService diligenceCertificateEvidenceService,
                                    DiligenceProcessFormalizationService diligenceProcessFormalizationService,
                                    DiligenceAutomaticFilingService diligenceAutomaticFilingService,
                                    DiligenceOperationalTimelineService diligenceOperationalTimelineService,
                                    DiligenceInstitutionalAnnexationService diligenceInstitutionalAnnexationService,
                                    DiligenceOperationalAnalyticsService diligenceOperationalAnalyticsService,
                                    DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService,
                                    DiligenceOperationalCommandCenterService diligenceOperationalCommandCenterService,
                                    DigitalCustodyChainSyncService digitalCustodyChainSyncService,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
        this.facadeService = facadeService;
        this.diligenceRouteOptimizationService = diligenceRouteOptimizationService;
        this.digitalCustodyChainService = digitalCustodyChainService;
        this.digitalCustodyChainLedgerService = digitalCustodyChainLedgerService;
        this.diligenceTelemetryService = diligenceTelemetryService;
        this.diligenceCheckpointService = diligenceCheckpointService;
        this.diligenceReferenceResolverService = diligenceReferenceResolverService;
        this.diligenceOperationalCertificateService = diligenceOperationalCertificateService;
        this.diligenceOperationalClosureService = diligenceOperationalClosureService;
        this.diligenceCertificateEvidenceService = diligenceCertificateEvidenceService;
        this.diligenceProcessFormalizationService = diligenceProcessFormalizationService;
        this.diligenceAutomaticFilingService = diligenceAutomaticFilingService;
        this.diligenceOperationalTimelineService = diligenceOperationalTimelineService;
        this.diligenceInstitutionalAnnexationService = diligenceInstitutionalAnnexationService;
        this.diligenceOperationalAnalyticsService = diligenceOperationalAnalyticsService;
        this.diligenceInstitutionalMeshDispatchService = diligenceInstitutionalMeshDispatchService;
        this.diligenceOperationalCommandCenterService = diligenceOperationalCommandCenterService;
        this.digitalCustodyChainSyncService = digitalCustodyChainSyncService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<PerfilDashboardPayload.DelegadoPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_painel", ApiVersion.V1);
        PerfilDashboardPayload.DelegadoPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/inqueritos/pendentes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceCollectionResponse> inqueritosPendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_inqueritos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoInqueritosPendentes());
    }

    @PostMapping("/inqueritos/{inqueritoId}/peca-multimidia")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<SurfaceActionResponse> registrarPecaInquerito(@PathVariable Long inqueritoId,
                                                                        @Valid @RequestBody DelegadoInqueritoMultimidiaRequest request,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_inquerito_peca_multimidia", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.delegadoRegistrarPecaInquerito(inqueritoId, request));
    }

    @GetMapping("/mandados/cumprir")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> mandadosPendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_mandados", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoMandadosPendentes());
    }


    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<Object> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha", ApiVersion.V1);
        return ResponseEntity.ok(service.malhaProcesso(processoId));
    }
    @GetMapping("/processos/{processoId}/acesso")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceActionResponse> solicitarAcessoProcesso(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_acesso_processo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoSolicitarAcessoProcesso(processoId));
    }

    @PostMapping("/requisicao/diligencia")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceActionResponse> requisitarDiligencia(@Valid @RequestBody DelegadoDiligenciaRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencia", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.delegadoRequisitarDiligencia(request));
    }

    @PostMapping("/diligencias/telemetria")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> registrarTelemetria(@Valid @RequestBody RouteTelemetryUpsertRequest request,
                                                                              @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.register(TelemetriaOperacionalCanal.DELEGADO, request, deviceId));
    }

    @GetMapping("/diligencias/telemetria/ultima")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> ultimaTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria_ultima", ApiVersion.V1);
        return diligenceTelemetryService.latest(TelemetriaOperacionalCanal.DELEGADO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/diligencias/telemetria/sync")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<RouteTelemetryBatchSyncResponse> sincronizarTelemetria(@Valid @RequestBody RouteTelemetryBatchSyncRequest request,
                                                                                 @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria_sync", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.registerBatch(TelemetriaOperacionalCanal.DELEGADO, request, deviceId));
    }

    @GetMapping("/diligencias/telemetria/historico")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<RouteTelemetrySnapshotResponse>> historicoTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.history(TelemetriaOperacionalCanal.DELEGADO, 20));
    }

    @PostMapping("/diligencias/rota-otimizada")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceRouteOptimizationResponse> rotaOtimizada(@Valid @RequestBody DiligenceRouteOptimizationRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_rota_otimizada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceRouteOptimizationService.optimize(request));
    }

    @PostMapping("/provas/custodia/selar")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<ChainOfCustodySealResponse> selarCustodia(@Valid @RequestBody ChainOfCustodySealRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_selar", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainService.seal(request));
    }

    @GetMapping("/provas/custodia/{chaveCustodia}/ledger")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<ChainOfCustodyLedgerResponse> ledgerCustodia(@PathVariable String chaveCustodia, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_ledger", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainLedgerService.ledger(chaveCustodia));
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> alertas(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_alertas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoAlertas());
    }

    @GetMapping("/inteligencia/pessoas/localizacao/recentes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceCollectionResponse> consultasRecentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_localizador_consultas_recentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoConsultasRecentes());
    }

    @GetMapping("/inteligencia/pessoas/localizacao/metricas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> metricasLocalizador(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_localizador_metricas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoMetricasLocalizador());
    }

    @PostMapping("/inteligencia/pessoas/localizacao")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<PessoaLocalizacaoResponse> localizarPessoa(@Valid @RequestBody PessoaLocalizacaoRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_localizador_pessoas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoLocalizarPessoa(request));
    }


    @PostMapping("/diligencias/{diligenciaId}/checkpoints/chegada")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceCheckpointResponse> confirmarChegada(@PathVariable String diligenciaId,
                                                                        @Valid @RequestBody DiligenceArrivalCheckpointRequest request,
                                                                        @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_checkpoint_chegada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.registerArrival(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request, deviceId));
    }

    @GetMapping("/diligencias/{diligenciaId}/checkpoints")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCheckpointResponse>> historicoCheckpoints(@PathVariable String diligenciaId,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_checkpoint_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @GetMapping("/diligencias/{diligenciaId}/vinculo-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalLinkResponse> vinculoOperacional(@PathVariable String diligenciaId,
                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_vinculo_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceReferenceResolverService.describe(TelemetriaOperacionalCanal.DELEGADO, diligenciaId));
    }

    @PostMapping("/diligencias/{diligenciaId}/certidoes/auto")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<DiligenceCertificateResponse> gerarCertidaoAutomatica(@PathVariable String diligenciaId,
                                                                                @Valid @RequestBody(required = false) DiligenceAutoCertificateRequest request,
                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_automatica", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalCertificateService.generate(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/certidoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateResponse>> historicoCertidoes(@PathVariable String diligenciaId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalCertificateService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }


    @PostMapping("/diligencias/{diligenciaId}/encerramento-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalClosureResponse> encerrarOperacional(@PathVariable String diligenciaId,
                                                                                   @Valid @RequestBody DiligenceOperationalClosureRequest request,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_encerramento_operacional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalClosureService.close(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/encerramentos-operacionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceOperationalClosureResponse>> historicoEncerramentos(@PathVariable String diligenciaId,
                                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_encerramento_operacional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalClosureService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @PostMapping("/diligencias/{diligenciaId}/certidoes/{certidaoId}/documentos/vincular")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> vincularDocumentosCertidao(@PathVariable String diligenciaId,
                                                                                                      @PathVariable Long certidaoId,
                                                                                                      @Valid @RequestBody DiligenceCertificateDocumentLinkRequest request,
                                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_documentos_vincular", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(diligenceCertificateEvidenceService.bind(certidaoId, TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/certidoes/{certidaoId}/documentos")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> listarDocumentosCertidao(@PathVariable String diligenciaId,
                                                                                                    @PathVariable Long certidaoId,
                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_documentos_listar", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.list(certidaoId, TelemetriaOperacionalCanal.DELEGADO, diligenciaId));
    }

    @GetMapping("/diligencias/{diligenciaId}/certidoes/{certidaoId}/documentos/sugestoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> sugerirDocumentosCertidao(@PathVariable String diligenciaId,
                                                                                                     @PathVariable Long certidaoId,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_documentos_sugestoes", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.suggestions(certidaoId, TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 10));
    }


    @PostMapping("/diligencias/{diligenciaId}/formalizacao-processual")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceProcessFormalizationResponse> formalizarProcessualmente(@PathVariable String diligenciaId,
                                                                                           @Valid @RequestBody(required = false) DiligenceProcessFormalizationRequest request,
                                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_formalizacao_processual", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceProcessFormalizationService.formalize(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/formalizacoes-processuais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceProcessFormalizationResponse>> historicoFormalizacoes(@PathVariable String diligenciaId,
                                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_formalizacao_processual_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceProcessFormalizationService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }


    @PostMapping("/diligencias/{diligenciaId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceAutomaticFilingResponse> gerarJuntadaAutomatica(@PathVariable String diligenciaId,
                                                                                   @Valid @RequestBody(required = false) DiligenceAutomaticFilingRequest request,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_juntada_automatica", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceAutomaticFilingService.file(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceAutomaticFilingResponse>> historicoJuntadasAutomaticas(@PathVariable String diligenciaId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_juntada_automatica_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceAutomaticFilingService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @GetMapping("/diligencias/{diligenciaId}/timeline-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceOperationalTimelineEntryResponse>> timelineOperacional(@PathVariable String diligenciaId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_timeline_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalTimelineService.timeline(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 50));
    }

    
    @PostMapping("/diligencias/{diligenciaId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalAnnexationResponse> anexarInstitucionalmente(@PathVariable String diligenciaId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalAnnexationRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_anexacao_institucional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalAnnexationService.annex(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceInstitutionalAnnexationResponse>> historicoAnexacoesInstitucionais(@PathVariable String diligenciaId,
                                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_anexacao_institucional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalAnnexationService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @GetMapping("/diligencias/{diligenciaId}/analytics-operacionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalAnalyticsResponse> analyticsOperacionais(@PathVariable String diligenciaId,
                                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_analytics_operacionais", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalAnalyticsService.analytics(TelemetriaOperacionalCanal.DELEGADO, diligenciaId));
    }

    @GetMapping("/painel/comando-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalCommandCenterResponse> comandoOperacional(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_comando_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalCommandCenterService.snapshot(TelemetriaOperacionalCanal.DELEGADO, 30, 10));
    }

    @PostMapping("/diligencias/{diligenciaId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalMeshDispatchResponse> expedirParaMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                                     @Valid @RequestBody(required = false) DiligenceInstitutionalMeshDispatchRequest request,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_expedicao", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalMeshDispatchService.dispatch(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceInstitutionalMeshDispatchResponse>> historicoExpedicoesMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_expedicao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @PostMapping("/diligencias/{diligenciaId}/malha-institucional/expedicoes/{dispatchId}/ack")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalMeshAckResponse> confirmarAckMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                                 @PathVariable Long dispatchId,
                                                                                                 @Valid @RequestBody(required = false) DiligenceInstitutionalMeshAckRequest request,
                                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_ack", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.acknowledge(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, dispatchId, request));
    }

    @PostMapping("/diligencias/{diligenciaId}/malha-institucional/replay")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalMeshReplayResponse> replayMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalMeshReplayRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_replay", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.replay(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

@PostMapping("/provas/custodia/{chaveCustodia}/sync/export")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<ChainOfCustodySyncBundleResponse> exportarCustodia(@PathVariable String chaveCustodia,
                                                                             @RequestBody(required = false) ChainOfCustodySyncExportRequest request,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_sync_export", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.exportBundle(chaveCustodia, request));
    }



    @PostMapping("/provas/custodia/sync/replay")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<ChainOfCustodySyncReplayResponse> replayCustodia(@Valid @RequestBody ChainOfCustodySyncReplayRequest request,
                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_sync_replay", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.replayVerify(request));
    }

    @GetMapping("/provas/custodia/{chaveCustodia}/sync/events")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<ChainOfCustodySyncEventResponse>> eventosCustodia(@PathVariable String chaveCustodia,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_sync_events", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.history(chaveCustodia));
    }



    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("DELEGADO"));
    }

}
