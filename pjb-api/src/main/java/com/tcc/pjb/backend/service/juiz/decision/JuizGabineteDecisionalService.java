package com.tcc.pjb.backend.service.juiz.decision;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.dto.juiz.GabineteKanbanResponse;
import com.tcc.pjb.backend.model.dto.juiz.PrazoFatalDto;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.juiz.session.GabineteOperationalProfile;
import com.tcc.pjb.backend.service.juiz.session.GabineteOperationalProfileResolver;
import com.tcc.pjb.backend.service.juiz.session.GabineteSessionProfile;
import com.tcc.pjb.backend.service.juiz.session.GabineteSessionProfileResolver;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
import com.tcc.pjb.backend.service.juiz.topology.JuizGabineteQueueIsolationService;
import com.tcc.pjb.backend.service.processual.pauta.PautaAudienciaNacionalService;
import com.tcc.pjb.backend.service.processo.DespachoInicialContinuityOrchestratorService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;

@Service
public class JuizGabineteDecisionalService {

    private static final EnumSet<TipoUsuario> MAGISTRATURA_GABINETE = EnumSet.of(
            TipoUsuario.JUIZ,
            TipoUsuario.JUIZ_ESTADUAL,
            TipoUsuario.JUIZ_FEDERAL,
            TipoUsuario.JUIZ_ESPECIAL,
            TipoUsuario.JUIZ_ELEITORAL,
            TipoUsuario.JUIZ_TRABALHISTA,
            TipoUsuario.JUIZ_MILITAR,
            TipoUsuario.MAGISTRADO
    );

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final PautaAudienciaNacionalService pautaAudienciaNacionalService;
    private final DecisionSafetyService decisionSafetyService;
    private final CaseContinuityDecisionGateService caseContinuityDecisionGateService;
    private final GabineteOperationalProfileResolver operationalProfileResolver;
    private final GabineteSessionProfileResolver sessionProfileResolver;
    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    private final DespachoInicialContinuityOrchestratorService despachoInicialContinuityOrchestratorService;
    private final JuizProcessoGuardRailService guardRailService;
    private final JuizGabineteRoutingResolver juizGabineteRoutingResolver;
    private final JuizGabineteQueueIsolationService juizGabineteQueueIsolationService;
    private final OfficialDocumentTemplateService officialDocumentTemplateService;

    public JuizGabineteDecisionalService(PerfilDashboardContextFactory contextFactory,
                                         PainelServiceCommons commons,
                                         ProcessoRepository processoRepository,
                                         WorkItemRepository workItemRepository,
                                         PjbAuthorizationService authorizationService,
                                         ProcessoLifecycleMachine lifecycleMachine,
                                         PautaAudienciaNacionalService pautaAudienciaNacionalService,
                                         DecisionSafetyService decisionSafetyService,
                                         CaseContinuityDecisionGateService caseContinuityDecisionGateService,
                                         GabineteOperationalProfileResolver operationalProfileResolver,
                                         GabineteSessionProfileResolver sessionProfileResolver,
                                         RepresentacaoProcessualPolicyService representacaoProcessualPolicyService,
                                         DespachoInicialContinuityOrchestratorService despachoInicialContinuityOrchestratorService,
                                         JuizProcessoGuardRailService guardRailService,
                                         JuizGabineteRoutingResolver juizGabineteRoutingResolver,
                                         JuizGabineteQueueIsolationService juizGabineteQueueIsolationService,
                                         OfficialDocumentTemplateService officialDocumentTemplateService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.lifecycleMachine = lifecycleMachine;
        this.pautaAudienciaNacionalService = pautaAudienciaNacionalService;
        this.decisionSafetyService = decisionSafetyService;
        this.caseContinuityDecisionGateService = caseContinuityDecisionGateService;
        this.operationalProfileResolver = operationalProfileResolver;
        this.sessionProfileResolver = sessionProfileResolver;
        this.representacaoProcessualPolicyService = representacaoProcessualPolicyService;
        this.despachoInicialContinuityOrchestratorService = despachoInicialContinuityOrchestratorService;
        this.guardRailService = guardRailService;
        this.juizGabineteRoutingResolver = juizGabineteRoutingResolver;
        this.juizGabineteQueueIsolationService = juizGabineteQueueIsolationService;
        this.officialDocumentTemplateService = officialDocumentTemplateService;
    }

