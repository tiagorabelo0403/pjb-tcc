package com.tcc.pjb.backend.controller.oficial_justica;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.ChatMensagemResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualChatResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCienciaIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualMessageRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCalendarioOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCumprimentoEncerramentoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaEnderecoTriageResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPessoaRastreioResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoAcessoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoWorkbenchResponse;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
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
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaCumprimentoEncerramentoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioCartorioAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioChannelAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioConfirmationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioReconciliationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRetryRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
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
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaCumprimentoSoberanoService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaPainelService;
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
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaCampoController {

    private final CapabilityRateLimiter rateLimiter;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final DiligenceRouteOptimizationService diligenceRouteOptimizationService;
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
    private final DigitalCustodyChainSyncService digitalCustodyChainSyncService;
    private final OficialJusticaCumprimentoSoberanoService oficialJusticaCumprimentoSoberanoService;
    private final OperationalFunctionCredentialService credentialService;

    public OficialJusticaCampoController(
            CapabilityRateLimiter rateLimiter,
            InstitutionalPainelSurfaceFacadeService facadeService,
            DiligenceRouteOptimizationService diligenceRouteOptimizationService,
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
            DigitalCustodyChainSyncService digitalCustodyChainSyncService,
            OficialJusticaCumprimentoSoberanoService oficialJusticaCumprimentoSoberanoService,
            OperationalFunctionCredentialService credentialService
    ) {
        this.rateLimiter = rateLimiter;
        this.facadeService = facadeService;
        this.diligenceRouteOptimizationService = diligenceRouteOptimizationService;
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
        this.digitalCustodyChainSyncService = digitalCustodyChainSyncService;
        this.oficialJusticaCumprimentoSoberanoService = oficialJusticaCumprimentoSoberanoService;
        this.credentialService = credentialService;
    }

    @GetMapping("/rota/dia")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceCollectionResponse> rotaDia(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialRotaDia());
    }

    @PostMapping("/rota/telemetria")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> registrarTelemetria(@Valid @RequestBody RouteTelemetryUpsertRequest request,
                                                                              @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.register(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, request, deviceId));
    }

    @GetMapping("/rota/telemetria/ultima")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> ultimaTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria_ultima", ApiVersion.V1);
        return diligenceTelemetryService.latest(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/rota/telemetria/sync")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<RouteTelemetryBatchSyncResponse> sincronizarTelemetria(@Valid @RequestBody RouteTelemetryBatchSyncRequest request,
                                                                                 @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria_sync", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.registerBatch(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, request, deviceId));
    }

    @GetMapping("/rota/telemetria/historico")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<RouteTelemetrySnapshotResponse>> historicoTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, 20));
    }

    @PostMapping("/rota/otimizada")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceRouteOptimizationResponse> rotaOtimizada(@Valid @RequestBody DiligenceRouteOptimizationRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_otimizada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceRouteOptimizationService.optimize(request));
    }

    @GetMapping("/localizador/consultas/recentes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceCollectionResponse> consultasRecentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_consultas_recentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialConsultasRecentes());
    }

    @GetMapping("/localizador/rastreio/resumo")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> resumoRastreioOperacional(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_rastreio_resumo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialResumoRastreioOperacional());
    }

    @GetMapping("/localizador/triagem-enderecos")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> triagemEnderecos(@RequestParam(defaultValue = "8") int limit,
                                                                    @RequestParam(defaultValue = "true") boolean incluirEnderecoEstrito,
                                                                    @RequestParam(defaultValue = "true") boolean incluirProntuario,
                                                                    @RequestParam(defaultValue = "false") boolean incluirRestricoes,
                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_triagem_enderecos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialTriagemEnderecos(limit, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes));
    }

    @GetMapping("/localizador/mandados/{mandadoId}/rastreio")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaPessoaRastreioResponse> rastrearMandado(@PathVariable String mandadoId,
                                                                                 @RequestParam(defaultValue = "true") boolean incluirEnderecoEstrito,
                                                                                 @RequestParam(defaultValue = "true") boolean incluirProntuario,
                                                                                 @RequestParam(defaultValue = "false") boolean incluirRestricoes,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_rastrear_mandado", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialRastrearMandado(mandadoId, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes));
    }

    @GetMapping("/localizador/processos/{processoId}/alvos/{polo}/rastreio")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaPessoaRastreioResponse> rastrearProcessoAlvo(@PathVariable Long processoId,
                                                                                      @PathVariable String polo,
                                                                                      @RequestParam(defaultValue = "true") boolean incluirEnderecoEstrito,
                                                                                      @RequestParam(defaultValue = "true") boolean incluirProntuario,
                                                                                      @RequestParam(defaultValue = "false") boolean incluirRestricoes,
                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_rastrear_processo_alvo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialRastrearProcessoAlvo(processoId, polo, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes));
    }

    @PostMapping("/localizador/pessoas")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<PessoaLocalizacaoResponse> localizarPessoa(@Valid @RequestBody PessoaLocalizacaoRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_pessoas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialLocalizarPessoa(request));
    }

    @PostMapping("/mandados/{mandadoId}/checkpoints/chegada")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceCheckpointResponse> confirmarChegada(@PathVariable String mandadoId,
                                                                        @Valid @RequestBody DiligenceArrivalCheckpointRequest request,
                                                                        @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_checkpoint_chegada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.registerArrival(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request, deviceId));
    }

    @GetMapping("/mandados/{mandadoId}/checkpoints")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCheckpointResponse>> historicoCheckpoints(@PathVariable String mandadoId,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_checkpoint_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @GetMapping("/mandados/{mandadoId}/vinculo-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceOperationalLinkResponse> vinculoOperacional(@PathVariable String mandadoId,
                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_vinculo_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceReferenceResolverService.describe(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId));
    }

    @PostMapping("/mandados/{mandadoId}/certidoes/auto")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceCertificateResponse> gerarCertidaoAutomatica(@PathVariable String mandadoId,
                                                                                @Valid @RequestBody(required = false) DiligenceAutoCertificateRequest request,
                                                                                @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_automatica", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_CERTIDAO_AUTOMATICA", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalCertificateService.generate(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/certidoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateResponse>> historicoCertidoes(@PathVariable String mandadoId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalCertificateService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/encerramento-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceOperationalClosureResponse> encerrarOperacional(@PathVariable String mandadoId,
                                                                                   @Valid @RequestBody DiligenceOperationalClosureRequest request,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_encerramento_operacional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalClosureService.close(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/encerramentos-operacionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceOperationalClosureResponse>> historicoEncerramentos(@PathVariable String mandadoId,
                                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_encerramento_operacional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalClosureService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/encerramento-soberano")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaCumprimentoEncerramentoResponse> encerrarSoberanamente(@PathVariable String mandadoId,
                                                                                                @Valid @RequestBody(required = false) OficialJusticaCumprimentoEncerramentoRequest request,
                                                                                                @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_encerramento_soberano", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_ENCERRAMENTO_SOBERANO", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(oficialJusticaCumprimentoSoberanoService.encerrar(mandadoId, request));
    }

    @PostMapping("/mandados/{mandadoId}/certidoes/{certidaoId}/documentos/vincular")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> vincularDocumentosCertidao(@PathVariable String mandadoId,
                                                                                                      @PathVariable Long certidaoId,
                                                                                                      @Valid @RequestBody DiligenceCertificateDocumentLinkRequest request,
                                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_documentos_vincular", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(diligenceCertificateEvidenceService.bind(certidaoId, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/certidoes/{certidaoId}/documentos")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> listarDocumentosCertidao(@PathVariable String mandadoId,
                                                                                                    @PathVariable Long certidaoId,
                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_documentos_listar", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.list(certidaoId, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId));
    }

    @GetMapping("/mandados/{mandadoId}/certidoes/{certidaoId}/documentos/sugestoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> sugerirDocumentosCertidao(@PathVariable String mandadoId,
                                                                                                     @PathVariable Long certidaoId,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_documentos_sugestoes", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.suggestions(certidaoId, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 10));
    }

    @PostMapping("/mandados/{mandadoId}/formalizacao-processual")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceProcessFormalizationResponse> formalizarProcessualmente(@PathVariable String mandadoId,
                                                                                           @Valid @RequestBody(required = false) DiligenceProcessFormalizationRequest request,
                                                                                           @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_formalizacao_processual", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_FORMALIZACAO_PROCESSUAL", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceProcessFormalizationService.formalize(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/formalizacoes-processuais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceProcessFormalizationResponse>> historicoFormalizacoes(@PathVariable String mandadoId,
                                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_formalizacao_processual_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceProcessFormalizationService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceAutomaticFilingResponse> gerarJuntadaAutomatica(@PathVariable String mandadoId,
                                                                                   @Valid @RequestBody(required = false) DiligenceAutomaticFilingRequest request,
                                                                                   @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_juntada_automatica", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_JUNTADA_AUTOMATICA", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceAutomaticFilingService.file(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceAutomaticFilingResponse>> historicoJuntadasAutomaticas(@PathVariable String mandadoId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_juntada_automatica_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceAutomaticFilingService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @GetMapping("/mandados/{mandadoId}/timeline-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceOperationalTimelineEntryResponse>> timelineOperacional(@PathVariable String mandadoId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_timeline_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalTimelineService.timeline(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 50));
    }

    @PostMapping("/mandados/{mandadoId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalAnnexationResponse> anexarInstitucionalmente(@PathVariable String mandadoId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalAnnexationRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_anexacao_institucional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalAnnexationService.annex(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceInstitutionalAnnexationResponse>> historicoAnexacoesInstitucionais(@PathVariable String mandadoId,
                                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_anexacao_institucional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalAnnexationService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @GetMapping("/mandados/{mandadoId}/analytics-operacionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceOperationalAnalyticsResponse> analyticsOperacionais(@PathVariable String mandadoId,
                                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_analytics_operacionais", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalAnalyticsService.analytics(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId));
    }

    @PostMapping("/mandados/{mandadoId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalMeshDispatchResponse> expedirParaMalhaInstitucional(@PathVariable String mandadoId,
                                                                                                     @Valid @RequestBody(required = false) DiligenceInstitutionalMeshDispatchRequest request,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_expedicao", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalMeshDispatchService.dispatch(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceInstitutionalMeshDispatchResponse>> historicoExpedicoesMalhaInstitucional(@PathVariable String mandadoId,
                                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_expedicao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/malha-institucional/expedicoes/{dispatchId}/ack")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalMeshAckResponse> confirmarAckMalhaInstitucional(@PathVariable String mandadoId,
                                                                                                 @PathVariable Long dispatchId,
                                                                                                 @Valid @RequestBody(required = false) DiligenceInstitutionalMeshAckRequest request,
                                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_ack", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.acknowledge(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, dispatchId, request));
    }

    @PostMapping("/mandados/{mandadoId}/malha-institucional/replay")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalMeshReplayResponse> replayMalhaInstitucional(@PathVariable String mandadoId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalMeshReplayRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_replay", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.replay(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @PostMapping("/custodia/{chaveCustodia}/sync/export")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<ChainOfCustodySyncBundleResponse> exportarCustodia(@PathVariable String chaveCustodia,
                                                                             @RequestBody(required = false) ChainOfCustodySyncExportRequest request,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_custodia_sync_export", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.exportBundle(chaveCustodia, request));
    }

    @PostMapping("/custodia/sync/replay")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<ChainOfCustodySyncReplayResponse> replayCustodia(@Valid @RequestBody ChainOfCustodySyncReplayRequest request,
                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_custodia_sync_replay", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.replayVerify(request));
    }

    @GetMapping("/custodia/{chaveCustodia}/sync/events")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<ChainOfCustodySyncEventResponse>> eventosCustodia(@PathVariable String chaveCustodia,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_custodia_sync_events", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.history(chaveCustodia));
    }

}