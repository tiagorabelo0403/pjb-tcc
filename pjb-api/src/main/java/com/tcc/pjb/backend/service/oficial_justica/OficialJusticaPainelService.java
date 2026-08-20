package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAccessContextMaterializationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualChatResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCienciaIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualMessageRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoWorkbenchResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioCartorioAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioChannelAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioConfirmationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioReconciliationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRetryRequest;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application.DestinatarioProcessualResolverApplicationService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final PerfilCapabilityMatrixService capabilityMatrixService;
    private final PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService;
    private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final DestinatarioProcessualResolverApplicationService destinatarioResolverApplicationService;
    private final OficialJusticaOficioCatalogService oficioCatalogService;
    private final OficialJusticaTraceableCommunicationLedgerService traceableCommunicationLedgerService;
    private final OficialJusticaInstitutionalDispatchService institutionalDispatchService;
    private final OficialJusticaEnderecoTriageService enderecoTriageService;
    private final OficialJusticaPortfolioProcessualService portfolioProcessualService;
    private final OficialJusticaWorkbenchService workbenchService;
    private final OficialJusticaOficioSecurityService oficioSecurityService;
    private final OficialJusticaAgendaOperacionalService agendaOperacionalService;
    private final OficialJusticaCalendarioOperacionalService calendarioOperacionalService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final OficialJusticaBalcaoVirtualService balcaoVirtualService;
    private final OficialJusticaNotificationCenterService notificationCenterService;
    private final OficialJusticaPanelEgressService panelEgressService;
    private final CalendarInstitutionalBridgeService institutionalBridgeService;
    private final InstitutionalDocumentSecurityGateApplicationService institutionalDocumentSecurityGateApplicationService;
    private final InstitutionalAccessContextMaterializationApplicationService institutionalAccessContextMaterializationApplicationService;
    private final PainelSharedExperienceService sharedExperienceService;
    private final PainelSignalReflectionService signalReflectionService;
    private final PainelNativeCollectionCompositionService collectionCompositionService;
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;
    private final OficialJusticaCommunicationFormalModelService communicationFormalModelService;

    public OficialJusticaPainelService(PerfilDashboardContextFactory contextFactory,
                                       PainelServiceCommons commons,
                                       ProcessoRepository processoRepository,
                                       WorkItemRepository workItemRepository,
                                       PjbAuthorizationService authorizationService,
                                       PerfilCapabilityMatrixService capabilityMatrixService,
                                       PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService,
                                       InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
                                       InstitutionalActorRoutingService institutionalActorRoutingService,
                                       InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                       InstitutionalPanelBrandingService institutionalPanelBrandingService,
                                       DestinatarioProcessualResolverApplicationService destinatarioResolverApplicationService,
                                       OficialJusticaOficioCatalogService oficioCatalogService,
                                       OficialJusticaTraceableCommunicationLedgerService traceableCommunicationLedgerService,
                                       OficialJusticaInstitutionalDispatchService institutionalDispatchService,
                                       OficialJusticaEnderecoTriageService enderecoTriageService,
                                       OficialJusticaPortfolioProcessualService portfolioProcessualService,
                                       OficialJusticaWorkbenchService workbenchService,
                                       OficialJusticaOficioSecurityService oficioSecurityService,
                                       OficialJusticaAgendaOperacionalService agendaOperacionalService,
                                       OficialJusticaCalendarioOperacionalService calendarioOperacionalService,
                                       OficialJusticaContextEnvelopeService contextEnvelopeService,
                                       OficialJusticaBalcaoVirtualService balcaoVirtualService,
                                       OficialJusticaNotificationCenterService notificationCenterService,
                                       OficialJusticaPanelEgressService panelEgressService,
                                       CalendarInstitutionalBridgeService institutionalBridgeService,
                                       InstitutionalDocumentSecurityGateApplicationService institutionalDocumentSecurityGateApplicationService,
                                       InstitutionalAccessContextMaterializationApplicationService institutionalAccessContextMaterializationApplicationService,
                                       PainelSharedExperienceService sharedExperienceService,
                                       PainelSignalReflectionService signalReflectionService,
                                       PainelNativeCollectionCompositionService collectionCompositionService,
                                       PainelActionSurfaceCompositionService actionSurfaceCompositionService,
                                       PainelExecutionSurfaceCompositionService executionSurfaceCompositionService,
                                       OficialJusticaCommunicationFormalModelService communicationFormalModelService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.capabilityMatrixService = capabilityMatrixService;
        this.intelligenceSummaryService = intelligenceSummaryService;
        this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
        this.institutionalActorRoutingService = institutionalActorRoutingService;
        this.institutionalMultimediaWorkspaceService = institutionalMultimediaWorkspaceService;
        this.institutionalPanelBrandingService = institutionalPanelBrandingService;
        this.destinatarioResolverApplicationService = destinatarioResolverApplicationService;
        this.oficioCatalogService = oficioCatalogService;
        this.traceableCommunicationLedgerService = traceableCommunicationLedgerService;
        this.institutionalDispatchService = institutionalDispatchService;
        this.enderecoTriageService = enderecoTriageService;
        this.portfolioProcessualService = portfolioProcessualService;
        this.workbenchService = workbenchService;
        this.oficioSecurityService = oficioSecurityService;
        this.agendaOperacionalService = agendaOperacionalService;
        this.calendarioOperacionalService = calendarioOperacionalService;
        this.contextEnvelopeService = contextEnvelopeService;
        this.balcaoVirtualService = balcaoVirtualService;
        this.notificationCenterService = notificationCenterService;
        this.panelEgressService = panelEgressService;
        this.institutionalBridgeService = institutionalBridgeService;
        this.institutionalDocumentSecurityGateApplicationService = institutionalDocumentSecurityGateApplicationService;
        this.institutionalAccessContextMaterializationApplicationService = institutionalAccessContextMaterializationApplicationService;
        this.sharedExperienceService = sharedExperienceService;
        this.signalReflectionService = signalReflectionService;
        this.collectionCompositionService = collectionCompositionService;
        this.actionSurfaceCompositionService = actionSurfaceCompositionService;
        this.executionSurfaceCompositionService = executionSurfaceCompositionService;
        this.communicationFormalModelService = communicationFormalModelService;
    }

    public PerfilDashboardPayload.OficialJusticaPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<WorkItem> inbox = panelEgressService.reconcileVisibility(usuario, commons.inboxHibrido(usuario, 30)).visibleItems();
        int pendentes = (int) inbox.stream().filter(this::isMandado).count();
        int cumpridos = (int) inbox.stream().filter(item -> item.getStatus() == WorkItemStatus.CONCLUIDO).count();
        int frustrados = (int) inbox.stream().filter(item -> commons.titleContains(item, "FRUSTRADO", "NEGATIVO")).count();
        List<PerfilDashboardPayload.OficialJusticaPayload.MandadoResumo> proximos = inbox.stream().filter(this::isMandado).limit(8).map(item -> new PerfilDashboardPayload.OficialJusticaPayload.MandadoResumo(String.valueOf(item.getId()), item.getType() != null ? item.getType().name() : "MANDADO", item.getComarca(), item.getDueAt() != null ? item.getDueAt().toString() : null, item.getProcesso() != null ? item.getProcesso().getNumeroProcesso() : null)).toList();
        List<String> penhoras = inbox.stream().filter(item -> commons.titleContains(item, "PENHORA", "AVALIACAO")).limit(8).map(commons::resumo).toList();
        PerfilDashboardPayload.LocalizadorGovernadoResumo localizadorGovernado = new PerfilDashboardPayload.LocalizadorGovernadoResumo(
                authorizationService.canLocatePessoaByCpf(usuario),
                intelligenceSummaryService.resumir(usuario, PessoaLocalizacaoService.CanalConsulta.OFICIAL_JUSTICA, 8)
        );
        Map<String, Object> panelBranding = institutionalPanelBrandingService.resolve("OFICIAL_JUSTICA", "PAINEL_OFICIAL_JUSTICA", usuario.getTipoUsuario());
        Map<String, Object> pendenciasOperacionais = portfolioProcessualService.painelResumoPendencias();
        Map<String, Object> portfolioProcessualNomeado = portfolioProcessualService.painelResumoProcessosNomeados();
        Map<String, Object> rastreioOperacional = enderecoTriageService.painelResumo();
        Map<String, Object> operationalWorkbench = workbenchService.painelResumo();
        Map<String, Object> agendaOperacional = agendaOperacionalService.painelResumo();
        LinkedHashMap<String, Object> organizacaoOperacional = new LinkedHashMap<>(contextEnvelopeService.oficialEnvelope(usuario, null));
        if (pendenciasOperacionais.get("scope") instanceof Map<?, ?> scopePendencias) {
            organizacaoOperacional.put("scopePendencias", scopePendencias);
        }
        if (agendaOperacional.get("scope") instanceof Map<?, ?> scopeAgenda) {
            organizacaoOperacional.put("scopeAgenda", scopeAgenda);
        }
        if (agendaOperacional.get("summary") instanceof Map<?, ?> summaryAgenda) {
            organizacaoOperacional.put("agendaResumo", summaryAgenda);
        }
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("OFICIAL_JUSTICA");
        Map<String, Object> operationalSignals = signalReflectionService.deriveSignals("OFICIAL_JUSTICA", sharedExperience, pendentes, ctx.prazoRadar().size(), "CUMPRIMENTO_EXTERNO");
        Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("OFICIAL_JUSTICA", operationalSignals);
        proximos = collectionCompositionService.composeList("OFICIAL_JUSTICA", "PROXIMOS_MANDADOS", proximos, operationalSignals, nativeComposition);
        penhoras = collectionCompositionService.composeList("OFICIAL_JUSTICA", "PENHORAS_AGENDADAS", penhoras, operationalSignals, nativeComposition);
        Map<String, Object> collectionComposition = collectionCompositionService.buildCollectionComposition("OFICIAL_JUSTICA", operationalSignals, nativeComposition, Map.of(
                "proximosMandados", proximos,
                "penhorasAgendadas", penhoras
        ));
        Map<String, Object> actionSurface = actionSurfaceCompositionService.buildActionSurface("OFICIAL_JUSTICA", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = executionSurfaceCompositionService.buildExecutionSurface("OFICIAL_JUSTICA", operationalSignals, nativeComposition, collectionComposition, actionSurface);
        CalendarInstitutionalBridgeResponse institutionalBridge = institutionalBridgeService.bridgeForUser(usuario, java.time.LocalDate.now(java.time.ZoneOffset.UTC), java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(14), null);
        var institutionalFocus = institutionalBridgeService.focus(institutionalBridge);
        LinkedHashMap<String, Object> calendarioOperacionalMutable = new LinkedHashMap<>(calendarioOperacionalService.calendario(java.time.YearMonth.now(java.time.ZoneOffset.UTC)).toPanelMap());
        calendarioOperacionalMutable.put("institutionalBridge", institutionalBridgeService.toPanelMap(institutionalBridge));
        calendarioOperacionalMutable.put("institutionalFocus", institutionalBridgeService.toFocusPanelMap(institutionalFocus));
        Map<String, Object> calendarioOperacional = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "CALENDARIO", calendarioOperacionalMutable, operationalSignals);
        calendarioOperacional = collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "CALENDARIO", calendarioOperacional, operationalSignals, nativeComposition);
        calendarioOperacional = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "CALENDARIO", calendarioOperacional, actionSurface, nativeComposition);
        calendarioOperacional = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "CALENDARIO", calendarioOperacional, executionSurface, nativeComposition);
        Map<String, Object> balcaoVirtual = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "OPERACIONAL", balcaoVirtualService.painelResumo(), operationalSignals);
        balcaoVirtual = collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", balcaoVirtual, operationalSignals, nativeComposition);
        balcaoVirtual = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", balcaoVirtual, actionSurface, nativeComposition);
        balcaoVirtual = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", balcaoVirtual, executionSurface, nativeComposition);
        Map<String, Object> notificationCenter = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "PENDENCIAS", notificationCenterService.painelResumo(), operationalSignals);
        notificationCenter = collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "PENDENCIAS", notificationCenter, operationalSignals, nativeComposition);
        notificationCenter = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "PENDENCIAS", notificationCenter, actionSurface, nativeComposition);
        notificationCenter = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "PENDENCIAS", notificationCenter, executionSurface, nativeComposition);
        organizacaoOperacional = new LinkedHashMap<>(signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "OPERACIONAL", organizacaoOperacional, operationalSignals));
        organizacaoOperacional = new LinkedHashMap<>(collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", organizacaoOperacional, operationalSignals, nativeComposition));
        organizacaoOperacional = new LinkedHashMap<>(actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", organizacaoOperacional, actionSurface, nativeComposition));
        organizacaoOperacional = new LinkedHashMap<>(executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", organizacaoOperacional, executionSurface, nativeComposition));
        pendenciasOperacionais = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "PENDENCIAS", pendenciasOperacionais, operationalSignals);
        pendenciasOperacionais = collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "PENDENCIAS", pendenciasOperacionais, operationalSignals, nativeComposition);
        pendenciasOperacionais = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "PENDENCIAS", pendenciasOperacionais, actionSurface, nativeComposition);
        pendenciasOperacionais = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "PENDENCIAS", pendenciasOperacionais, executionSurface, nativeComposition);
        rastreioOperacional = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "OPERACIONAL", rastreioOperacional, operationalSignals);
        rastreioOperacional = collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", rastreioOperacional, operationalSignals, nativeComposition);
        rastreioOperacional = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", rastreioOperacional, actionSurface, nativeComposition);
        rastreioOperacional = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "OPERACIONAL", rastreioOperacional, executionSurface, nativeComposition);
        operationalWorkbench = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "WORKBENCH", operationalWorkbench, operationalSignals);
        operationalWorkbench = collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "WORKBENCH", operationalWorkbench, operationalSignals, nativeComposition);
        operationalWorkbench = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "WORKBENCH", operationalWorkbench, actionSurface, nativeComposition);
        operationalWorkbench = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "WORKBENCH", operationalWorkbench, executionSurface, nativeComposition);
        agendaOperacional = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "AGENDA", agendaOperacional, operationalSignals);
        agendaOperacional = collectionCompositionService.decorateBlock("OFICIAL_JUSTICA", "AGENDA", agendaOperacional, operationalSignals, nativeComposition);
        agendaOperacional = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "AGENDA", agendaOperacional, actionSurface, nativeComposition);
        agendaOperacional = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "AGENDA", agendaOperacional, executionSurface, nativeComposition);
        Map<String, Object> panelVisualIdentity = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "VISUAL_IDENTITY", castMap(panelBranding.get("panelVisualIdentity")), operationalSignals);
        panelVisualIdentity = actionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "VISUAL_IDENTITY", panelVisualIdentity, actionSurface, nativeComposition);
        panelVisualIdentity = executionSurfaceCompositionService.decorateBlock("OFICIAL_JUSTICA", "VISUAL_IDENTITY", panelVisualIdentity, executionSurface, nativeComposition);
        String etag = commons.etag("OFICIAL", usuario.getId(), pendentes, cumpridos, frustrados, proximos, penhoras, ctx.behavioralAudit(), localizadorGovernado.metricas(), organizacaoOperacional, pendenciasOperacionais, portfolioProcessualNomeado, rastreioOperacional, operationalWorkbench, agendaOperacional, calendarioOperacional, balcaoVirtual, notificationCenter, institutionalBridge, operationalSignals);
        return new PerfilDashboardPayload.OficialJusticaPayload(
                etag,
                ctx.generatedAt(),
                ctx.perfilAtivo(),
                ctx.tratamento(),
                ctx.pendencias(),
                ctx.prazoRadar(),
                ctx.sessionRisk(),
                ctx.sigiloAtivo(),
                ctx.plantao(),
                ctx.onboarding(),
                ctx.externalSystems(),
                ctx.behavioralAudit(),
                usuario.getUf() + ":" + usuario.getComarca(),
                pendentes,
                cumpridos,
                frustrados,
                proximos,
                usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR,
                penhoras,
                authorizationService.canLocatePessoaByCpf(usuario),
                capabilityMatrixService.capacidadesOficial(usuario),
                localizadorGovernado,
                castMap(panelBranding.get("institutionalBranding")),
                panelVisualIdentity,
                organizacaoOperacional,
                pendenciasOperacionais,
                portfolioProcessualNomeado,
                rastreioOperacional,
                operationalWorkbench,
                agendaOperacional,
                calendarioOperacional,
                balcaoVirtual,
                notificationCenter,
                institutionalFocus,
                institutionalBridge,
                operationalSignals,
                nativeComposition,
                collectionComposition,
                actionSurface,
                executionSurface,
                sharedExperience
        );
    }

    public List<Map<String, Object>> listarMandados() {
        Usuario usuario = contextFactory.build().usuario();
        return panelEgressService.reconcileVisibility(usuario, commons.inboxHibrido(usuario, 20)).visibleItems().stream().filter(this::isMandado).map(commons::mapWorkItem).toList();
    }

    public Map<String, Object> notificacoes(int limit) {
        return notificationCenterService.listar(limit);
    }

    public Map<String, Object> notificacoesRuntime() {
        return notificationCenterService.runtimeStatus();
    }

    public OperationalStepUpChallengeResponse issueCienciaIntimacaoChallenge(Long processoId) {
        return notificationCenterService.issueCienciaProcessualChallenge(processoId);
    }

    public Map<String, Object> confirmarCienciaIntimacao(Long processoId, OficialJusticaCienciaIntimacaoRequest request) {
        return notificationCenterService.confirmarCienciaProcessual(processoId, request);
    }

    public com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCalendarioOperacionalResponse calendarioOperacional(java.time.YearMonth month) {
        return calendarioOperacionalService.calendario(month);
    }

    public Map<String, Object> catalogoOficios() {
        return oficioCatalogService.catalogo(contextFactory.build().usuario().getTipoUsuario());
    }

    public Map<String, Object> listarExecucoesOficios(int limit) {
        return traceableCommunicationLedgerService.recentExecutions(contextFactory.build().usuario().getTipoUsuario(), limit);
    }

    public Map<String, Object> statusExecucaoOficio(String executionId) {
        return traceableCommunicationLedgerService.executionStatus(contextFactory.build().usuario().getTipoUsuario(), executionId);
    }

    public Map<String, Object> confirmarEntregaOficio(String executionId, OficialJusticaOficioConfirmationRequest request) {
        return traceableCommunicationLedgerService.confirmDelivery(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> confirmarCanalOficio(String executionId, OficialJusticaOficioChannelAckRequest request) {
        return traceableCommunicationLedgerService.confirmChannelDelivery(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> ackCartorioOficio(String executionId, OficialJusticaOficioCartorioAckRequest request) {
        return traceableCommunicationLedgerService.acknowledgeCartorio(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> reconciliarOficio(String executionId, OficialJusticaOficioReconciliationRequest request) {
        return traceableCommunicationLedgerService.reconcileExecution(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> malhaExternaOficio(String executionId) {
        return traceableCommunicationLedgerService.externalMeshStatus(contextFactory.build().usuario().getTipoUsuario(), executionId);
    }

    public Map<String, Object> retentarEntregaOficio(String executionId, OficialJusticaOficioRetryRequest request) {
        return traceableCommunicationLedgerService.retryExecution(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
        authorizationService.requireVinculoInstitucionalComProcesso(processoId);
        return institutionalActorTopologyMeshService.snapshot(processoId);
    }

    @Transactional
    public Map<String, Object> registrarCumprimento(String mandadoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        WorkItem item = resolveMandado(mandadoId);
        Map<String, Object> formalization = communicationFormalModelService.formalizeOutcome(item.getProcesso(), item, usuario, request, false);
        item.setStatus(WorkItemStatus.CONCLUIDO);
        item.setDescricao(communicationFormalModelService.appendFormalTrace(item.getDescricao(), item.getProcesso(), item, usuario, request, false, formalization));
        item = workItemRepository.save(item);
        commons.publishTerritoryHistory(usuario, "OFICIAL", "MANDADO_CUMPRIDO", "Mandado cumprido registrado.", item.getProcesso(), item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.putAll(commons.mapWorkItem(item));
        out.put("formalization", formalization);
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "CERTIDAO_OFICIAL_JUSTICA",
                        item.getProcesso() != null ? item.getProcesso().getId() : null,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> registrarFrustracao(String mandadoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        WorkItem item = resolveMandado(mandadoId);
        Map<String, Object> formalization = communicationFormalModelService.formalizeOutcome(item.getProcesso(), item, usuario, request, true);
        item.setStatus(WorkItemStatus.CONCLUIDO);
        item.setDescricao(communicationFormalModelService.appendFormalTrace(item.getDescricao(), item.getProcesso(), item, usuario, request, true, formalization));
        item = workItemRepository.save(item);
        Processo processo = item.getProcesso();
        InstitutionalActorRoutingService.InstitutionalRoute route = processo != null
                ? institutionalActorRoutingService.secretaryExecution(processo.getId(), "CERTIDAO_NEGATIVA")
                : new InstitutionalActorRoutingService.InstitutionalRoute("SECRETARIA_CUMPRIMENTO", "SECRETARIA_CUMPRIMENTO", TipoUsuario.SERVIDOR_FORUM, "CERTIDAO_NEGATIVA", null, "Fallback operacional sem processo associado.", Map.of());
        WorkItem followup = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo != null ? processo.getFaseAtual() : null)
                .templateCode("CERTIFICAR_NEGATIVO:" + item.getId())
                .type(WorkItemType.EXPEDICAO)
                .titulo("Certificar negativo e aguardar nova ordem")
                .descricao("Fluxo automático após cumprimento frustrado do oficial de justiça")
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .blocking(false)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal("Certidão negativa e nova ordem judicial")
                .build();
        followup = workItemRepository.save(followup);
        commons.publishTerritoryHistory(usuario, "ASSESSOR", "CUMPRIMENTO_FRUSTRADO", "Nova tarefa de certificação negativa enviada à fila da secretaria.", processo, followup.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mandado", commons.mapWorkItem(item));
        out.put("followup", commons.mapWorkItem(followup));
        out.put("formalization", formalization);
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "CERTIDAO_OFICIAL_JUSTICA",
                        processo != null ? processo.getId() : null,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> registrarAvaliacao(Long processoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        Processo processo = resolveProcesso(processoId);
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.officialJustice(processoId, true, "AVALIACAO_PENHORA");
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo != null ? processo.getFaseAtual() : null)
                .templateCode("AVALIACAO_PENHORA:" + processoId + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.CALCULO)
                .titulo("Avaliação de bens penhorados registrada")
                .descricao(String.valueOf(request))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .assignedUser(usuario)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(2)
                .dueAt(Instant.now())
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .build();
        item = workItemRepository.save(item);
        commons.publishUserHistory(usuario, "OFICIAL", "AVALIACAO_REGISTRADA", "Avaliação patrimonial registrada.", item.getProcesso(), item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.putAll(commons.mapWorkItem(item));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "AVALIACAO_OFICIAL_JUSTICA",
                        processoId,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> emitirOficio(Long processoId, OficialJusticaOficioRequest request) {
        Processo processo = resolveProcessoObrigatorio(processoId);
        Usuario usuario = contextFactory.build().usuario();
        oficioSecurityService.enforceCanSendIntoProcess(processo, usuario, "OFICIO_OFICIAL_JUSTICA");
        OficialJusticaOficioRequest safe = request == null
                ? new OficialJusticaOficioRequest("Ofício do oficial de justiça", "Destinatário institucional", "Conteúdo não informado", "Fundamento não informado", null, null, null, null, List.of(), Map.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)
                : request;
        var destinatario = OficialJusticaOficioWorkflowSupport.resolveDestinatario(destinatarioResolverApplicationService, safe);
        OficialJusticaOficioCatalogService.OficioTypeDefinition oficioType = oficioCatalogService.resolveType(safe.tipoOficioCode(), false);
        OficialJusticaOficioCatalogService.TemplateDefinition template = oficioCatalogService.resolveTemplate(safe.minutaCode(), oficioType);
        Map<String, Object> destinatarioMap = OficialJusticaOficioWorkflowSupport.buildDestinatarioMap(destinatario, safe);
        Map<String, Object> minutaGovernada = oficioCatalogService.renderMinutaGovernada(safe, processo, usuario, destinatarioMap, oficioType, template, false);
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.officialJustice(processoId, usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "OFICIO_OFICIAL_JUSTICA");
        var institutionalSignatureGate = institutionalDocumentSecurityGateApplicationService.enforce(
                null,
                null,
                InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO,
                "OFICIO_OFICIAL_JUSTICA",
                true);
        WorkItem oficio = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("OFICIO_OFICIAL:" + processoId + ':' + usuario.getId() + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.EXPEDICAO)
                .titulo("Ofício do oficial de justiça — " + safe.assunto())
                .descricao(OficialJusticaOficioWorkflowSupport.composeOficioDescricao(safe, false, oficioType, template, destinatarioMap, minutaGovernada))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .assignedUser(usuario)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .dueAt(Instant.now())
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(OficialJusticaOficioWorkflowSupport.normalizeFundamento(safe.fundamento()))
                .build();
        oficio = workItemRepository.save(oficio);
        oficioSecurityService.enforceOriginalOnlyForDirectProcessSubmission(safe, minutaGovernada, true);
        WorkItem juntadaDireta = OficialJusticaOficioWorkflowSupport.criarJuntadaDiretaNoProcesso(workItemRepository, processo, usuario, oficio, safe, minutaGovernada, false);
        Map<String, Object> traceableExecution = traceableCommunicationLedgerService.registerExecution(
                usuario.getTipoUsuario(),
                processoId,
                "OFICIO_OFICIAL_JUSTICA",
                "JUNTADA_DIRETA_PROCESSO_ORIGINAL",
                template.code(),
                oficioType.asMap(),
                destinatarioMap,
                minutaGovernada,
                false
        );
        String executionId = String.valueOf(traceableExecution.get("executionId"));
        Map<String, Object> dispatchTopology = OficialJusticaOficioWorkflowSupport.directProcessDispatchTopology(processo, usuario, juntadaDireta, executionId, destinatarioMap, minutaGovernada, false);
        traceableExecution = traceableCommunicationLedgerService.attachDispatchTopology(usuario.getTipoUsuario(), executionId, dispatchTopology);
        commons.publishUserHistory(usuario, "OFICIAL", "OFICIO_REGISTRADO", "Ofício do oficial de justiça registrado e protocolado diretamente no processo dentro do PJB.", processo, oficio.getId());
        commons.publishTerritoryHistory(usuario, "OFICIAL", "OFICIO_OFICIAL_JUNTADA_DIRETA", "Ofício original do oficial juntado diretamente no processo sem balcão intermediário.", processo, juntadaDireta.getId());
        Map<String, Object> securityEnvelope = oficioSecurityService.envelope(processo, usuario, "OFICIO_OFICIAL_JUSTICA");
        Map<String, Object> originalOnlyEnvelope = oficioSecurityService.originalOnlyEnvelope(safe, minutaGovernada, true);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "OFICIO_REGISTRADO_DIRETO_NO_PROCESSO");
        out.put("processoId", processoId);
        out.put("workItemId", oficio.getId());
        out.put("workflowAxis", route.routeAxis());
        out.put("oficioType", oficioType.asMap());
        out.put("minutaGovernada", minutaGovernada);
        out.put("destinatarioResolvido", destinatarioMap);
        out.put("traceableExecution", traceableExecution);
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("institutionalDispatch", dispatchTopology);
        out.put("registeredInsideNamedProcess", Boolean.TRUE);
        out.put("protocoladoDiretoNoProcesso", Boolean.TRUE);
        out.put("securityEnvelope", securityEnvelope);
        out.put("originalOnlyEnvelope", originalOnlyEnvelope);
        out.put("institutionalSignatureGate", institutionalSignatureGate.asMap());
        out.put("institutionalAccessContext", institutionalAccessContextMaterializationApplicationService.materializar(institutionalSignatureGate.affiliationId(), institutionalSignatureGate.nominationId()).asMap());
        out.put("oficio", commons.mapWorkItem(oficio));
        out.put("juntadaDiretaProcesso", commons.mapWorkItem(juntadaDireta));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "OFICIO_OFICIAL_JUSTICA",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> responderOficio(Long processoId, OficialJusticaOficioRequest request) {
        Processo processo = resolveProcessoObrigatorio(processoId);
        Usuario usuario = contextFactory.build().usuario();
        oficioSecurityService.enforceCanSendIntoProcess(processo, usuario, "RESPOSTA_OFICIO_OFICIAL_JUSTICA");
        OficialJusticaOficioRequest safe = request == null
                ? new OficialJusticaOficioRequest("Resposta a ofício", "Destinatário institucional", "Resposta não informada", "Fundamento não informado", null, null, null, null, List.of(), Map.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)
                : request;
        var destinatario = OficialJusticaOficioWorkflowSupport.resolveDestinatario(destinatarioResolverApplicationService, safe);
        OficialJusticaOficioCatalogService.OficioTypeDefinition oficioType = oficioCatalogService.resolveType(safe.tipoOficioCode(), true);
        OficialJusticaOficioCatalogService.TemplateDefinition template = oficioCatalogService.resolveTemplate(safe.minutaCode(), oficioType);
        Map<String, Object> destinatarioMap = OficialJusticaOficioWorkflowSupport.buildDestinatarioMap(destinatario, safe);
        Map<String, Object> minutaGovernada = oficioCatalogService.renderMinutaGovernada(safe, processo, usuario, destinatarioMap, oficioType, template, true);
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.officialJustice(processoId, usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "RESPOSTA_OFICIO_OFICIAL_JUSTICA");
        var institutionalSignatureGate = institutionalDocumentSecurityGateApplicationService.enforce(
                null,
                null,
                InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO,
                "RESPOSTA_OFICIO_OFICIAL_JUSTICA",
                true);
        WorkItem resposta = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("RESPOSTA_OFICIO_OFICIAL:" + processoId + ':' + usuario.getId() + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.EXPEDICAO)
                .titulo("Resposta a ofício pelo oficial de justiça — " + safe.assunto())
                .descricao(OficialJusticaOficioWorkflowSupport.composeOficioDescricao(safe, true, oficioType, template, destinatarioMap, minutaGovernada))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .assignedUser(usuario)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .dueAt(Instant.now())
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(OficialJusticaOficioWorkflowSupport.normalizeFundamento(safe.fundamento()))
                .build();
        resposta = workItemRepository.save(resposta);
        oficioSecurityService.enforceOriginalOnlyForDirectProcessSubmission(safe, minutaGovernada, true);
        WorkItem juntadaDireta = OficialJusticaOficioWorkflowSupport.criarJuntadaDiretaNoProcesso(workItemRepository, processo, usuario, resposta, safe, minutaGovernada, true);
        Map<String, Object> traceableExecution = traceableCommunicationLedgerService.registerExecution(
                usuario.getTipoUsuario(),
                processoId,
                "RESPOSTA_OFICIO_OFICIAL_JUSTICA",
                "JUNTADA_DIRETA_PROCESSO_ORIGINAL",
                template.code(),
                oficioType.asMap(),
                destinatarioMap,
                minutaGovernada,
                false
        );
        String executionId = String.valueOf(traceableExecution.get("executionId"));
        Map<String, Object> dispatchTopology = OficialJusticaOficioWorkflowSupport.directProcessDispatchTopology(processo, usuario, juntadaDireta, executionId, destinatarioMap, minutaGovernada, true);
        traceableExecution = traceableCommunicationLedgerService.attachDispatchTopology(usuario.getTipoUsuario(), executionId, dispatchTopology);
        commons.publishUserHistory(usuario, "OFICIAL", "RESPOSTA_OFICIO_REGISTRADA", "Resposta a ofício registrada e protocolada diretamente no processo dentro do PJB.", processo, resposta.getId());
        commons.publishTerritoryHistory(usuario, "OFICIAL", "RESPOSTA_OFICIO_JUNTADA_DIRETA", "Resposta a ofício do oficial juntada diretamente no processo sem balcão intermediário.", processo, juntadaDireta.getId());
        Map<String, Object> securityEnvelope = oficioSecurityService.envelope(processo, usuario, "RESPOSTA_OFICIO_OFICIAL_JUSTICA");
        Map<String, Object> originalOnlyEnvelope = oficioSecurityService.originalOnlyEnvelope(safe, minutaGovernada, true);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "RESPOSTA_OFICIO_REGISTRADA_DIRETO_NO_PROCESSO");
        out.put("processoId", processoId);
        out.put("workItemId", resposta.getId());
        out.put("workflowAxis", route.routeAxis());
        out.put("oficioType", oficioType.asMap());
        out.put("minutaGovernada", minutaGovernada);
        out.put("destinatarioResolvido", destinatarioMap);
        out.put("traceableExecution", traceableExecution);
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("institutionalDispatch", dispatchTopology);
        out.put("registeredInsideNamedProcess", Boolean.TRUE);
        out.put("protocoladoDiretoNoProcesso", Boolean.TRUE);
        out.put("securityEnvelope", securityEnvelope);
        out.put("originalOnlyEnvelope", originalOnlyEnvelope);
        out.put("institutionalSignatureGate", institutionalSignatureGate.asMap());
        out.put("institutionalAccessContext", institutionalAccessContextMaterializationApplicationService.materializar(institutionalSignatureGate.affiliationId(), institutionalSignatureGate.nominationId()).asMap());
        out.put("respostaOficio", commons.mapWorkItem(resposta));
        out.put("juntadaDiretaProcesso", commons.mapWorkItem(juntadaDireta));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "RESPOSTA_OFICIO_OFICIAL_JUSTICA",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        false
                )
        ));
        return out;
    }

    public Map<String, Object> resumoRastreioOperacional() {
        return enderecoTriageService.painelResumo();
    }

    public com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaEnderecoTriageResponse triagemEnderecos(int limit,
                                                                                                                boolean incluirEnderecoEstrito,
                                                                                                                boolean incluirProntuario,
                                                                                                                boolean incluirRestricoes) {
        return enderecoTriageService.triagem(limit, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes);
    }

    public com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPessoaRastreioResponse rastrearMandado(String mandadoId,
                                                                                                                boolean incluirEnderecoEstrito,
                                                                                                                boolean incluirProntuario,
                                                                                                                boolean incluirRestricoes) {
        return enderecoTriageService.rastrearMandado(mandadoId, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes);
    }

    public com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPessoaRastreioResponse rastrearProcessoAlvo(Long processoId,
                                                                                                                     String polo,
                                                                                                                     boolean incluirEnderecoEstrito,
                                                                                                                     boolean incluirProntuario,
                                                                                                                     boolean incluirRestricoes) {
        return enderecoTriageService.rastrearProcessoAlvo(processoId, polo, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes);
    }

    public List<Map<String, Object>> gerarRotaDia() {
        return agendaOperacionalService.agenda(24, "TODOS", "TODAS", "TODAS", "TODAS", true).agenda().stream()
                .map(stop -> {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("ordem", stop.ordem());
                    row.put("workItemId", stop.workItemId());
                    row.put("processoId", stop.processoId());
                    row.put("processoNumero", stop.processoNumero());
                    row.put("vara", stop.vara());
                    row.put("tribunal", stop.tribunal());
                    row.put("esfera", stop.esfera());
                    row.put("prioridadeOperacional", stop.prioridadeOperacional());
                    row.put("statusOperacional", stop.statusOperacional());
                    row.put("statusLabel", stop.statusLabel());
                    row.put("corStatus", stop.corStatus());
                    row.put("corAndamento", stop.corAndamento());
                    row.put("chegadaEstimada", stop.chegadaEstimada());
                    row.put("classificacaoRota", stop.classificacaoRota());
                    row.put("podeEnviarNoProcesso", stop.podeEnviarNoProcesso());
                    return Map.copyOf(row);
                })
                .toList();
    }

    public com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPendenciaOperacionalResponse pendenciasOperacionais(int limit,
                                                                                                                           String rito,
                                                                                                                           String vara,
                                                                                                                           Boolean somentePendentes) {
        return portfolioProcessualService.pendencias(limit, rito, vara, somentePendentes);
    }

    public com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoNomeadoResponse processosNomeados(int limit,
                                                                                                                  String rito,
                                                                                                                  String vara,
                                                                                                                  Boolean somentePendentes) {
        return portfolioProcessualService.processosNomeados(limit, rito, vara, somentePendentes);
    }

    public com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoAcessoResponse acessoProcessoNomeado(Long processoId) {
        return portfolioProcessualService.acessoProcessoNomeado(processoId);
    }

    public Map<String, Object> resumoWorkbenchOperacional() {
        return workbenchService.painelResumo();
    }

    public OficialJusticaDiligenciaQueueResponse filaDiligenciasViva(int limit,
                                                                     String rito,
                                                                     String vara,
                                                                     String pasta,
                                                                     String prioridade,
                                                                     Boolean somentePendentes) {
        return workbenchService.filaViva(limit, rito, vara, pasta, prioridade, somentePendentes);
    }

    public OficialJusticaProcessoWorkbenchResponse processoWorkbench(Long processoId) {
        return workbenchService.processoWorkbench(processoId);
    }

    public OficialJusticaAgendaOperacionalResponse agendaOperacional(int limit,
                                                                    String rito,
                                                                    String vara,
                                                                    String pasta,
                                                                    String prioridade,
                                                                    Boolean somentePendentes) {
        return agendaOperacionalService.agenda(limit, rito, vara, pasta, prioridade, somentePendentes);
    }

    public OficialJusticaBalcaoVirtualChatResponse balcaoVirtualSalas(int limit) {
        return balcaoVirtualService.salas(limit);
    }

    public OficialJusticaBalcaoVirtualChatResponse balcaoVirtualSalaProcesso(Long processoId, int previewLimit) {
        return balcaoVirtualService.salaProcesso(processoId, previewLimit);
    }

    public List<com.tcc.pjb.backend.model.dto.ChatMensagemResponse> balcaoVirtualHistorico(Long processoId, int limit) {
        return balcaoVirtualService.historico(processoId, limit);
    }

    public Map<String, Object> balcaoVirtualEnviar(Long processoId, OficialJusticaBalcaoVirtualMessageRequest request) {
        return balcaoVirtualService.enviar(processoId, request);
    }

    private boolean isMandado(WorkItem item) {
        return commons.titleContains(item, "MANDADO", "CITACAO", "INTIMACAO", "BUSCA", "PENHORA");
    }

    private WorkItem resolveMandado(String mandadoId) {
        try {
            Long id = Long.parseLong(mandadoId);
            return workItemRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("WorkItem", id));
        } catch (NumberFormatException ex) {
            throw new RecursoNaoEncontradoException("WorkItem", mandadoId);
        }
    }

    private Processo resolveProcesso(Long processoId) {
        if (processoId == null) {
            return null;
        }
        return processoRepository.findById(processoId).orElse(null);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private Processo resolveProcessoObrigatorio(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));
    }
}