    @Cacheable(cacheNames = "juiz_painel", key = "'bootstrap:' + @currentUserService.currentUserIdOrZero()", condition = "@cacheRuntime.redisEnabled()")
    public GabineteDecisionalSnapshot bootstrapGabinete() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        if (!MAGISTRATURA_GABINETE.contains(usuario.getTipoUsuario())) {
            throw new AccessDeniedPjbException("Acesso restrito a magistratura singular.");
        }

        List<WorkItem> inbox = juizGabineteQueueIsolationService.filtrarInboxCompativel(usuario, commons.inboxHibrido(usuario, 60));
        List<CalendarEventDto> agenda = commons.agenda(LocalDate.now(), LocalDate.now().plusDays(14));
        GabineteOperationalProfile profile = operationalProfileResolver.resolve(usuario, inbox, agenda);
        GabineteSessionProfile sessionProfile = sessionProfileResolver.resolve(usuario, inbox, agenda, profile);

        List<FilaDecisionalItem> filaDecisional = inbox.stream()
                .filter(this::isDecisao)
                .sorted(this::compareByPrioridadeEPrazo)
                .limit(30)
                .map(this::toFilaItem)
                .toList();
        List<FilaDecisionalItem> minutasPendentes = inbox.stream()
                .filter(this::isMinutaPendente)
                .limit(20)
                .map(this::toFilaItem)
                .toList();
        List<FilaDecisionalItem> audienciasAgendadas = agenda.stream()
                .map(this::toFilaItemFromCalendar)
                .toList();

