package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
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
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import com.tcc.pjb.backend.service.painel.shared.PainelCompositionPipelineService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final PjbAuthorizationService authorizationService;
    private final PerfilCapabilityMatrixService capabilityMatrixService;
    private final PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService;
    private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService;
    private final OficialJusticaOficioDispatchService oficioDispatchService;
    private final OficialJusticaEnderecoTriageService enderecoTriageService;
    private final OficialJusticaPortfolioProcessualService portfolioProcessualService;
    private final OficialJusticaWorkbenchService workbenchService;
    private final OficialJusticaAgendaOperacionalService agendaOperacionalService;
    private final OficialJusticaCalendarioOperacionalService calendarioOperacionalService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final OficialJusticaBalcaoVirtualService balcaoVirtualService;
    private final OficialJusticaNotificationCenterService notificationCenterService;
    private final OficialJusticaPanelEgressService panelEgressService;
    private final CalendarInstitutionalBridgeService institutionalBridgeService;
    private final PainelSharedExperienceService sharedExperienceService;
    private final PainelCompositionPipelineService compositionPipeline;
    private final OficialJusticaDesfechoDiligenciaService desfechoDiligenciaService;

    public OficialJusticaPainelService(PerfilDashboardContextFactory contextFactory,
                                       PainelServiceCommons commons,
                                       PjbAuthorizationService authorizationService,
                                       PerfilCapabilityMatrixService capabilityMatrixService,
                                       PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService,
                                       InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
                                       InstitutionalPanelBrandingService institutionalPanelBrandingService,
                                       OficialJusticaOficioDispatchService oficioDispatchService,
                                       OficialJusticaEnderecoTriageService enderecoTriageService,
                                       OficialJusticaPortfolioProcessualService portfolioProcessualService,
                                       OficialJusticaWorkbenchService workbenchService,
                                       OficialJusticaAgendaOperacionalService agendaOperacionalService,
                                       OficialJusticaCalendarioOperacionalService calendarioOperacionalService,
                                       OficialJusticaContextEnvelopeService contextEnvelopeService,
                                       OficialJusticaBalcaoVirtualService balcaoVirtualService,
                                       OficialJusticaNotificationCenterService notificationCenterService,
                                       OficialJusticaPanelEgressService panelEgressService,
                                       CalendarInstitutionalBridgeService institutionalBridgeService,
                                       PainelSharedExperienceService sharedExperienceService,
                                       PainelCompositionPipelineService compositionPipeline,
                                       OficialJusticaDesfechoDiligenciaService desfechoDiligenciaService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.authorizationService = authorizationService;
        this.capabilityMatrixService = capabilityMatrixService;
        this.intelligenceSummaryService = intelligenceSummaryService;
        this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
        this.institutionalPanelBrandingService = institutionalPanelBrandingService;
        this.oficioDispatchService = oficioDispatchService;
        this.enderecoTriageService = enderecoTriageService;
        this.portfolioProcessualService = portfolioProcessualService;
        this.workbenchService = workbenchService;
        this.agendaOperacionalService = agendaOperacionalService;
        this.calendarioOperacionalService = calendarioOperacionalService;
        this.contextEnvelopeService = contextEnvelopeService;
        this.balcaoVirtualService = balcaoVirtualService;
        this.notificationCenterService = notificationCenterService;
        this.panelEgressService = panelEgressService;
        this.institutionalBridgeService = institutionalBridgeService;
        this.sharedExperienceService = sharedExperienceService;
        this.compositionPipeline = compositionPipeline;
        this.desfechoDiligenciaService = desfechoDiligenciaService;
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
        Map<String, Object> operationalSignals = compositionPipeline.deriveSignals("OFICIAL_JUSTICA", sharedExperience, pendentes, ctx.prazoRadar().size(), "CUMPRIMENTO_EXTERNO");
        Map<String, Object> nativeComposition = compositionPipeline.buildNativeComposition("OFICIAL_JUSTICA", operationalSignals);
        proximos = compositionPipeline.composeList("OFICIAL_JUSTICA", "PROXIMOS_MANDADOS", proximos, operationalSignals, nativeComposition);
        penhoras = compositionPipeline.composeList("OFICIAL_JUSTICA", "PENHORAS_AGENDADAS", penhoras, operationalSignals, nativeComposition);
        Map<String, Object> collectionComposition = compositionPipeline.buildCollectionComposition("OFICIAL_JUSTICA", operationalSignals, nativeComposition, Map.of(
                "proximosMandados", proximos,
                "penhorasAgendadas", penhoras
        ));
        Map<String, Object> actionSurface = compositionPipeline.buildActionSurface("OFICIAL_JUSTICA", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = compositionPipeline.buildExecutionSurface("OFICIAL_JUSTICA", operationalSignals, nativeComposition, collectionComposition, actionSurface);
        CalendarInstitutionalBridgeResponse institutionalBridge = institutionalBridgeService.bridgeForUser(usuario, java.time.LocalDate.now(java.time.ZoneOffset.UTC), java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(14), null);
        var institutionalFocus = institutionalBridgeService.focus(institutionalBridge);
        LinkedHashMap<String, Object> calendarioOperacionalMutable = new LinkedHashMap<>(calendarioOperacionalService.calendario(java.time.YearMonth.now(java.time.ZoneOffset.UTC)).toPanelMap());
        calendarioOperacionalMutable.put("institutionalBridge", institutionalBridgeService.toPanelMap(institutionalBridge));
        calendarioOperacionalMutable.put("institutionalFocus", institutionalBridgeService.toFocusPanelMap(institutionalFocus));
        Map<String, Object> calendarioOperacional = compositionPipeline.decorate("OFICIAL_JUSTICA", "CALENDARIO", calendarioOperacionalMutable, operationalSignals, nativeComposition, actionSurface, executionSurface);
        Map<String, Object> balcaoVirtual = compositionPipeline.decorate("OFICIAL_JUSTICA", "OPERACIONAL", balcaoVirtualService.painelResumo(), operationalSignals, nativeComposition, actionSurface, executionSurface);
        Map<String, Object> notificationCenter = compositionPipeline.decorate("OFICIAL_JUSTICA", "PENDENCIAS", notificationCenterService.painelResumo(), operationalSignals, nativeComposition, actionSurface, executionSurface);
        organizacaoOperacional = new LinkedHashMap<>(compositionPipeline.decorate("OFICIAL_JUSTICA", "OPERACIONAL", organizacaoOperacional, operationalSignals, nativeComposition, actionSurface, executionSurface));
        pendenciasOperacionais = compositionPipeline.decorate("OFICIAL_JUSTICA", "PENDENCIAS", pendenciasOperacionais, operationalSignals, nativeComposition, actionSurface, executionSurface);
        rastreioOperacional = compositionPipeline.decorate("OFICIAL_JUSTICA", "OPERACIONAL", rastreioOperacional, operationalSignals, nativeComposition, actionSurface, executionSurface);
        operationalWorkbench = compositionPipeline.decorate("OFICIAL_JUSTICA", "WORKBENCH", operationalWorkbench, operationalSignals, nativeComposition, actionSurface, executionSurface);
        agendaOperacional = compositionPipeline.decorate("OFICIAL_JUSTICA", "AGENDA", agendaOperacional, operationalSignals, nativeComposition, actionSurface, executionSurface);
        Map<String, Object> panelVisualIdentity = compositionPipeline.decorateWithoutCollection("OFICIAL_JUSTICA", "VISUAL_IDENTITY", castMap(panelBranding.get("panelVisualIdentity")), operationalSignals, nativeComposition, actionSurface, executionSurface);
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
        return oficioDispatchService.catalogo();
    }

    public Map<String, Object> listarExecucoesOficios(int limit) {
        return oficioDispatchService.listarExecucoes(limit);
    }

    public Map<String, Object> statusExecucaoOficio(String executionId) {
        return oficioDispatchService.statusExecucao(executionId);
    }

    public Map<String, Object> confirmarEntregaOficio(String executionId, OficialJusticaOficioConfirmationRequest request) {
        return oficioDispatchService.confirmarEntrega(executionId, request);
    }

    public Map<String, Object> confirmarCanalOficio(String executionId, OficialJusticaOficioChannelAckRequest request) {
        return oficioDispatchService.confirmarCanal(executionId, request);
    }

    public Map<String, Object> ackCartorioOficio(String executionId, OficialJusticaOficioCartorioAckRequest request) {
        return oficioDispatchService.ackCartorio(executionId, request);
    }

    public Map<String, Object> reconciliarOficio(String executionId, OficialJusticaOficioReconciliationRequest request) {
        return oficioDispatchService.reconciliar(executionId, request);
    }

    public Map<String, Object> malhaExternaOficio(String executionId) {
        return oficioDispatchService.malhaExterna(executionId);
    }

    public Map<String, Object> retentarEntregaOficio(String executionId, OficialJusticaOficioRetryRequest request) {
        return oficioDispatchService.retentar(executionId, request);
    }

    public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
        authorizationService.requireVinculoInstitucionalComProcesso(processoId);
        return institutionalActorTopologyMeshService.snapshot(processoId);
    }

    public Map<String, Object> registrarCumprimento(String mandadoId, Object request) {
        return desfechoDiligenciaService.registrarCumprimento(mandadoId, request);
    }

    public Map<String, Object> registrarFrustracao(String mandadoId, Object request) {
        return desfechoDiligenciaService.registrarFrustracao(mandadoId, request);
    }

    public Map<String, Object> registrarAvaliacao(Long processoId, Object request) {
        return desfechoDiligenciaService.registrarAvaliacao(processoId, request);
    }

    public Map<String, Object> emitirOficio(Long processoId, OficialJusticaOficioRequest request) {
        return oficioDispatchService.emitir(processoId, request);
    }

    public Map<String, Object> responderOficio(Long processoId, OficialJusticaOficioRequest request) {
        return oficioDispatchService.responder(processoId, request);
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
}
