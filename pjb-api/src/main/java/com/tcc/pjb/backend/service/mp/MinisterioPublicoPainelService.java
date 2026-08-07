package com.tcc.pjb.backend.service.mp;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.MinisterioPublicoParecerRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import com.tcc.pjb.backend.service.criminal.InqueritoPolicialDigitalService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
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
public class MinisterioPublicoPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService;
    private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final PainelSharedExperienceService sharedExperienceService;
    private final PainelSignalReflectionService signalReflectionService;
    private final PainelNativeCollectionCompositionService collectionCompositionService;
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;
    private final InqueritoPolicialDigitalService inqueritoPolicialDigitalService;

    public MinisterioPublicoPainelService(PerfilDashboardContextFactory contextFactory,
                                          PainelServiceCommons commons,
                                          ProcessoRepository processoRepository,
                                          WorkItemRepository workItemRepository,
                                          RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService,
                                          InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
                                          InstitutionalActorRoutingService institutionalActorRoutingService,
                                          InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                          InstitutionalPanelBrandingService institutionalPanelBrandingService,
                                          PainelSharedExperienceService sharedExperienceService,
                                          PainelSignalReflectionService signalReflectionService,
                                          PainelNativeCollectionCompositionService collectionCompositionService,
                                          PainelActionSurfaceCompositionService actionSurfaceCompositionService,
                                          PainelExecutionSurfaceCompositionService executionSurfaceCompositionService,
                                          InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService,
                                          InqueritoPolicialDigitalService inqueritoPolicialDigitalService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.recursalPeticionamentoFacadeService = recursalPeticionamentoFacadeService;
        this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
        this.institutionalActorRoutingService = institutionalActorRoutingService;
        this.institutionalMultimediaWorkspaceService = institutionalMultimediaWorkspaceService;
        this.institutionalPanelBrandingService = institutionalPanelBrandingService;
        this.sharedExperienceService = sharedExperienceService;
        this.signalReflectionService = signalReflectionService;
        this.collectionCompositionService = collectionCompositionService;
        this.actionSurfaceCompositionService = actionSurfaceCompositionService;
        this.executionSurfaceCompositionService = executionSurfaceCompositionService;
        this.institutionalMaterialActionGuardService = institutionalMaterialActionGuardService;
        this.inqueritoPolicialDigitalService = inqueritoPolicialDigitalService;
    }

    public PerfilDashboardPayload.MinisterioPublicoPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 30);
        int manifestacoes = (int) inbox.stream().filter(this::isManifestacao).count();
        int recursos = (int) inbox.stream().filter(this::isRecurso).count();
        int prazos48h = (int) inbox.stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(Instant.now().plus(48, ChronoUnit.HOURS))).count();
        List<String> prioridadeAlta = inbox.stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).limit(8).map(commons::resumo).toList();
        List<String> inqueritos = inbox.stream().filter(this::isInquerito).limit(8).map(commons::resumo).toList();
        String etag = commons.etag("MP", usuario.getId(), manifestacoes, recursos, prazos48h, prioridadeAlta, inqueritos, ctx.behavioralAudit());
        Map<String, Object> panelBranding = institutionalPanelBrandingService.resolve("MINISTERIO_PUBLICO", "PAINEL_MINISTERIO_PUBLICO", usuario.getTipoUsuario());
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("MINISTERIO_PUBLICO");
        Map<String, Object> operationalSignals = signalReflectionService.deriveSignals("MINISTERIO_PUBLICO", sharedExperience, manifestacoes + recursos, prazos48h, "ATUACAO_FINALISTICA");
        Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("MINISTERIO_PUBLICO", operationalSignals);
        prioridadeAlta = collectionCompositionService.composeList("MINISTERIO_PUBLICO", "PROCESSOS_PRIORIDADE_ALTA", prioridadeAlta, operationalSignals, nativeComposition);
        inqueritos = collectionCompositionService.composeList("MINISTERIO_PUBLICO", "INQUERITOS_EM_ACOMPANHAMENTO", inqueritos, operationalSignals, nativeComposition);
        Map<String, Object> collectionComposition = collectionCompositionService.buildCollectionComposition("MINISTERIO_PUBLICO", operationalSignals, nativeComposition, Map.of(
                "processosPrioridadeAlta", prioridadeAlta,
                "inqueritosEmAcompanhamento", inqueritos
        ));
        Map<String, Object> actionSurface = actionSurfaceCompositionService.buildActionSurface("MINISTERIO_PUBLICO", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = executionSurfaceCompositionService.buildExecutionSurface("MINISTERIO_PUBLICO", operationalSignals, nativeComposition, collectionComposition, actionSurface);
        Map<String, Object> panelVisualIdentity = signalReflectionService.reflectInBlock("MINISTERIO_PUBLICO", "VISUAL_IDENTITY", castMap(panelBranding.get("panelVisualIdentity")), operationalSignals);
        panelVisualIdentity = collectionCompositionService.decorateBlock("MINISTERIO_PUBLICO", "VISUAL_IDENTITY", panelVisualIdentity, operationalSignals, nativeComposition);
        panelVisualIdentity = actionSurfaceCompositionService.decorateBlock("MINISTERIO_PUBLICO", "VISUAL_IDENTITY", panelVisualIdentity, actionSurface, nativeComposition);
        panelVisualIdentity = executionSurfaceCompositionService.decorateBlock("MINISTERIO_PUBLICO", "VISUAL_IDENTITY", panelVisualIdentity, executionSurface, nativeComposition);
        return new PerfilDashboardPayload.MinisterioPublicoPayload(
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
                usuario.getTipoUsuario().name(),
                manifestacoes,
                recursos,
                prazos48h,
                prioridadeAlta,
                inqueritos,
                true,
                castMap(panelBranding.get("institutionalBranding")),
                panelVisualIdentity,
                operationalSignals,
                nativeComposition,
                collectionComposition,
                actionSurface,
                executionSurface,
                sharedExperience
        );
    }

    public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
        return institutionalActorTopologyMeshService.snapshot(processoId);
    }

    public List<Map<String, Object>> listarManifestacoesPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(this::isManifestacao).map(commons::mapWorkItem).toList();
    }

    @Transactional
    public Map<String, Object> interporRecurso(Long processoId,
                                               String tipoRecurso,
                                               String razoes,
                                               String fundamentacao,
                                               boolean pedidoEfeitoSuspensivo,
                                               boolean preparoDispensado,
                                               String observacoes) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_RECURSO);
        return recursalPeticionamentoFacadeService.interporRecurso(
                processoId,
                tipoRecurso,
                razoes,
                fundamentacao,
                pedidoEfeitoSuspensivo,
                preparoDispensado,
                observacoes
        );
    }

    @Transactional
    public Map<String, Object> registrarManifestacao(Long processoId, Object request) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_MANIFESTACAO);
        Usuario usuario = contextFactory.build().usuario();
        commons.publishUserHistory(usuario, "MP", "MANIFESTACAO_REGISTRADA", "Manifestação registrada no painel do MP.", processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REGISTRADA");
        out.put("processoId", processoId);
        out.put("payload", String.valueOf(request));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "MINISTERIO_PUBLICO",
                        "MANIFESTACAO_MINISTERIAL",
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
    public Map<String, Object> registrarParecer(Long processoId, MinisterioPublicoParecerRequest request) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_PARECER);
        Usuario usuario = contextFactory.build().usuario();
        MinisterioPublicoParecerRequest safe = request == null
                ? new MinisterioPublicoParecerRequest("Parecer ministerial", "Fundamentação não informada", List.of(), List.of(), List.of(), List.of(), List.of(), Boolean.TRUE, Boolean.FALSE)
                : request;
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("PARECER_MP:" + processoId + ':' + usuario.getId() + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.PETICAO)
                .titulo("Parecer ministerial — " + processo.getNumeroProcesso())
                .descricao(safe.parecer())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(safe.fundamentacao())
                .dueAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        item = workItemRepository.save(item);
        commons.publishUserHistory(usuario, "MP", "PARECER_REGISTRADO", "Parecer registrado no painel do MP.", processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PARECER_REGISTRADO");
        out.put("processoId", processoId);
        out.put("workItemId", item.getId());
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "MINISTERIO_PUBLICO",
                        "PARECER_MINISTERIAL",
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

    public List<Map<String, Object>> listarInqueritosEmAcompanhamento() {
        List<Map<String, Object>> inqueritosDigitais = inqueritoPolicialDigitalService.listarMeus(null).stream()
                .map(this::mapInqueritoDigital)
                .toList();
        java.util.Set<Long> processosComInqueritoDigital = inqueritosDigitais.stream()
                .map(item -> (Long) item.get("processoId"))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> itensPainel = commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(this::isInquerito)
                .filter(item -> item.getProcesso() == null || !processosComInqueritoDigital.contains(item.getProcesso().getId()))
                .map(commons::mapWorkItem)
                .map(this::marcarOrigemPainel)
                .toList();
        List<Map<String, Object>> combinados = new java.util.ArrayList<>(inqueritosDigitais);
        combinados.addAll(itensPainel);
        return List.copyOf(combinados);
    }

    private Map<String, Object> mapInqueritoDigital(InqueritoPolicialDigitalService.InqueritoView inquerito) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", inquerito.id());
        map.put("titulo", inquerito.numeroProcedimento() + " — " + inquerito.tipo());
        map.put("descricao", inquerito.resumoFatos());
        map.put("processoId", inquerito.processoVinculadoId());
        map.put("processoNumero", inquerito.processoVinculadoNumero());
        map.put("dueAt", inquerito.prazoConclusao());
        map.put("prioridade", null);
        map.put("status", inquerito.status());
        map.put("queueCode", null);
        map.put("origem", "INQUERITO_DIGITAL");
        return map;
    }

    private Map<String, Object> marcarOrigemPainel(Map<String, Object> item) {
        LinkedHashMap<String, Object> marcado = new LinkedHashMap<>(item);
        marcado.put("origem", "PAINEL_OPERACIONAL");
        return marcado;
    }

    @Transactional
    public Map<String, Object> requisitarDiligencia(Long processoId, Object request) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA);
        Usuario usuario = contextFactory.build().usuario();
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.policeDiligence(processoId);
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("DELEGACIA_DILIGENCIA:" + processoId + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.DILIGENCIA)
                .titulo("Cumprir diligência requisitada pelo Ministério Público")
                .descricao(String.valueOf(request))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .dueAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal("Requisição de diligência do Ministério Público")
                .build();
        item = workItemRepository.save(item);
        commons.publishTerritoryHistory(usuario, "DELEGADO", "MP_REQUISITOU_DILIGENCIA", "Nova diligência recebida do MP.", processo, item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REQUISITADA");
        out.put("workItemId", item.getId());
        out.put("dueAt", item.getDueAt());
        out.put("encaminhadoPara", route.inboxKey());
        out.put("routeAxis", route.routeAxis());
        return out;
    }

    public List<Map<String, Object>> listarPrazosDentroDe48h() {
        Instant limit = Instant.now().plus(48, ChronoUnit.HOURS);
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(limit))
                .map(commons::mapWorkItem)
                .toList();
    }

    private boolean isManifestacao(WorkItem item) {
        return commons.titleContains(item, "MANIFESTACAO", "PARECER", "PROMOCAO", "COTA", "DENUNCIA");
    }

    private boolean isRecurso(WorkItem item) {
        return commons.titleContains(item, "RECURSO", "APELACAO", "AGRAVO", "EMBARGOS");
    }

    private boolean isInquerito(WorkItem item) {
        return commons.titleContains(item, "INQUERITO", "INVESTIGACAO", "PIC", "PROCEDIMENTO INVESTIGATORIO");
    }


    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
