package com.tcc.pjb.backend.service.desembargador;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
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
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.model.dto.processual.observability.business.ProcessBusinessObservabilityResponse;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.processual.observability.business.ProcessBusinessObservabilityService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;

@Service
public class DesembargadorColegialdoPainelService {

    private static final EnumSet<TipoUsuario> COLEGIADO_ROLES = EnumSet.of(TipoUsuario.DESEMBARGADOR, TipoUsuario.DESEMBARGADOR_FEDERAL);

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final ProcessBusinessObservabilityService processBusinessObservabilityService;
    private final DecisionSafetyService decisionSafetyService;
    private final CaseContinuityDecisionGateService caseContinuityDecisionGateService;
    private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;
    private final PainelSharedExperienceService sharedExperienceService;
    private final PainelSignalReflectionService signalReflectionService;
    private final PainelNativeCollectionCompositionService collectionCompositionService;
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;

    public DesembargadorColegialdoPainelService(PerfilDashboardContextFactory contextFactory,
                                                PainelServiceCommons commons,
                                                ProcessoRepository processoRepository,
                                                WorkItemRepository workItemRepository,
                                                PjbAuthorizationService authorizationService,
                                                ProcessoLifecycleMachine lifecycleMachine,
                                                ProcessBusinessObservabilityService processBusinessObservabilityService,
                                                DecisionSafetyService decisionSafetyService,
                                                CaseContinuityDecisionGateService caseContinuityDecisionGateService,
                                                InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
                                                InstitutionalActorRoutingService institutionalActorRoutingService,
                                                RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService,
                                                PainelSharedExperienceService sharedExperienceService,
                                                PainelSignalReflectionService signalReflectionService,
                                                PainelNativeCollectionCompositionService collectionCompositionService,
                                                PainelActionSurfaceCompositionService actionSurfaceCompositionService,
                                       PainelExecutionSurfaceCompositionService executionSurfaceCompositionService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.lifecycleMachine = lifecycleMachine;
        this.processBusinessObservabilityService = processBusinessObservabilityService;
        this.decisionSafetyService = decisionSafetyService;
        this.caseContinuityDecisionGateService = caseContinuityDecisionGateService;
        this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
        this.institutionalActorRoutingService = institutionalActorRoutingService;
        this.recursalQualifiedDocumentMaterializerService = recursalQualifiedDocumentMaterializerService;
        this.sharedExperienceService = sharedExperienceService;
        this.signalReflectionService = signalReflectionService;
        this.collectionCompositionService = collectionCompositionService;
        this.actionSurfaceCompositionService = actionSurfaceCompositionService;
        this.executionSurfaceCompositionService = executionSurfaceCompositionService;
    }

