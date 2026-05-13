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
public class OficialJusticaMandadoController {

    private final OficialJusticaPainelService service;
    private final CapabilityRateLimiter rateLimiter;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final OperationalFunctionCredentialService credentialService;

    public OficialJusticaMandadoController(
            OficialJusticaPainelService service,
            CapabilityRateLimiter rateLimiter,
            InstitutionalPainelSurfaceFacadeService facadeService,
            OperationalFunctionCredentialService credentialService
    ) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.facadeService = facadeService;
        this.credentialService = credentialService;
    }

    @GetMapping("/mandados")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceCollectionResponse> mandados(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_mandados", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialMandados());
    }

    @PostMapping("/mandados/{mandadoId}/cumprimento")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> registrarCumprimento(@PathVariable String mandadoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_cumprimento", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.oficialRegistrarCumprimento(mandadoId, request));
    }

    @PostMapping("/mandados/{mandadoId}/frustracao")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> registrarFrustracao(@PathVariable String mandadoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_frustracao", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.oficialRegistrarFrustracao(mandadoId, request));
    }

    @PostMapping("/penhoras/{processoId}/avaliacao")
    @PreAuthorize("hasRole('OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> registrarAvaliacao(@PathVariable Long processoId, @RequestBody Object request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_avaliacao", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.oficialRegistrarAvaliacao(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_OFICIAL_JUSTICA_CIENTE_INTIMACAO_CHALLENGE)
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OperationalStepUpChallengeResponse> emitirStepUpCienciaIntimacao(@PathVariable Long processoId,
                                                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_ciente_intimacao_challenge", ApiVersion.V1);
        return ResponseEntity.ok(service.issueCienciaIntimacaoChallenge(processoId));
    }

    @PostMapping("/processos/{processoId}/ciente-intimacao")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> confirmarCienciaIntimacao(@PathVariable Long processoId,
                                                                           @Valid @RequestBody(required = false) OficialJusticaCienciaIntimacaoRequest request,
                                                                           @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_ciente_intimacao", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_CIENTE_INTIMACAO", String.valueOf(processoId), unlockToken);
        return ResponseEntity.ok(facadeService.oficialConfirmarCienciaIntimacao(processoId, request));
    }

    @PostMapping("/processos/{processoId}/oficios")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> emitirOficio(@PathVariable Long processoId,
                                                              @Valid @RequestBody OficialJusticaOficioRequest request,
                                                              @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_emitir_oficio", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_EMITIR_OFICIO", String.valueOf(processoId), unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.oficialEmitirOficio(processoId, request));
    }

    @PostMapping("/processos/{processoId}/oficios/resposta")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> responderOficio(@PathVariable Long processoId,
                                                                 @Valid @RequestBody OficialJusticaOficioRequest request,
                                                                 @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_responder_oficio", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_RESPONDER_OFICIO", String.valueOf(processoId), unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.oficialResponderOficio(processoId, request));
    }

    @GetMapping("/oficios/catalogo")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> catalogoOficios(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_catalogo_oficios", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialCatalogoOficios());
    }

    @GetMapping("/oficios/execucoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> listarExecucoesOficios(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_execucoes_oficios", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialExecucoesOficios());
    }

    @GetMapping("/oficios/execucoes/{executionId}")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> statusExecucaoOficio(@PathVariable String executionId,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_status_execucao_oficio", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialStatusExecucaoOficio(executionId));
    }

    @PostMapping("/oficios/execucoes/{executionId}/confirmacao")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> confirmarEntregaOficio(@PathVariable String executionId,
                                                                        @Valid @RequestBody(required = false) OficialJusticaOficioConfirmationRequest request,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_confirmacao_entrega_oficio", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialConfirmarEntregaOficio(executionId, request));
    }

    @GetMapping("/oficios/execucoes/{executionId}/malha")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> malhaExternaOficio(@PathVariable String executionId,
                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_externa_oficio", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialMalhaExternaOficio(executionId));
    }

    @PostMapping("/oficios/execucoes/{executionId}/ack-canal")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> ackCanalOficio(@PathVariable String executionId,
                                                                @Valid @RequestBody(required = false) OficialJusticaOficioChannelAckRequest request,
                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_ack_canal_oficio", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialConfirmarCanalOficio(executionId, request));
    }

    @PostMapping("/oficios/execucoes/{executionId}/ack-cartorio")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> ackCartorioOficio(@PathVariable String executionId,
                                                                   @Valid @RequestBody(required = false) OficialJusticaOficioCartorioAckRequest request,
                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_ack_cartorio_oficio", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialAckCartorioOficio(executionId, request));
    }

    @PostMapping("/oficios/execucoes/{executionId}/reconciliacao")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> reconciliarOficio(@PathVariable String executionId,
                                                                   @Valid @RequestBody(required = false) OficialJusticaOficioReconciliationRequest request,
                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_reconciliacao_oficio", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialReconciliarOficio(executionId, request));
    }

    @PostMapping("/oficios/execucoes/{executionId}/retentativa")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceActionResponse> retentarEntregaOficio(@PathVariable String executionId,
                                                                       @Valid @RequestBody(required = false) OficialJusticaOficioRetryRequest request,
                                                                       Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_retentativa_entrega_oficio", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialRetentarEntregaOficio(executionId, request));
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<Object> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha", ApiVersion.V1);
        return ResponseEntity.ok(service.malhaProcesso(processoId));
    }

}