        long acervoTotal = processoRepository.findForMagistradoDashboard(
                usuario.getUf(),
                usuario.getComarca(),
                PageRequest.of(0, 1)
        ).getTotalElements();
        int processosSentenciados = (int) inbox.stream().filter(this::isSentencaPendente).count();
        int processosInstruidos = (int) inbox.stream().filter(this::isInstrucaoConcluida).count();
        int recursosResponder = (int) inbox.stream().filter(this::isRecursoParaContrarrazao).count();
        int prazos24h = (int) inbox.stream().filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS))).count();
        int prazos48h = (int) inbox.stream().filter(i -> i.getDueAt() != null
                && i.getDueAt().isBefore(Instant.now().plus(48, ChronoUnit.HOURS))
                && !i.getDueAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS))).count();
        String gabineteKey = Optional.ofNullable(profile.metadata().get("gabineteKey"))
                .map(String::valueOf)
                .filter(v -> !v.isBlank())
                .orElseGet(() -> "GABINETE_" + Optional.ofNullable(usuario.getComarca())
                        .map(v -> v.toUpperCase(Locale.ROOT).replace(' ', '_'))
                        .orElse("PADRAO"));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(profile.toMap());
        metadata.putAll(sessionProfile.toMap());
        metadata.put("gabineteDescriptor", profile.descriptor());
        metadata.put("sessionDescriptor", sessionProfile.descriptor());
        metadata.put("topologicalInboxSize", inbox.size());
        metadata.put("topologicalIsolation", "ATIVO");
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new GabineteDecisionalSnapshot(
                ctx.generatedAt(),
                ctx.perfilAtivo(),
                ctx.tratamento(),
                gabineteKey,
                acervoTotal,
                processosSentenciados,
                processosInstruidos,
                recursosResponder,
                prazos24h,
                prazos48h,
                profile.decisionDesk(),
                profile.advisoryDesk(),
                profile.recursalSupportDesk(),
                profile.hearingDesk(),
                profile.coordinationDesk(),
                profile.sessionChannel(),
                sessionProfile.sessionDesk(),
                sessionProfile.sessionSecretariatDesk(),
                sessionProfile.draftingDesk(),
                sessionProfile.hearingSupportDesk(),
                sessionProfile.hearingWindow(),
                sessionProfile.sessionCadence(),
                sessionProfile.colegiadoChannel(),
                sessionProfile.chamberLabel(),
                sessionProfile.relatoriaDesk(),
                sessionProfile.publicationDesk(),
                sessionProfile.sessionRoom(),
                sessionProfile.quorumLabel(),
                sessionProfile.publicationMode(),
                sessionProfile.escalationMode(),
                sessionProfile.requiresClerkReinforcement(),
                sessionProfile.requiresSessionReview(),
                profile.loadBand(),
                profile.coordinationMode(),
                profile.urgentItems(),
                profile.blockingItems(),
                profile.recursalItems(),
                profile.hearingItems(),
                profile.secrecyItems(),
                filaDecisional,
                minutasPendentes,
                audienciasAgendadas,
                mergeLabels(profile.labels(), sessionProfile.labels()),
                Collections.unmodifiableMap(metadata),
                ctx.prazoRadar(),
                ctx.sessionRisk()
        );
    }

    @Cacheable(cacheNames = "juiz_kanban", key = "@currentUserService.currentUserIdOrZero()", condition = "@cacheRuntime.redisEnabled()")
    public GabineteKanbanResponse kanban() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<WorkItem> inbox = juizGabineteQueueIsolationService.filtrarInboxCompativel(usuario, commons.inboxHibrido(usuario, 120));
        List<CalendarEventDto> agenda = commons.agenda(LocalDate.now(), LocalDate.now().plusDays(14));
        GabineteOperationalProfile profile = operationalProfileResolver.resolve(usuario, inbox, agenda);

        List<FilaDecisionalItem> decisional = inbox.stream()
                .filter(this::isDecisao)
                .sorted(this::compareByPrioridadeEPrazo)
                .limit(30)
                .map(this::toFilaItem)
                .toList();
        List<FilaDecisionalItem> assessoria = inbox.stream()
                .filter(this::isMinutaPendente)
                .limit(30)
                .map(this::toFilaItem)
                .toList();
        List<FilaDecisionalItem> recursal = inbox.stream()
                .filter(this::isRecursoParaContrarrazao)
                .limit(20)
                .map(this::toFilaItem)
                .toList();
        List<FilaDecisionalItem> audiencia = agenda.stream()
                .limit(20)
                .map(this::toFilaItemFromCalendar)
                .toList();
        List<PrazoFatalDto> prazosFatais = inbox.stream()
                .filter(item -> item.getDueAt() != null)
                .sorted(this::compareByPrioridadeEPrazo)
                .limit(15)
                .map(item -> {
                    Duration restante = Duration.between(Instant.now(), item.getDueAt());
                    long horas = restante.toHours();
                    return new PrazoFatalDto(
                            item.getId(),
                            item.getProcesso() != null ? item.getProcesso().getId() : null,
                            item.getTitulo(),
                            item.getDueAt(),
                            horas,
                            restante.isNegative()
                    );
                })
                .toList();
        return new GabineteKanbanResponse(decisional, assessoria, recursal, audiencia, prazosFatais, profile.loadBand());
    }

    public List<Map<String, Object>> filaDecisionalPaginada(int page, int size) {
        PerfilDashboardContext ctx = contextFactory.build();
        return juizGabineteQueueIsolationService.filtrarInboxCompativel(ctx.usuario(), commons.inboxHibrido(ctx.usuario(), size * (page + 2))).stream()
                .filter(this::isDecisao)
                .sorted(this::compareByPrioridadeEPrazo)
                .skip((long) page * size)
                .limit(size)
                .map(commons::mapWorkItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public JuizProcessoGuardRailService.GuardRailSnapshot guardrails(Long processoId) {
        return guardRailService.avaliar(processoId, "ATUACAO_JUDICIAL");
    }

    @Transactional
    @CacheEvict(cacheNames = {"juiz_painel", "juiz_kanban"}, allEntries = true, condition = "@cacheRuntime.redisEnabled()")
    public Map<String, Object> assinarDespacho(Long processoId, String conteudo, String fundamentacao) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL");

        caseContinuityDecisionGateService.requireAllowed(processoId, ProcessoLifecycleAction.ASSINAR_DESPACHO);
        decisionSafetyService.requireSafeDecisionContext(processo, usuario, "DESPACHO", conteudo, fundamentacao);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.requireAtuacaoPermitida(processo, usuario, "ASSINAR_DESPACHO");
        JuizGabineteRoutingProfile gabineteRouting = juizGabineteRoutingResolver.resolve(processo);
        SecretariatOperationalRoutingProfile secretariatRouting = gabineteRouting.secretariatRouting();
        String dedupKey = deterministicKey("DESPACHO", processoId, usuario.getId());
        WorkItem despacho = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.DESPACHO)
                .titulo("Despacho judicial — " + processo.getNumeroProcesso())
                .descricao(conteudo)
                .queueCode(firstNonBlank(secretariatRouting.executionQueueCode(), "GABINETE_DECISAO"))
                .inboxKey(firstNonBlank(secretariatRouting.executionInboxKey(), "SECRETARIA_JUNTADA"))
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(despacho);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.ASSINAR_DESPACHO);
        processoRepository.save(processo);
        commons.publishUserHistory(usuario, "JUIZ", "DESPACHO_ASSINADO", "Despacho assinado no gabinete decisório.", processo, processoId);
        var conferenciaItem = decisionSafetyService.registrarConferenciaCruzadaSeNecessario(processo, usuario, "DESPACHO", conteudo).orElse(null);
        var continuidade = despachoInicialContinuityOrchestratorService.onDespachoInicialAssinado(processo, usuario, conteudo, fundamentacao);
        OfficialDocumentTemplateRenderResponse documentoAssinado = renderMagistrateDocument(processo, TemplateDocumentoOficial.DESPACHO, "Despacho judicial — " + processo.getNumeroProcesso(), Map.of(
                "fundamentacao", defaultText(fundamentacao, "Fundamentação judicial lançada no gabinete."),
                "determinacao", defaultText(conteudo, "Determinação judicial materializada no fluxo do gabinete.")
        ));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ASSINADO");
        out.put("processoId", processoId);
        out.put("workItemId", despacho.getId());
        out.put("dedupKey", dedupKey);
        out.put("continuidadeInicial", continuidade.asMap());
        out.put("guardRail", guard.metrics());
        out.put("roteamentoSecretaria", secretariatRouting.toMap());
        out.put("roteamentoGabinete", gabineteRouting.toMap());
        out.put("documentoAssinado", summarizeRenderedDocument(documentoAssinado));
        if (conferenciaItem != null) {
            out.put("conferenciaCruzadaWorkItemId", conferenciaItem.getId());
        }
        return out;
    }

    @Transactional
    public Map<String, Object> proferirSentenca(Long processoId,
                                                String dispositivo,
                                                String fundamentacao,
                                                String tipoSentenca) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL");

        caseContinuityDecisionGateService.requireAllowed(processoId, ProcessoLifecycleAction.PROFERIR_SENTENCA);
        decisionSafetyService.requireSafeDecisionContext(processo, usuario, "SENTENCA", dispositivo, fundamentacao);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.requireAtuacaoPermitida(processo, usuario, "PROFERIR_SENTENCA");
        JuizGabineteRoutingProfile gabineteRouting = juizGabineteRoutingResolver.resolve(processo);
        SecretariatOperationalRoutingProfile secretariatRouting = gabineteRouting.secretariatRouting();
        String dedupKey = deterministicKey("SENTENCA", processoId, usuario.getId());
        WorkItem sentencaItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.SENTENCA)
                .titulo("Sentença — " + processo.getNumeroProcesso() + " — " + tipoSentenca)
                .descricao(dispositivo)
                .queueCode(firstNonBlank(secretariatRouting.executionQueueCode(), "GABINETE_SENTENCA"))
                .inboxKey(firstNonBlank(secretariatRouting.executionInboxKey(), "SECRETARIA_PUBLICACAO_SENTENCA"))
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(sentencaItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.PROFERIR_SENTENCA);
        processoRepository.save(processo);
        commons.publishUserHistory(usuario, "JUIZ", "SENTENCA_PROFERIDA", "Sentença proferida e encaminhada para publicação.", processo, processoId);
        WorkItem conferenciaItem = decisionSafetyService.registrarConferenciaCruzadaSeNecessario(processo, usuario, "SENTENCA", dispositivo).orElse(null);

        OfficialDocumentTemplateRenderResponse documentoAssinado = renderMagistrateDocument(processo, TemplateDocumentoOficial.SENTENCA, "Sentença — " + processo.getNumeroProcesso() + " — " + defaultText(tipoSentenca, "PADRAO"), Map.of(
                "relatorio", buildSentenceReport(processo, tipoSentenca),
                "fundamentacao", defaultText(fundamentacao, "Fundamentação judicial lançada no gabinete."),
                "dispositivo", defaultText(dispositivo, "Dispositivo judicial materializado no gabinete.")
        ));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SENTENCA_PROFERIDA");
        out.put("tipo", tipoSentenca);
        out.put("processoId", processoId);
        out.put("workItemId", sentencaItem.getId());
        out.put("dedupKey", dedupKey);
        out.put("encaminhadoPara", firstNonBlank(secretariatRouting.executionInboxKey(), "SECRETARIA_PUBLICACAO_SENTENCA"));
        out.put("guardRail", guard.metrics());
        out.put("roteamentoSecretaria", secretariatRouting.toMap());
        out.put("roteamentoGabinete", gabineteRouting.toMap());
        out.put("documentoAssinado", summarizeRenderedDocument(documentoAssinado));
        if (conferenciaItem != null) {
            out.put("conferenciaCruzadaWorkItemId", conferenciaItem.getId());
        }
        return out;
    }

    @Transactional
    public Map<String, Object> proferirDecisaoInterlocutoria(Long processoId,
                                                             String dispositivo,
                                                             String fundamentacao,
                                                             String tipoDecisao) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL");

        caseContinuityDecisionGateService.requireAllowed(processoId, ProcessoLifecycleAction.ASSINAR_DESPACHO);
        decisionSafetyService.requireSafeDecisionContext(processo, usuario, "DECISAO_INTERLOCUTORIA", dispositivo, fundamentacao);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.requireAtuacaoPermitida(processo, usuario, "PROFERIR_DECISAO_INTERLOCUTORIA");
        JuizGabineteRoutingProfile gabineteRouting = juizGabineteRoutingResolver.resolve(processo);
        SecretariatOperationalRoutingProfile secretariatRouting = gabineteRouting.secretariatRouting();
        String dedupKey = deterministicKey("DECISAO_INTERLOCUTORIA", processoId, usuario.getId());
        WorkItem decisaoItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.DECISAO)
                .titulo("Decisão interlocutória — " + processo.getNumeroProcesso() + " — " + defaultText(tipoDecisao, "PADRAO"))
                .descricao(dispositivo)
                .queueCode(firstNonBlank(secretariatRouting.executionQueueCode(), "GABINETE_DECISAO_INTERLOCUTORIA"))
                .inboxKey(firstNonBlank(secretariatRouting.executionInboxKey(), "SECRETARIA_PUBLICACAO_DECISAO"))
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(decisaoItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.ASSINAR_DESPACHO);
        processoRepository.save(processo);
        commons.publishUserHistory(usuario, "JUIZ", "DECISAO_INTERLOCUTORIA_PROFERIDA", "Decisão interlocutória proferida e encaminhada para publicação controlada.", processo, processoId);
        WorkItem conferenciaItem = decisionSafetyService.registrarConferenciaCruzadaSeNecessario(processo, usuario, "DECISAO_INTERLOCUTORIA", dispositivo).orElse(null);

        OfficialDocumentTemplateRenderResponse documentoAssinado = renderMagistrateDocument(processo, TemplateDocumentoOficial.DECISAO, "Decisão interlocutória — " + processo.getNumeroProcesso() + " — " + defaultText(tipoDecisao, "PADRAO"), Map.of(
                "fundamentacao", defaultText(fundamentacao, "Fundamentação interlocutória lançada no gabinete."),
                "dispositivo", defaultText(dispositivo, "Dispositivo interlocutório materializado no gabinete.")
        ));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "DECISAO_INTERLOCUTORIA_PROFERIDA");
        out.put("tipo", tipoDecisao);
        out.put("processoId", processoId);
        out.put("workItemId", decisaoItem.getId());
        out.put("dedupKey", dedupKey);
        out.put("encaminhadoPara", firstNonBlank(secretariatRouting.executionInboxKey(), "SECRETARIA_PUBLICACAO_DECISAO"));
        out.put("guardRail", guard.metrics());
        out.put("roteamentoSecretaria", secretariatRouting.toMap());
        out.put("roteamentoGabinete", gabineteRouting.toMap());
        out.put("documentoAssinado", summarizeRenderedDocument(documentoAssinado));
        if (conferenciaItem != null) {
            out.put("conferenciaCruzadaWorkItemId", conferenciaItem.getId());
        }
        return out;
    }

    @Transactional
    public Map<String, Object> designarAudiencia(Long processoId,
                                                 LocalDateTime dataHora,
                                                 String tipo,
                                                 String local) {
        Instant resolved = dataHora == null ? null : dataHora.toInstant(ZoneOffset.of("-03:00"));
        return designarAudiencia(processoId, resolved, tipo, local);
    }

    @Transactional
    public Map<String, Object> designarAudiencia(Long processoId,
                                                 Instant dataHora,
                                                 String tipo,
                                                 String local) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL");

        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.requireAtuacaoPermitida(processo, usuario, "DESIGNAR_AUDIENCIA");
        JuizGabineteRoutingProfile gabineteRouting = juizGabineteRoutingResolver.resolve(processo);
        SecretariatOperationalRoutingProfile secretariatRouting = gabineteRouting.secretariatRouting();
        Instant effectiveDateTime = dataHora == null ? Instant.now().plus(72, ChronoUnit.HOURS) : dataHora;
        String effectiveTipo = firstNonBlank(tipo, "AUDIENCIA_DE_INSTRUCAO");
        String effectiveLocal = firstNonBlank(local, secretariatRouting.hearingRoomPrefix() + "_SALA_01");

        var pauta = pautaAudienciaNacionalService.registrar(
                processo,
                usuario,
                LocalDateTime.ofInstant(effectiveDateTime, ZoneOffset.of("-03:00")),
                60,
                effectiveTipo,
                effectiveLocal
        );

        WorkItem audienciaItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("AUDIENCIA:" + processoId + ':' + effectiveDateTime.toEpochMilli())
                .type(WorkItemType.AUDIENCIA)
                .titulo("Audiência " + effectiveTipo + " — " + processo.getNumeroProcesso())
                .descricao("Local: " + effectiveLocal)
                .queueCode(firstNonBlank(secretariatRouting.audienceQueueCode(), "SECRETARIA_AGENDA_AUDIENCIA"))
                .inboxKey(firstNonBlank(secretariatRouting.audienceInboxKey(), "SECRETARIA_AGENDA_AUDIENCIA"))
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .dueAt(effectiveDateTime)
                .build();
        workItemRepository.save(audienciaItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.DESIGNAR_AUDIENCIA);
        processoRepository.save(processo);
        commons.publishUserHistory(usuario, "JUIZ", "AUDIENCIA_DESIGNADA", "Audiência designada pelo gabinete.", processo, processoId);

        var representacaoAudiencia = representacaoProcessualPolicyService.resolve(
                processo,
                null,
                processo != null && processo.getRito() != null && processo.getRito().isTrabalhista() ? "PROCURACAO_APUD_ACTA" : null,
                audienciaItem.getId(),
                effectiveTipo,
                effectiveTipo.toUpperCase(Locale.ROOT).contains("CONCILIACAO") || effectiveTipo.toUpperCase(Locale.ROOT).contains("MEDIACAO"),
                effectiveTipo.toUpperCase(Locale.ROOT).contains("CONCILIACAO") || effectiveTipo.toUpperCase(Locale.ROOT).contains("MEDIACAO"),
                null,
                null
        );

        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("status", "AUDIENCIA_DESIGNADA");
        response.put("tipo", effectiveTipo);
        response.put("dataHora", effectiveDateTime);
        response.put("workItemId", audienciaItem.getId());
        response.put("calendarEventId", pauta.eventId());
        response.put("pautaKey", pauta.pautaKey());
        response.put("representacaoAudiencia", representacaoAudiencia.envelope());
        response.put("guardRail", guard.metrics());
        response.put("roteamentoSecretaria", secretariatRouting.toMap());
        return Map.copyOf(response);
    }

    private int compareByPrioridadeEPrazo(WorkItem a, WorkItem b) {
        int primary = Integer.compare(
                a.getPrioridade() == null ? Integer.MAX_VALUE : a.getPrioridade(),
                b.getPrioridade() == null ? Integer.MAX_VALUE : b.getPrioridade()
        );
        if (primary != 0) {
            return primary;
        }
        if (a.getDueAt() == null && b.getDueAt() == null) {
            return 0;
        }
        if (a.getDueAt() == null) {
            return 1;
        }
        if (b.getDueAt() == null) {
            return -1;
        }
        return a.getDueAt().compareTo(b.getDueAt());
    }

    private FilaDecisionalItem toFilaItem(WorkItem item) {
        return new FilaDecisionalItem(
                item.getId(),
                item.getTitulo(),
                item.getType() == null ? "INDEFINIDO" : item.getType().name(),
                item.getDueAt(),
                item.getPrioridade(),
                item.getStatus() == null ? "PENDENTE" : item.getStatus().name(),
                item.getProcesso() == null ? null : item.getProcesso().getId(),
                item.getQueueCode(),
                item.getInboxKey(),
                resolveDeskAxis(item),
                item.isBlocking()
        );
    }

    private FilaDecisionalItem toFilaItemFromCalendar(CalendarEventDto event) {
        return new FilaDecisionalItem(
                null,
                event.title(),
                "AUDIENCIA",
                event.start() == null ? null : event.start().toInstant(ZoneOffset.of("-03:00")),
                1,
                "AGENDADO",
                null,
                "PAUTA_AUDIENCIA",
                "AGENDA_GABINETE",
                "AUDIENCIA",
                false
        );
    }

    private boolean isDecisao(WorkItem item) {
        return commons.titleContains(item, "DECISAO", "DESPACHO", "SENTENCA", "JULGAMENTO", "ACÓRDÃO", "RECURSO");
    }

    private boolean isMinutaPendente(WorkItem item) {
        return commons.titleContains(item, "MINUTA", "DRAFT", "RASCUNHO");
    }

    private boolean isSentencaPendente(WorkItem item) {
        return commons.titleContains(item, "SENTENCA", "JULGAR", "INSTRUIDO");
    }

    private boolean isInstrucaoConcluida(WorkItem item) {
        return commons.titleContains(item, "INSTRUCAO_CONCLUIDA", "CONCLUSO");
    }

    private boolean isRecursoParaContrarrazao(WorkItem item) {
        return commons.titleContains(item, "RECURSO", "APELACAO", "AGRAVO", "CONTRARRAZOES", "RESPOSTA_RECURSO");
    }


    private List<String> mergeLabels(List<String> primary, List<String> secondary) {
        java.util.LinkedHashSet<String> labels = new java.util.LinkedHashSet<>();
        if (primary != null) {
            labels.addAll(primary);
        }
        if (secondary != null) {
            labels.addAll(secondary);
        }
        return List.copyOf(labels);
    }

    private String resolveDeskAxis(WorkItem item) {
        String source = ((item.getQueueCode() == null ? "" : item.getQueueCode()) + ' '
                + (item.getInboxKey() == null ? "" : item.getInboxKey()) + ' '
                + (item.getTitulo() == null ? "" : item.getTitulo()))
                .toUpperCase(Locale.ROOT);
        if (source.contains("RECURS")) {
            return "RECURSAL";
        }
        if (source.contains("AUDIENC")) {
            return "AUDIENCIA";
        }
        if (source.contains("SENTENCA") || source.contains("DECISAO") || source.contains("DESPACHO")) {
            return "DECISORIO";
        }
        if (source.contains("SIGILO") || source.contains("SEGREDO")) {
            return "SIGILO";
        }
        return "GABINETE";
    }

    private OfficialDocumentTemplateRenderResponse renderMagistrateDocument(Processo processo,
                                                                          TemplateDocumentoOficial template,
                                                                          String titulo,
                                                                          Map<String, String> variaveis) {
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                template,
                titulo,
                variaveis,
                Boolean.TRUE,
                Boolean.TRUE
        ));
    }

    private Map<String, Object> summarizeRenderedDocument(OfficialDocumentTemplateRenderResponse render) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("documentoId", render.documentoId());
        out.put("template", render.template().name());
        out.put("tituloDocumento", render.tituloDocumento());
        out.put("hashSha256", render.hashSha256());
        out.put("selado", render.selado());
        out.put("assinaturaQualificada", render.assinaturaQualificada());
        out.put("validacaoSoberana", render.validacaoSoberana());
        return Collections.unmodifiableMap(out);
    }

    private String buildSentenceReport(Processo processo,
                                       String tipoSentenca) {
        return "Relatório sintético do processo " + processo.getNumeroProcesso() + " para sentença do tipo " + defaultText(tipoSentenca, "PADRAO") + '.';
    }

    private String defaultText(String value,
                               String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String deterministicKey(String prefix, Long processoId, Long usuarioId) {
        String raw = prefix + ':' + processoId + ':' + usuarioId;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
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

    public record GabineteDecisionalSnapshot(
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            String gabineteKey,
            long acervoTotal,
            int processosSentenciados,
            int processosInstruidos,
            int recursosResponder,
            int prazos24h,
            int prazos48h,
            String decisionDesk,
            String advisoryDesk,
            String recursalSupportDesk,
            String hearingDesk,
            String coordinationDesk,
            String sessionChannel,
            String sessionDesk,
            String sessionSecretariatDesk,
            String draftingDesk,
            String hearingSupportDesk,
            String hearingWindow,
            String sessionCadence,
            String colegiadoChannel,
            String chamberLabel,
            String relatoriaDesk,
            String publicationDesk,
            String sessionRoom,
            String quorumLabel,
            String publicationMode,
            String escalationMode,
            boolean requiresClerkReinforcement,
            boolean requiresSessionReview,
            String loadBand,
            String coordinationMode,
            int urgentItems,
            int blockingItems,
            int recursalItems,
            int hearingItems,
            int secrecyItems,
            List<FilaDecisionalItem> filaDecisional,
            List<FilaDecisionalItem> minutasPendentes,
            List<FilaDecisionalItem> audienciasAgendadas,
            List<String> labels,
            Map<String, Object> metadata,
            List<?> prazoRadar,
            Object sessionRisk) {
    }

    public record FilaDecisionalItem(
            Long workItemId,
            String titulo,
            String tipo,
            Instant dueAt,
            Integer prioridade,
            String status,
            Long processoId,
            String queueCode,
            String inboxKey,
            String deskAxis,
            boolean blocking) {
    }
}