    public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
        return institutionalActorTopologyMeshService.snapshot(processoId);
    }

    @Cacheable(cacheNames = "desembargador_painel", key = "@currentUserService.currentUserIdOrZero()", condition = "@cacheRuntime.redisEnabled()")
    public ColegialdoSnapshot bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = requireDesembargador(ctx.usuario());
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 60);
        List<String> recursosParaRelatar = inbox.stream().filter(this::isRelator).limit(20).map(commons::resumo).toList();
        List<String> votosParaProferir = inbox.stream().filter(this::isVotante).limit(20).map(commons::resumo).toList();
        List<String> acordaosParaAssinatura = inbox.stream().filter(this::isAcordaoPendente).limit(15).map(commons::resumo).toList();
        List<String> pedidosDeVista = inbox.stream().filter(this::isPedidoVista).limit(10).map(commons::resumo).toList();
        int sessoesProgramadas = (int) inbox.stream().filter(this::isSessao).count();
        int prazosUrgentes = (int) inbox.stream().filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(Instant.now().plus(48, ChronoUnit.HOURS))).count();
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("DESEMBARGADOR_COLEGIADO");
        Map<String, Object> operationalSignals = signalReflectionService.deriveSignals("DESEMBARGADOR_COLEGIADO", sharedExperience, recursosParaRelatar.size() + votosParaProferir.size() + acordaosParaAssinatura.size(), prazosUrgentes, "PAUTA_E_ACORDAO");
        Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("DESEMBARGADOR_COLEGIADO", operationalSignals);
        recursosParaRelatar = collectionCompositionService.composeList("DESEMBARGADOR_COLEGIADO", "RECURSOS_RELATORIA", recursosParaRelatar, operationalSignals, nativeComposition);
        votosParaProferir = collectionCompositionService.composeList("DESEMBARGADOR_COLEGIADO", "VOTOS_PROFERIR", votosParaProferir, operationalSignals, nativeComposition);
        acordaosParaAssinatura = collectionCompositionService.composeList("DESEMBARGADOR_COLEGIADO", "ACORDAOS_ASSINATURA", acordaosParaAssinatura, operationalSignals, nativeComposition);
        pedidosDeVista = collectionCompositionService.composeList("DESEMBARGADOR_COLEGIADO", "PEDIDOS_VISTA", pedidosDeVista, operationalSignals, nativeComposition);
        Map<String, Object> collectionComposition = collectionCompositionService.buildCollectionComposition("DESEMBARGADOR_COLEGIADO", operationalSignals, nativeComposition, Map.of(
                "recursosParaRelatar", recursosParaRelatar,
                "votosParaProferir", votosParaProferir,
                "acordaosParaAssinatura", acordaosParaAssinatura,
                "pedidosDeVista", pedidosDeVista
        ));
        Map<String, Object> actionSurface = actionSurfaceCompositionService.buildActionSurface("DESEMBARGADOR_COLEGIADO", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = executionSurfaceCompositionService.buildExecutionSurface("DESEMBARGADOR_COLEGIADO", operationalSignals, nativeComposition, collectionComposition, actionSurface);
        return new ColegialdoSnapshot(
                ctx.generatedAt(),
                ctx.perfilAtivo(),
                ctx.tratamento(),
                resolveTribunal(usuario),
                resolveOrgaoJulgador(usuario),
                recursosParaRelatar,
                votosParaProferir,
                acordaosParaAssinatura,
                pedidosDeVista,
                sessoesProgramadas,
                prazosUrgentes,
                ctx.prazoRadar(),
                ctx.sessionRisk(),
                operationalSignals,
                nativeComposition,
                collectionComposition,
                actionSurface,
                executionSurface,
                sharedExperience
        );
    }

    @Cacheable(cacheNames = "desembargador_governanca", key = "@currentUserService.currentUserIdOrZero()", condition = "@cacheRuntime.redisEnabled()")
    public CamaraGestaoDashboard governancaCamara() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = requireDesembargador(ctx.usuario());
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 120);
        ProcessBusinessObservabilityResponse business = processBusinessObservabilityService.snapshot();
        long acervoTribunal = processoRepository.countByUf(usuario.getUf());
        long julgadosPendentes = inbox.stream().filter(this::isRelator).count();
        long votosPendentes = inbox.stream().filter(this::isVotante).count();
        long acordaosPendentes = inbox.stream().filter(this::isAcordaoPendente).count();
        long vistasAbertas = inbox.stream().filter(this::isPedidoVista).count();
        long sessoesAbertas = inbox.stream().filter(this::isSessao).count();
        long prazosCriticos = inbox.stream().filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(Instant.now().plus(72, ChronoUnit.HOURS))).count();
        double taxaVista = julgadosPendentes == 0 ? 0.0d : (vistasAbertas * 100.0d) / julgadosPendentes;
        double taxaBacklog = acervoTribunal == 0 ? 0.0d : (business.workItemsPendentes() * 100.0d) / acervoTribunal;
        List<String> alertas = new java.util.ArrayList<>();
        if (vistasAbertas > 0) {
            alertas.add("Há pedidos de vista abertos impactando a velocidade da câmara.");
        }
        if (prazosCriticos > 0) {
            alertas.add("Existem prazos críticos com janela inferior a 72 horas.");
        }
        if (business.comunicacoesFrustradas() > 0) {
            alertas.add("Há comunicações frustradas com potencial impacto na pauta colegiada.");
        }
        if (business.caseFilesAttentionRequired() > 0) {
            alertas.add("Existem casos unificados com atenção estrutural pendente na câmara colegiada.");
        }
        if (business.divergentRootProceedings() > 0 || business.orphanProceedingParents() > 0) {
            alertas.add("O organismo unificado da câmara possui vínculos estruturais pendentes de reconciliação.");
        }
        if (taxaBacklog > 20.0d) {
            alertas.add("Backlog operacional acima do patamar de conforto da câmara.");
        }
        return new CamaraGestaoDashboard(
                LocalDateTime.now(),
                resolveTribunal(usuario),
                resolveOrgaoJulgador(usuario),
                acervoTribunal,
                julgadosPendentes,
                votosPendentes,
                acordaosPendentes,
                vistasAbertas,
                sessoesAbertas,
                prazosCriticos,
                round(taxaVista),
                round(taxaBacklog),
                business.alertas(),
                List.copyOf(alertas)
        );
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> proferirVoto(Long processoId, String voto, String fundamentacao, String decisao) {
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentDesembargador();
        caseContinuityDecisionGateService.requireAllowed(processoId, ProcessoLifecycleAction.PROFERIR_VOTO);
        decisionSafetyService.requireSafeDecisionContext(processo, usuario, "VOTO", voto, fundamentacao);
        String dedupKey = deterministicKey("VOTO", processoId, usuario.getId());
        InstitutionalActorRoutingService.InstitutionalRoute relatoriaRoute = institutionalActorRoutingService.colegiado(processoId, "VOTO_RELATORIA");
        WorkItem votoItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.DECISAO)
                .titulo("Voto — " + processo.getNumeroProcesso() + " — " + decisao)
                .descricao(voto)
                .queueCode(relatoriaRoute.queueCode())
                .inboxKey(relatoriaRoute.inboxKey())
                .assignedRole(relatoriaRoute.assignedRole())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(votoItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.PROFERIR_VOTO);
        processoRepository.save(processo);
        commons.publishUserHistory(usuario, "DESEMBARGADOR", "VOTO_PROFERIDO", "Voto proferido no colegiado.", processo, processoId);
        WorkItem conferenciaItem = decisionSafetyService.registrarConferenciaCruzadaSeNecessario(processo, usuario, "VOTO", voto).orElse(null);
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarVotoColegiado(
                processoId,
                votoItem.getTitulo(),
                voto,
                fundamentacao,
                decisao,
                resolveOrgaoJulgador(usuario),
                "SEGUNDO_GRAU"
        );
        LinkedHashMap<String, Object> extras = new LinkedHashMap<>();
        extras.put("decisao", decisao);
        extras.put("encaminhadoPara", relatoriaRoute.inboxKey());
        extras.put("documentoFormalAssinado", documentoFormalAssinado);
        extras.put("assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"));
        extras.put("validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana"));
        if (conferenciaItem != null) {
            extras.put("conferenciaCruzadaWorkItemId", conferenciaItem.getId());
        }
        return response("VOTO_REGISTRADO", processoId, votoItem.getId(), extras);
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> lavrarAcordao(Long processoId, String ementa, String dispositivo, String fundamentacao) {
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentDesembargador();
        caseContinuityDecisionGateService.requireAllowed(processoId, ProcessoLifecycleAction.LAVRAR_ACORDAO);
        decisionSafetyService.requireSafeDecisionContext(processo, usuario, "ACORDAO", dispositivo, fundamentacao);
        String dedupKey = deterministicKey("ACORDAO", processoId, usuario.getId());
        InstitutionalActorRoutingService.InstitutionalRoute publicationRoute = institutionalActorRoutingService.colegiadoPublication(processoId, "ACORDAO_PUBLICACAO");
        WorkItem acordaoItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.SENTENCA)
                .titulo("Acórdão — " + processo.getNumeroProcesso())
                .descricao("EMENTA: " + ementa + " | DISPOSITIVO: " + dispositivo)
                .queueCode(publicationRoute.queueCode())
                .inboxKey(publicationRoute.inboxKey())
                .assignedRole(publicationRoute.assignedRole())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(acordaoItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.LAVRAR_ACORDAO);
        processoRepository.save(processo);
        commons.publishUserHistory(usuario, "DESEMBARGADOR", "ACORDAO_LAVRADO", "Acórdão lavrado pelo relator.", processo, processoId);
        WorkItem conferenciaItem = decisionSafetyService.registrarConferenciaCruzadaSeNecessario(processo, usuario, "ACORDAO", dispositivo).orElse(null);
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarAcordao(
                processoId,
                acordaoItem.getTitulo(),
                ementa,
                fundamentacao,
                dispositivo,
                resolveOrgaoJulgador(usuario),
                "SEGUNDO_GRAU",
                "ACORDAO_LAVRADO"
        );
        LinkedHashMap<String, Object> extras = new LinkedHashMap<>();
        extras.put("dedupKey", dedupKey);
        extras.put("documentoFormalAssinado", documentoFormalAssinado);
        extras.put("assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"));
        extras.put("validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana"));
        if (conferenciaItem != null) {
            extras.put("conferenciaCruzadaWorkItemId", conferenciaItem.getId());
        }
        return response("ACORDAO_LAVRADO", processoId, acordaoItem.getId(), extras);
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> pedirVista(Long processoId, int diasVista) {
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentDesembargador();
        Instant prazoVista = Instant.now().plus(diasVista, ChronoUnit.DAYS);
        InstitutionalActorRoutingService.InstitutionalRoute vistaRoute = institutionalActorRoutingService.colegiado(processoId, "VISTA_RETORNO");
        WorkItem vistaItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("VISTA:" + processoId + ':' + usuario.getId())
                .type(WorkItemType.VISTA)
                .titulo("Pedido de Vista — " + processo.getNumeroProcesso())
                .descricao("Vista solicitada por " + usuario.getNome() + " por " + diasVista + " dias.")
                .queueCode(vistaRoute.queueCode())
                .inboxKey(vistaRoute.inboxKey())
                .assignedRole(vistaRoute.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .dueAt(prazoVista)
                .build();
        workItemRepository.save(vistaItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.PEDIR_VISTA);
        processoRepository.save(processo);
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                processoId,
                vistaItem.getTitulo(),
                "Pedido de vista formulado em julgamento colegiado com suspensão temporária da conclusão.",
                "Vista concedida por " + diasVista + " dias, com retorno até " + prazoVista + '.',
                resolveOrgaoJulgador(usuario),
                "SEGUNDO_GRAU",
                "PEDIDO_VISTA",
                Map.of("prazoVista", prazoVista.toString(), "diasVista", Integer.toString(diasVista))
        );
        return response("VISTA_CONCEDIDA", processoId, vistaItem.getId(), Map.of(
                "diasVista", diasVista,
                "prazoVista", prazoVista,
                "documentoFormalAssinado", documentoFormalAssinado,
                "assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"),
                "validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana")
        ));
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> registrarDestaque(Long processoId, String motivo) {
        return registrarAtoRelatoria(processoId, "DESTAQUE_REGISTRADO", "PEDIDO_DESTAQUE", "COLEGIADO_DESTAQUE", motivo, WorkItemType.DECISAO);
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> gerenciarSustentacaoOral(Long processoId, boolean deferido, String observacao) {
        String status = deferido ? "SUSTENTACAO_ORAL_DEFERIDA" : "SUSTENTACAO_ORAL_INDEFERIDA";
        String detalhe = (deferido ? "Deferida" : "Indeferida") + " | " + safe(observacao);
        return registrarAtoRelatoria(processoId, status, "SUSTENTACAO_ORAL", "COLEGIADO_SUSTENTACAO_ORAL", detalhe, WorkItemType.DECISAO);
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> abrirSessao(String sessaoId, String pauta) {
        Usuario usuario = currentDesembargador();
        WorkItem item = WorkItem.builder()
                .templateCode(deterministicKey("SESSAO_ABERTA", (long) safe(sessaoId).hashCode(), usuario.getId()))
                .type(WorkItemType.AUDIENCIA)
                .titulo("Abertura de Sessão Colegiada — " + safe(sessaoId))
                .descricao("Pauta: " + safe(pauta))
                .queueCode("COLEGIADO_SESSAO_ABERTA")
                .inboxKey("COLEGIADO_SESSAO_MONITORAMENTO")
                .assignedRole(TipoUsuario.ASSESSOR_DESEMBARGADOR)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .dueAt(Instant.now())
                .build();
        workItemRepository.save(item);
        commons.publishUserHistory(usuario, "DESEMBARGADOR", "SESSAO_ABERTA", "Sessão colegiada aberta pela relatoria.", null, null);
        return Map.of("status", "SESSAO_ABERTA", "sessaoId", safe(sessaoId), "workItemId", item.getId(), "pauta", safe(pauta));
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> fecharSessao(String sessaoId, String pauta) {
        Usuario usuario = currentDesembargador();
        WorkItem item = WorkItem.builder()
                .templateCode(deterministicKey("SESSAO_FECHADA", (long) safe(sessaoId).hashCode(), usuario.getId()))
                .type(WorkItemType.AUDIENCIA)
                .titulo("Fechamento de Sessão Colegiada — " + safe(sessaoId))
                .descricao("Pauta: " + safe(pauta))
                .queueCode("COLEGIADO_SESSAO_FECHADA")
                .inboxKey("SECRETARIA_PUBLICACAO_ACORDAO")
                .assignedRole(TipoUsuario.ASSESSOR_DESEMBARGADOR)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .dueAt(Instant.now())
                .build();
        workItemRepository.save(item);
        commons.publishUserHistory(usuario, "DESEMBARGADOR", "SESSAO_FECHADA", "Sessão colegiada encerrada pela relatoria.", null, null);
        return Map.of("status", "SESSAO_FECHADA", "sessaoId", safe(sessaoId), "workItemId", item.getId(), "pauta", safe(pauta));
    }

    @Transactional
    @CacheEvict(cacheNames = {"desembargador_painel", "desembargador_governanca"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> registrarImpedimentoOuSuspeicao(Long processoId, String tipo, String fundamento) {
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentDesembargador();
        InstitutionalActorRoutingService.InstitutionalRoute incidenteRoute = institutionalActorRoutingService.colegiado(processoId, "REDISTRIBUICAO_IMPEDIMENTO");
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(deterministicKey("IMPEDIMENTO", processoId, usuario.getId()))
                .type(WorkItemType.OUTRO)
                .titulo("Incidente de " + safe(tipo) + " — " + processo.getNumeroProcesso())
                .descricao("Fundamento: " + safe(fundamento))
                .queueCode(incidenteRoute.queueCode())
                .inboxKey(incidenteRoute.inboxKey())
                .assignedRole(incidenteRoute.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .dueAt(Instant.now().plus(12, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(item);
        commons.publishUserHistory(usuario, "DESEMBARGADOR", "INCIDENTE_IMPEDIMENTO", "Incidente de impedimento/suspeição registrado.", processo, processoId);
        return response("INCIDENTE_REGISTRADO", processoId, item.getId(), Map.of("tipo", safe(tipo), "fundamento", safe(fundamento)));
    }

    private Map<String, Object> registrarAtoRelatoria(Long processoId,
                                                      String status,
                                                      String tituloPrefixo,
                                                      String queueCode,
                                                      String detalhe,
                                                      WorkItemType type) {
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentDesembargador();
        InstitutionalActorRoutingService.InstitutionalRoute governanceRoute = institutionalActorRoutingService.colegiado(processoId, queueCode);
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(deterministicKey(status, processoId, usuario.getId()))
                .type(type)
                .titulo(tituloPrefixo + " — " + processo.getNumeroProcesso())
                .descricao(safe(detalhe))
                .queueCode(governanceRoute.queueCode())
                .inboxKey(governanceRoute.inboxKey())
                .assignedRole(governanceRoute.assignedRole())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .dueAt(Instant.now().plus(6, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(item);
        commons.publishUserHistory(usuario, "DESEMBARGADOR", status, detalhe, processo, processoId);
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                processoId,
                item.getTitulo(),
                detalhe,
                status,
                resolveOrgaoJulgador(usuario),
                "SEGUNDO_GRAU",
                tituloPrefixo,
                Map.of("queueCode", queueCode)
        );
        return response(status, processoId, item.getId(), Map.of(
                "queueCode", queueCode,
                "documentoFormalAssinado", documentoFormalAssinado,
                "assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"),
                "validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana")
        ));
    }

    private Usuario currentDesembargador() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = requireDesembargador(ctx.usuario());
        authorizationService.requireRole(usuario, "ROLE_DESEMBARGADOR", "ROLE_DESEMBARGADOR_FEDERAL");
        return usuario;
    }

    private Usuario requireDesembargador(Usuario usuario) {
        if (!COLEGIADO_ROLES.contains(usuario.getTipoUsuario())) {
            throw new AccessDeniedPjbException("Acesso restrito a desembargadores.");
        }
        return usuario;
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private Map<String, Object> response(String status, Long processoId, Long workItemId, Map<String, Object> extras) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("processoId", processoId);
        out.put("workItemId", workItemId);
        out.putAll(extras);
        return out;
    }

    private String resolveTribunal(Usuario usuario) {
        return switch (usuario.getTipoUsuario()) {
            case DESEMBARGADOR -> "TJ_" + (usuario.getUf() == null ? "BR" : usuario.getUf().toUpperCase());
            case DESEMBARGADOR_FEDERAL -> "TRF_" + (usuario.getUf() == null ? "BR" : usuario.getUf().toUpperCase());
            default -> "TRIBUNAL_DESCONHECIDO";
        };
    }

    private String resolveOrgaoJulgador(Usuario usuario) {
        return "CAMARA_" + (usuario.getComarca() == null ? "GERAL" : usuario.getComarca().toUpperCase());
    }

    private boolean isRelator(WorkItem item) {
        return commons.titleContains(item, "RELATOR", "RECURSO_RELATAR", "APELACAO_RELATAR");
    }

    private boolean isVotante(WorkItem item) {
        return commons.titleContains(item, "VOTO", "SESSAO_JULGAMENTO", "PAUTA");
    }

    private boolean isAcordaoPendente(WorkItem item) {
        return commons.titleContains(item, "ACORDAO", "LAVRATURA", "ASSINATURA_ACORDAO");
    }

    private boolean isPedidoVista(WorkItem item) {
        return commons.titleContains(item, "VISTA", "PEDIDO_VISTA");
    }

    private boolean isSessao(WorkItem item) {
        return commons.titleContains(item, "SESSAO", "PLENARIO", "TURMA", "CAMARA");
    }

    private String deterministicKey(String prefix, Long resourceId, Long usuarioId) {
        return UUID.nameUUIDFromBytes((prefix + ':' + resourceId + ':' + usuarioId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String normalized = value.trim();
        return normalized.length() <= 400 ? normalized : normalized.substring(0, 400);
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    public record ColegialdoSnapshot(
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            String tribunal,
            String orgaoJulgador,
            List<String> recursosParaRelatar,
            List<String> votosParaProferir,
            List<String> acordaosParaAssinatura,
            List<String> pedidosDeVista,
            int sessoesProgramadas,
            int prazosUrgentes,
            List<?> prazoRadar,
            Object sessionRisk,
            Map<String, Object> operationalSignals,
            Map<String, Object> nativeComposition,
            Map<String, Object> collectionComposition,
            Map<String, Object> actionSurface,
            Map<String, Object> executionSurface,
            Map<String, Object> sharedExperience
    ) {
    }

    public record CamaraGestaoDashboard(
            LocalDateTime generatedAt,
            String tribunal,
            String orgaoJulgador,
            long acervoTribunal,
            long julgadosPendentes,
            long votosPendentes,
            long acordaosPendentes,
            long vistasAbertas,
            long sessoesAbertas,
            long prazosCriticos,
            double taxaVistaPercent,
            double taxaBacklogPercent,
            List<String> alertasEstruturais,
            List<String> alertasCamara
    ) {
    }
}
