package com.tcc.pjb.backend.service.delegado;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoInqueritoMultimidiaRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.criminal.InqueritoMultimidiaWorkspaceService;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeExecutionService;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeToolbeltService;
import com.tcc.pjb.backend.service.criminal.PoliceInvestigationSystemLandscapeService;
import com.tcc.pjb.backend.service.criminal.PoliceSovereignOperationalWorkbenchService;
import com.tcc.pjb.backend.service.criminal.PoliceTraceableExecutionLedgerService;
import com.tcc.pjb.backend.service.criminal.PoliceTransactionalAdapterMeshService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelegadoPainelService {

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
    private final InqueritoMultimidiaWorkspaceService inqueritoMultimidiaWorkspaceService;
    private final PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService;
    private final PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService;
    private final PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService;
    private final PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService;
    private final PjbPoliceNativeExecutionService pjbPoliceNativeExecutionService;
    private final PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private final PainelSharedExperienceService sharedExperienceService;
    private final PainelSignalReflectionService signalReflectionService;
    private final PainelNativeCollectionCompositionService collectionCompositionService;
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;

    public DelegadoPainelService(PerfilDashboardContextFactory contextFactory,
                                 PainelServiceCommons commons,
                                 ProcessoRepository processoRepository,
                                 WorkItemRepository workItemRepository,
                                 PjbAuthorizationService authorizationService,
                                 PerfilCapabilityMatrixService capabilityMatrixService,
                                 PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService,
                                 InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
                                 InstitutionalActorRoutingService institutionalActorRoutingService,
                                 InstitutionalPanelBrandingService institutionalPanelBrandingService,
                                 InqueritoMultimidiaWorkspaceService inqueritoMultimidiaWorkspaceService,
                                 PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService,
                                 PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService,
                                 PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService,
                                 PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService,
                                 PjbPoliceNativeExecutionService pjbPoliceNativeExecutionService,
                                 PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService,
                                 QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService,
                                 PainelSharedExperienceService sharedExperienceService,
                                 PainelSignalReflectionService signalReflectionService,
                                 PainelNativeCollectionCompositionService collectionCompositionService,
                                 PainelActionSurfaceCompositionService actionSurfaceCompositionService,
                                 PainelExecutionSurfaceCompositionService executionSurfaceCompositionService,
                                 InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.capabilityMatrixService = capabilityMatrixService;
        this.intelligenceSummaryService = intelligenceSummaryService;
        this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
        this.institutionalActorRoutingService = institutionalActorRoutingService;
        this.institutionalPanelBrandingService = institutionalPanelBrandingService;
        this.inqueritoMultimidiaWorkspaceService = inqueritoMultimidiaWorkspaceService;
        this.policeInvestigationSystemLandscapeService = policeInvestigationSystemLandscapeService;
        this.pjbPoliceNativeToolbeltService = pjbPoliceNativeToolbeltService;
        this.policeTransactionalAdapterMeshService = policeTransactionalAdapterMeshService;
        this.policeSovereignOperationalWorkbenchService = policeSovereignOperationalWorkbenchService;
        this.pjbPoliceNativeExecutionService = pjbPoliceNativeExecutionService;
        this.policeTraceableExecutionLedgerService = policeTraceableExecutionLedgerService;
        this.sharedExperienceService = sharedExperienceService;
        this.signalReflectionService = signalReflectionService;
        this.collectionCompositionService = collectionCompositionService;
        this.actionSurfaceCompositionService = actionSurfaceCompositionService;
        this.executionSurfaceCompositionService = executionSurfaceCompositionService;
        this.qualifiedDocumentSignatureEnvelopeService = qualifiedDocumentSignatureEnvelopeService;
        this.institutionalMaterialActionGuardService = institutionalMaterialActionGuardService;
    }

    public PerfilDashboardPayload.DelegadoPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 30);
        int inqueritos = (int) inbox.stream().filter(this::isInquerito).count();
        int tcos = (int) inbox.stream().filter(this::isTco).count();
        int mandados = (int) inbox.stream().filter(this::isMandado).count();
        List<String> bos = inbox.stream().filter(this::isInquerito).limit(8).map(commons::resumo).toList();
        List<String> alertas = listarAlertasCrime();
        PerfilDashboardPayload.LocalizadorGovernadoResumo localizadorGovernado = new PerfilDashboardPayload.LocalizadorGovernadoResumo(
                authorizationService.canLocatePessoaByCpf(usuario),
                intelligenceSummaryService.resumir(usuario, PessoaLocalizacaoService.CanalConsulta.DELEGADO, 8)
        );
        String etag = commons.etag("DELEGADO", usuario.getId(), inqueritos, tcos, mandados, bos, alertas, ctx.behavioralAudit(), localizadorGovernado.metricas());
        String actorLane = usuario.getTipoUsuario().name().contains("FEDERAL") ? "POLICIA_FEDERAL" : "POLICIA_CIVIL";
        Map<String, Object> panelBranding = institutionalPanelBrandingService.resolve(actorLane, "PAINEL_INQUERITO_MULTIMIDIA", usuario.getTipoUsuario());
        Map<String, Object> policeSystemLandscape = policeInvestigationSystemLandscapeService.landscapeFor(usuario.getTipoUsuario());
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("DELEGADO");
        Map<String, Object> operationalSignals = signalReflectionService.deriveSignals("DELEGADO", sharedExperience, inqueritos + tcos + mandados, (int) ctx.prazoRadar().stream().limit(3).count(), "TRIAGEM_INVESTIGATIVA");
        Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("DELEGADO", operationalSignals);
        bos = collectionCompositionService.composeList("DELEGADO", "BOLETINS_OCORRENCIA_RECENTES", bos, operationalSignals, nativeComposition);
        alertas = collectionCompositionService.composeList("DELEGADO", "ALERTAS_CRIME", alertas, operationalSignals, nativeComposition);
        Map<String, Object> collectionComposition = collectionCompositionService.buildCollectionComposition("DELEGADO", operationalSignals, nativeComposition, Map.of(
                "boletinsOcorrenciaRecentes", bos,
                "alertasCrime", alertas
        ));
        Map<String, Object> actionSurface = actionSurfaceCompositionService.buildActionSurface("DELEGADO", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = executionSurfaceCompositionService.buildExecutionSurface("DELEGADO", operationalSignals, nativeComposition, collectionComposition, actionSurface);
        Map<String, Object> investigativeWorkstation = policeSovereignOperationalWorkbenchService.compose(usuario.getTipoUsuario());
        investigativeWorkstation = new LinkedHashMap<>(investigativeWorkstation);
        investigativeWorkstation.put("nativeToolbelt", pjbPoliceNativeToolbeltService.nativeWorkbench(usuario.getTipoUsuario()));
        investigativeWorkstation.put("transactionalAdapterMesh", policeTransactionalAdapterMeshService.sovereignMesh(usuario.getTipoUsuario()));
        investigativeWorkstation.put("nativeExecutionWorkbench", pjbPoliceNativeExecutionService.nativeExecutionWorkbench(usuario.getTipoUsuario()));
        investigativeWorkstation.put("traceableOperationalLedger", policeTraceableExecutionLedgerService.operationalLedgerBlueprint(usuario.getTipoUsuario()));
        investigativeWorkstation.put("recentTraceableExecutions", policeTraceableExecutionLedgerService.recentExecutions(usuario.getTipoUsuario(), 8));
        investigativeWorkstation = signalReflectionService.reflectInBlock("DELEGADO", "WORKBENCH", investigativeWorkstation, operationalSignals);
        investigativeWorkstation = collectionCompositionService.decorateBlock("DELEGADO", "WORKBENCH", investigativeWorkstation, operationalSignals, nativeComposition);
        investigativeWorkstation = actionSurfaceCompositionService.decorateBlock("DELEGADO", "WORKBENCH", investigativeWorkstation, actionSurface, nativeComposition);
        investigativeWorkstation = executionSurfaceCompositionService.decorateBlock("DELEGADO", "WORKBENCH", investigativeWorkstation, executionSurface, nativeComposition);
        policeSystemLandscape = signalReflectionService.reflectInBlock("DELEGADO", "LANDSCAPE", policeSystemLandscape, operationalSignals);
        policeSystemLandscape = collectionCompositionService.decorateBlock("DELEGADO", "LANDSCAPE", policeSystemLandscape, operationalSignals, nativeComposition);
        policeSystemLandscape = actionSurfaceCompositionService.decorateBlock("DELEGADO", "LANDSCAPE", policeSystemLandscape, actionSurface, nativeComposition);
        policeSystemLandscape = executionSurfaceCompositionService.decorateBlock("DELEGADO", "LANDSCAPE", policeSystemLandscape, executionSurface, nativeComposition);
        Map<String, Object> panelVisualIdentity = signalReflectionService.reflectInBlock("DELEGADO", "VISUAL_IDENTITY", castMap(panelBranding.get("panelVisualIdentity")), operationalSignals);
        panelVisualIdentity = actionSurfaceCompositionService.decorateBlock("DELEGADO", "VISUAL_IDENTITY", panelVisualIdentity, actionSurface, nativeComposition);
        panelVisualIdentity = executionSurfaceCompositionService.decorateBlock("DELEGADO", "VISUAL_IDENTITY", panelVisualIdentity, executionSurface, nativeComposition);
        return new PerfilDashboardPayload.DelegadoPayload(
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
                usuario.getComarca(),
                usuario.getTipoUsuario().name().contains("FEDERAL") ? "FEDERAL" : "ESTADUAL",
                inqueritos,
                tcos,
                mandados,
                bos,
                alertas,
                true,
                authorizationService.canLocatePessoaByCpf(usuario),
                capabilityMatrixService.capacidadesDelegado(usuario),
                localizadorGovernado,
                castMap(panelBranding.get("institutionalBranding")),
                panelVisualIdentity,
                policeSystemLandscape,
                investigativeWorkstation,
                operationalSignals,
                nativeComposition,
                collectionComposition,
                actionSurface,
                executionSurface,
                sharedExperience
        );
    }

    public List<Map<String, Object>> listarInqueritosPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(this::isInquerito).map(commons::mapWorkItem).toList();
    }

    public List<Map<String, Object>> listarMandadosPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(this::isMandado).map(commons::mapWorkItem).toList();
    }

    public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
        return institutionalActorTopologyMeshService.snapshot(processoId);
    }

    public Map<String, Object> solicitarAcessoProcesso(Long processoId) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario usuario = contextFactory.build().usuario();
        commons.publishTerritoryHistory(usuario, "DELEGADO", "ACESSO_PROCESSO_SOLICITADO", "Solicitação de acesso a processo registrada.", processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SOLICITADO");
        out.put("processoId", processoId);
        out.put("processoNumero", processo.getNumeroProcesso());
        out.put("scope", usuario.getUf() + "/" + usuario.getComarca());
        return out;
    }

    @Transactional
    public Map<String, Object> registrarDiligencia(Object request) {
        Usuario usuario = contextFactory.build().usuario();
        String resumo = String.valueOf(request);
        Processo processo = resolveProcessoFromRequest(request);
        if (processo != null) {
            institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.DELEGADO_DILIGENCIA);
        }
        else {
            institutionalMaterialActionGuardService.requireAllowedForCatalogAction(
                    InstitutionalMaterialActionGuardService.MaterialAction.DELEGADO_DILIGENCIA,
                    new InstitutionalMaterialActionGuardService.CatalogActionContext(
                            InstitutionalMaterialActionGuardService.TargetSphere.INDETERMINADA,
                            null,
                            RamoDireito.PENAL,
                            null,
                            "DILIGENCIA_INVESTIGATIVA",
                            resumo,
                            true
                    )
            );
        }
        InstitutionalActorRoutingService.InstitutionalRoute route = processo != null
                ? institutionalActorRoutingService.ministerioPublico(processo.getId(), "DILIGENCIA_REQUISITADA")
                : null;
        WorkItem workItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo != null ? processo.getFaseAtual() : null)
                .templateCode("MP_DILIGENCIA:" + Instant.now().toEpochMilli())
                .type(WorkItemType.DILIGENCIA)
                .titulo("Analisar diligência requisitada pelo delegado")
                .descricao(resumo)
                .queueCode(route == null ? "MP_DILIGENCIA" : route.queueCode())
                .inboxKey(route == null ? "MP_DILIGENCIA" : route.inboxKey())
                .assignedRole(route == null ? com.tcc.pjb.backend.model.entity.enums.TipoUsuario.MEMBRO_MINISTERIO_PUBLICO : route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .blocking(false)
                .dueAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal("CPP e diligências investigativas")
                .build();
        workItem = workItemRepository.save(workItem);
        commons.publishTerritoryHistory(usuario, "MP", "DELEGADO_REQUISITOU_DILIGENCIA", "Nova diligência encaminhada ao MP.", workItem.getProcesso(), workItem.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CRIADO");
        out.put("workItemId", workItem.getId());
        out.put("assignedRole", workItem.getAssignedRole());
        out.put("dueAt", workItem.getDueAt());
        out.put("encaminhadoPara", workItem.getInboxKey());
        return out;
    }

    @Transactional
    public Map<String, Object> registrarPecaInquerito(Long inqueritoId, DelegadoInqueritoMultimidiaRequest request) {
        institutionalMaterialActionGuardService.requireAllowedForCatalogAction(
                InstitutionalMaterialActionGuardService.MaterialAction.DELEGADO_PECA_INQUERITO,
                new InstitutionalMaterialActionGuardService.CatalogActionContext(
                        InstitutionalMaterialActionGuardService.TargetSphere.INDETERMINADA,
                        null,
                        RamoDireito.PENAL,
                        null,
                        "PECA_INVESTIGATIVA",
                        request == null ? null : request.narrativa(),
                        true
                )
        );
        Usuario usuario = contextFactory.build().usuario();
        DelegadoInqueritoMultimidiaRequest safe = request == null
                ? new DelegadoInqueritoMultimidiaRequest("RELATORIO_INQUERITO", "Narrativa investigativa não informada", null, null, null, null, null, null, Boolean.FALSE, Boolean.TRUE)
                : request;
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PECA_INVESTIGATIVA_REGISTRADA");
        out.put("inqueritoId", inqueritoId);
        out.put("tipoPeca", safe.tipoPeca());
        out.put("fundamentoOperacional", safe.fundamentoOperacional());
        out.putAll(inqueritoMultimidiaWorkspaceService.compose(inqueritoId, usuario.getTipoUsuario(), safe));
        out.put("documentoFormalAssinado", buildSignedInvestigativePiece(usuario, inqueritoId, safe));
        return out;
    }

    private Map<String, Object> buildSignedInvestigativePiece(Usuario usuario,
                                                              Long inqueritoId,
                                                              DelegadoInqueritoMultimidiaRequest request) {
        String titulo = resolveInvestigativePieceTitle(request);
        SignedDocumentEnvelope signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null,
                usuario,
                titulo,
                investigativePieceCanonicalText(inqueritoId, request),
                usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "PERFIL_NAO_IDENTIFICADO",
                "PECA_INVESTIGATIVA_QUALIFICADA_SOBERANA",
                request.sigiloSensivelResolvido(),
                List.of(
                        "peca_investigativa_assinada",
                        "assinatura_transversal_completa",
                        normalizeToken(request.tipoPeca()),
                        request.sigiloSensivelResolvido() ? "sigilo_sensivel" : "sigilo_ordinario"
                )
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("tituloDocumento", titulo);
        out.put("conteudoAssinado", signedContent.renderedContent());
        out.put("hashSha256", signedContent.contentHash());
        out.put("assinaturaQualificada", signedContent.assinaturaQualificada());
        out.put("validacaoSoberana", signedContent.validacaoSoberana());
        out.put("selado", Boolean.TRUE);
        return Collections.unmodifiableMap(out);
    }

    private String resolveInvestigativePieceTitle(DelegadoInqueritoMultimidiaRequest request) {
        String tipo = normalizeToken(request.tipoPeca());
        return switch (tipo) {
            case "REPRESENTACAO_POLICIAL" -> "Representação policial formal";
            case "CERTIDAO_CARTORIO_POLICIAL" -> "Certidão cartorária policial formal";
            default -> "Relatório formal de inquérito";
        };
    }

    private String investigativePieceCanonicalText(Long inqueritoId,
                                                   DelegadoInqueritoMultimidiaRequest request) {
        return String.join("\n",
                resolveInvestigativePieceTitle(request),
                "inquerito_id=" + String.valueOf(inqueritoId),
                "tipo_peca=" + normalizeToken(request.tipoPeca()),
                "fundamento_operacional=" + normalizeFreeText(request.fundamentoOperacional()),
                "sigilo_sensivel=" + request.sigiloSensivelResolvido(),
                "preparar_pacote_protocolo=" + request.prepararPacoteProtocoloResolvido(),
                "midia_inline_total=" + (request.midiaInline() == null ? 0 : request.midiaInline().size()),
                "provas_documentais_total=" + (request.provasDocumentais() == null ? 0 : request.provasDocumentais().size()),
                "documentos_pessoais_total=" + (request.documentosPessoais() == null ? 0 : request.documentosPessoais().size()),
                "documentos_representacao_total=" + (request.documentosRepresentacao() == null ? 0 : request.documentosRepresentacao().size()),
                "documentos_anexados_total=" + (request.documentosAnexados() == null ? 0 : request.documentosAnexados().size()),
                "narrativa=" + normalizeFreeText(request.narrativa())
        );
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "NAO_INFORMADO";
        }
        return value.trim().replace(' ', '_').toUpperCase(java.util.Locale.ROOT);
    }

    private static String normalizeFreeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim().replace('\r', ' ').replace('\n', ' ');
    }

    public List<String> listarAlertasCrime() {
        Usuario usuario = contextFactory.build().usuario();
        return List.of(
                "Mandados ativos em monitoramento na circunscrição " + usuario.getComarca(),
                "Verificação RENAJUD recomendada antes de apreensão veicular.",
                "BNMP consultado na inicialização do painel.");
    }

    private boolean isInquerito(WorkItem item) {
        return commons.titleContains(item, "INQUERITO", "INVESTIG", "PORTARIA");
    }

    private boolean isTco(WorkItem item) {
        return commons.titleContains(item, "TCO", "TERMO CIRCUNSTANCIADO");
    }

    private boolean isMandado(WorkItem item) {
        return commons.titleContains(item, "MANDADO", "PRISAO", "BUSCA", "APREENSAO");
    }

    private Processo resolveProcessoFromRequest(Object request) {
        if (request instanceof Map<?, ?> map) {
            Object v = map.get("processoId");
            if (v instanceof Number n) {
                return processoRepository.findById(n.longValue()).orElse(null);
            }
            if (v != null) {
                try {
                    return processoRepository.findById(Long.parseLong(String.valueOf(v))).orElse(null);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
