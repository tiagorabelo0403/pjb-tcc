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
public class OficialJusticaPainelController {

    private final OficialJusticaPainelService service;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;
    private final InstitutionalPainelSurfaceFacadeService facadeService;
    private final DiligenceOperationalCommandCenterService diligenceOperationalCommandCenterService;

    public OficialJusticaPainelController(
            OficialJusticaPainelService service,
            CapabilityRateLimiter rateLimiter,
            PainelSharedExperienceService sharedExperienceService,
            InstitutionalPainelSurfaceFacadeService facadeService,
            DiligenceOperationalCommandCenterService diligenceOperationalCommandCenterService
    ) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
        this.facadeService = facadeService;
        this.diligenceOperationalCommandCenterService = diligenceOperationalCommandCenterService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<PerfilDashboardPayload.OficialJusticaPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_painel", ApiVersion.V1);
        PerfilDashboardPayload.OficialJusticaPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/pendencias-operacionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> pendenciasOperacionais(@RequestParam(defaultValue = "20") int limit,
                                                                           @RequestParam(defaultValue = "TODOS") String rito,
                                                                           @RequestParam(defaultValue = "TODAS") String vara,
                                                                           @RequestParam(defaultValue = "true") boolean somentePendentes,
                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_pendencias_operacionais", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialPendenciasOperacionais(limit, rito, vara, somentePendentes));
    }

    @GetMapping("/processos-nomeados")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> processosNomeados(@RequestParam(defaultValue = "20") int limit,
                                                                      @RequestParam(defaultValue = "TODOS") String rito,
                                                                      @RequestParam(defaultValue = "TODAS") String vara,
                                                                      @RequestParam(defaultValue = "false") boolean somentePendentes,
                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_processos_nomeados", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialProcessosNomeados(limit, rito, vara, somentePendentes));
    }

    @GetMapping("/processos-nomeados/{processoId}/acesso")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaProcessoAcessoResponse> acessoProcessoNomeado(@PathVariable Long processoId,
                                                                                       Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_acesso_processo_nomeado", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialAcessoProcessoNomeado(processoId));
    }

    @GetMapping("/workbench/resumo")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> resumoWorkbench(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_workbench_resumo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialResumoWorkbenchOperacional());
    }

    @GetMapping("/diligencias/fila-viva")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaDiligenciaQueueResponse> filaViva(@RequestParam(defaultValue = "24") int limit,
                                                                          @RequestParam(defaultValue = "TODOS") String rito,
                                                                          @RequestParam(defaultValue = "TODAS") String vara,
                                                                          @RequestParam(defaultValue = "TODAS") String pasta,
                                                                          @RequestParam(defaultValue = "TODAS") String prioridade,
                                                                          @RequestParam(defaultValue = "true") boolean somentePendentes,
                                                                          Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_fila_viva", ApiVersion.V1);
        return ResponseEntity.ok(service.filaDiligenciasViva(limit, rito, vara, pasta, prioridade, somentePendentes));
    }

    @GetMapping("/processos-nomeados/{processoId}/workbench")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaProcessoWorkbenchResponse> workbenchProcesso(@PathVariable Long processoId,
                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_workbench_processo", ApiVersion.V1);
        return ResponseEntity.ok(service.processoWorkbench(processoId));
    }

    @GetMapping("/agenda-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaAgendaOperacionalResponse> agendaOperacional(@RequestParam(defaultValue = "24") int limit,
                                                                                      @RequestParam(defaultValue = "TODOS") String rito,
                                                                                      @RequestParam(defaultValue = "TODAS") String vara,
                                                                                      @RequestParam(defaultValue = "TODAS") String pasta,
                                                                                      @RequestParam(defaultValue = "TODAS") String prioridade,
                                                                                      @RequestParam(defaultValue = "true") boolean somentePendentes,
                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_agenda_operacional", ApiVersion.V1);
        return ResponseEntity.ok(service.agendaOperacional(limit, rito, vara, pasta, prioridade, somentePendentes));
    }

    @GetMapping("/calendario-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaCalendarioOperacionalResponse> calendarioOperacional(@RequestParam(required = false) String month,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_calendario_operacional", ApiVersion.V1);
        YearMonth target;
        try {
            target = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        } catch (Exception ex) {
            target = YearMonth.now();
        }
        return ResponseEntity.ok(service.calendarioOperacional(target));
    }

    @GetMapping("/notificacoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<Map<String, Object>> notificacoes(@RequestParam(defaultValue = "20") int limit,
                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_notificacoes", ApiVersion.V1);
        return ResponseEntity.ok(service.notificacoes(limit));
    }

    @GetMapping("/notificacoes/runtime")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<Map<String, Object>> notificacoesRuntime(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_notificacoes_runtime", ApiVersion.V1);
        return ResponseEntity.ok(service.notificacoesRuntime());
    }

    @GetMapping("/balcao-virtual/salas")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaBalcaoVirtualChatResponse> balcaoVirtualSalas(@RequestParam(defaultValue = "12") int limit,
                                                                                       Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_balcao_virtual_salas", ApiVersion.V1);
        return ResponseEntity.ok(service.balcaoVirtualSalas(limit));
    }

    @GetMapping("/balcao-virtual/processos/{processoId}/sala")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaBalcaoVirtualChatResponse> balcaoVirtualSalaProcesso(@PathVariable Long processoId,
                                                                                              @RequestParam(defaultValue = "8") int previewLimit,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_balcao_virtual_sala", ApiVersion.V1);
        return ResponseEntity.ok(service.balcaoVirtualSalaProcesso(processoId, previewLimit));
    }

    @GetMapping("/balcao-virtual/processos/{processoId}/mensagens")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<ChatMensagemResponse>> balcaoVirtualHistorico(@PathVariable Long processoId,
                                                                             @RequestParam(defaultValue = "40") int limit,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_balcao_virtual_historico", ApiVersion.V1);
        return ResponseEntity.ok(service.balcaoVirtualHistorico(processoId, limit));
    }

    @PostMapping("/balcao-virtual/processos/{processoId}/mensagens")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<Map<String, Object>> balcaoVirtualEnviar(@PathVariable Long processoId,
                                                                   @Valid @RequestBody OficialJusticaBalcaoVirtualMessageRequest request,
                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_balcao_virtual_enviar", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.balcaoVirtualEnviar(processoId, request));
    }

    @GetMapping("/painel/comando-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceOperationalCommandCenterResponse> comandoOperacional(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_comando_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalCommandCenterService.snapshot(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, 30, 10));
    }

    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "oficial_justica_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("OFICIAL_JUSTICA"));
    }

}