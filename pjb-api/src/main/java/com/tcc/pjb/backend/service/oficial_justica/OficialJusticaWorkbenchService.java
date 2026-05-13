package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPessoaRastreioResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoWorkbenchResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaWorkbenchService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final PjbTimeService timeService;
    private final PjbAuthorizationService authorizationService;
    private final OficialJusticaProcessoVinculoService vinculoService;
    private final ProcessoRepository processoRepository;
    private final OficialJusticaOficioSecurityService oficioSecurityService;
    private final OficialJusticaEnderecoTriageService enderecoTriageService;
    private final OficialJusticaOrganizationalScopeService organizationalScopeService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final OficialJusticaExecucaoVivaService execucaoVivaService;
    private final OficialJusticaPanelEgressService panelEgressService;

    public OficialJusticaWorkbenchService(PerfilDashboardContextFactory contextFactory,
                                          PainelServiceCommons commons,
                                          PjbTimeService timeService,
                                          PjbAuthorizationService authorizationService,
                                          OficialJusticaProcessoVinculoService vinculoService,
                                          ProcessoRepository processoRepository,
                                          OficialJusticaOficioSecurityService oficioSecurityService,
                                          OficialJusticaEnderecoTriageService enderecoTriageService,
                                          OficialJusticaOrganizationalScopeService organizationalScopeService,
                                          OficialJusticaContextEnvelopeService contextEnvelopeService,
                                          OficialJusticaExecucaoVivaService execucaoVivaService,
                                          OficialJusticaPanelEgressService panelEgressService) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.commons = Objects.requireNonNull(commons);
        this.timeService = Objects.requireNonNull(timeService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.vinculoService = Objects.requireNonNull(vinculoService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.oficioSecurityService = Objects.requireNonNull(oficioSecurityService);
        this.enderecoTriageService = Objects.requireNonNull(enderecoTriageService);
        this.organizationalScopeService = Objects.requireNonNull(organizationalScopeService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.execucaoVivaService = Objects.requireNonNull(execucaoVivaService);
        this.panelEgressService = Objects.requireNonNull(panelEgressService);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painelResumo() {
        Usuario usuario = contextFactory.build().usuario();
        OficialJusticaPanelEgressService.VisibilitySnapshot hygieneSnapshot = panelEgressService.reconcileVisibility(
                usuario,
                organizationalScopeService.filterByScope(usuario, vinculoService.vinculosDiretosUsuario(usuario.getId(), usuario.getTipoUsuario(), 120))
        );
        List<WorkItem> items = hygieneSnapshot.visibleItems();
        OficialJusticaOrganizationalScopeService.Scope scope = organizationalScopeService.resolve(usuario, items);
        Map<Long, Map<String, Object>> securityByProcess = buildSecurityByProcess(usuario, items);
        int atrasadas = (int) items.stream().filter(this::isOverdue).count();
        int criticas = (int) items.stream().filter(item -> urgencyScore(item) >= 4).count();
        int bloqueadas = (int) items.stream().filter(item -> isSendBlocked(item, securityByProcess)).count();
        Map<String, List<String>> numerosPorRito = organizationalScopeService.processNumbersByRito(items);
        List<String> varasDisponiveis = organizationalScopeService.availableVaras(items);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "OFICIAL_OPERATIONAL_WORKBENCH_V2");
        out.put("securityLevel", "GOVERNAMENTAL_TRIPLA_CAMADA");
        List<OficialJusticaExecucaoVivaService.ExecutionState> execucoes = items.stream()
                .map(item -> execucaoVivaService.resolve(item, securityByProcess.get(item.getProcessoId())))
                .toList();
        out.put("liveQueuePath", "/api/v1/oficial-justica/diligencias/fila-viva");
        out.put("processWorkbenchPath", "/api/v1/oficial-justica/processos-nomeados/{processoId}/workbench");
        out.put("scope", organizationalScopeService.toMap(scope));
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(usuario, scope));
        out.put("summary", Map.of(
                "totalItems", items.size(),
                "atrasadas", atrasadas,
                "criticas", criticas,
                "bloqueadasParaEnvio", bloqueadas,
                "comRitoEspecial", items.stream().filter(item -> isSpecialRito(item.getProcesso())).count(),
                "comCalculoRelevante", items.stream().filter(item -> !resolveCalculatorSuggestion(item.getProcesso()).equals("SEM_RELEVANCIA_CALCULATORIA_IMEDIATA")).count(),
                "ritosCobertos", numerosPorRito.size(),
                "varasCobertas", varasDisponiveis.size()
        ));
        out.put("filtrosOrganizados", buildFilterSummary(scope, items));
        out.put("pastasOrganizadas", buildFolderSummary(items));
        out.put("numerosProcessosOrganizadosPorRito", buildRitoBuckets(items).stream().map(bucket -> Map.of(
                "rito", bucket.rito(),
                "total", bucket.total(),
                "processos", bucket.processos()
        )).toList());
        out.put("varasDisponiveis", varasDisponiveis);
        out.put("lotacoesDisponiveis", organizationalScopeService.availableLotacoes(usuario, items));
        out.put("execucaoVivaResumo", Map.of(
                "emDiligencia", execucoes.stream().filter(item -> "EM_DILIGENCIA".equals(item.statusOperacional())).count(),
                "aguardandoRetorno", execucoes.stream().filter(item -> "AGUARDANDO_RETORNO".equals(item.statusOperacional())).count(),
                "concluidas", execucoes.stream().filter(item -> "CONCLUIDA".equals(item.statusOperacional())).count(),
                "atrasadas", execucoes.stream().filter(item -> "ATRASADA".equals(item.statusOperacional())).count(),
                "pendentes", execucoes.stream().filter(item -> "PENDENTE".equals(item.statusOperacional())).count()
        ));
        out.put("retencaoAutomaticaPainel", Map.of(
                "enabled", Boolean.TRUE,
                "autoRemovedFromPanel", hygieneSnapshot.autoRemovedCount(),
                "deskDispatches", hygieneSnapshot.dispatchedToDesk(),
                "mode", "AUTO_EGRESSO_BALCAO_FORUM_TRIBUNAL"
        ));
        out.put("legendaAndamento", colorLegend());
        out.put("calculadoraJudicial", calculatorEntry());
        out.put("iaOperacional", Map.of(
                "enabled", Boolean.TRUE,
                "mode", "ASSISTENCIA_OPERACIONAL_GOVERNADA",
                "focus", List.of("normalizacao_nome", "heuristica_localidade", "triagem_alvo", "resumo_fundamentado", "padronizacao_vara", "conferencia_territorial"),
                "humanReviewAlwaysPossible", Boolean.TRUE,
                "neverWritesSilently", Boolean.TRUE
        ));
        return Collections.unmodifiableMap(out);
    }

    @Transactional(readOnly = true)
    public OficialJusticaDiligenciaQueueResponse filaViva(int limit,
                                                          String rito,
                                                          String vara,
                                                          String pasta,
                                                          String prioridade,
                                                          Boolean somentePendentes) {
        Usuario usuario = contextFactory.build().usuario();
        int safeLimit = Math.max(1, Math.min(limit, 80));
        List<WorkItem> all = loadItems(usuario, 240);
        OficialJusticaOrganizationalScopeService.Scope scope = organizationalScopeService.resolve(usuario, all);
        Map<Long, Map<String, Object>> securityByProcess = buildSecurityByProcess(usuario, all);
        List<WorkItem> filtered = all.stream()
                .filter(item -> matchesRito(item, rito))
                .filter(item -> matchesVara(item, vara))
                .filter(item -> matchesPasta(item, pasta))
                .filter(item -> matchesPrioridade(item, prioridade))
                .filter(item -> matchesOnlyPending(item, somentePendentes))
                .sorted(Comparator.comparingInt(this::urgencyScore).reversed()
                        .thenComparing(WorkItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WorkItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
        List<OficialJusticaDiligenciaQueueResponse.Row> rows = new ArrayList<>();
        for (WorkItem item : filtered) {
            rows.add(buildRow(scope, item, securityByProcess.get(item.getProcessoId()), all));
        }
        int atrasadas = (int) rows.stream().filter(row -> row.prazoFatalEm() != null && !row.prazoFatalEm().isAfter(timeService.nowUtc())).count();
        int criticas = (int) rows.stream().filter(row -> "CRITICA".equals(row.prioridadeOperacional())).count();
        int aguardandoRetorno = (int) rows.stream().filter(row -> "AGUARDANDO_RETORNO".equals(row.statusOperacional())).count();
        int cumpridas = (int) rows.stream().filter(row -> "CONCLUIDA".equals(row.statusOperacional())).count();
        int bloqueadas = (int) rows.stream().filter(row -> !row.podeEnviarNoProcesso()).count();
        int comCalculo = (int) rows.stream().filter(row -> row.calculadoraSugerida() != null && !row.calculadoraSugerida().equals("SEM_RELEVANCIA_CALCULATORIA_IMEDIATA")).count();
        int especiais = (int) rows.stream().filter(row -> row.rito() != null && row.rito().contains("ESPECIAL")).count();
        List<String> alerts = new ArrayList<>();
        if (bloqueadas > 0) {
            alerts.add("Há itens em modo somente leitura para envio dentro do processo; o lock pós-cumprimento já foi ativado nessas cadeias.");
        }
        if (atrasadas > 0) {
            alerts.add("Existem diligências fora do prazo fatal e a fila deve ser replanejada imediatamente.");
        }
        return new OficialJusticaDiligenciaQueueResponse(
                territorio(usuario),
                timeService.nowUtc(),
                toQueueScope(scope),
                new OficialJusticaDiligenciaQueueResponse.Summary(rows.size(), atrasadas, criticas, aguardandoRetorno, cumpridas, bloqueadas, comCalculo, especiais, countDistinctRitos(rows), countDistinctVaras(rows)),
                buildFilters(scope, filtered.isEmpty() ? all : filtered),
                buildFolderBuckets(filtered.isEmpty() ? all : filtered),
                buildRitoBuckets(filtered.isEmpty() ? all : filtered),
                rows,
                List.copyOf(alerts)
        );
    }

    @Transactional(readOnly = true)
    public OficialJusticaProcessoWorkbenchResponse processoWorkbench(Long processoId) {
        Usuario usuario = contextFactory.build().usuario();
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        List<WorkItem> vinculos = organizationalScopeService.filterByScope(usuario, vinculoService.vinculosDiretosProcesso(processoId, usuario.getId(), usuario.getTipoUsuario(), 120));
        if (vinculos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Processo nomeado dentro do escopo do oficial", processoId);
        }
        AuthzDecision decision = authorizationService.canReadProcesso(processo);
        WorkItem principal = vinculos.stream()
                .sorted(Comparator.comparingInt(this::urgencyScore).reversed()
                        .thenComparing(WorkItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(null);
        Map<String, Object> securityEnvelope = oficioSecurityService.envelope(processo, usuario, "OFICIAL_WORKBENCH_PROCESSO");
        List<OficialJusticaProcessoWorkbenchResponse.PendingAction> pendencias = vinculos.stream()
                .filter(this::isOpen)
                .sorted(Comparator.comparingInt(this::urgencyScore).reversed().thenComparing(WorkItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(12)
                .map(item -> new OficialJusticaProcessoWorkbenchResponse.PendingAction(
                        item.getId(),
                        item.getTitulo(),
                        classifyCategory(item),
                        classifyPriority(urgencyScore(item)),
                        item.getDueAt(),
                        nextAction(item),
                        resolveColorToken(processo, item)
                ))
                .toList();
        List<String> alerts = new ArrayList<>();
        if (!decision.allowed()) {
            alerts.add("Leitura processual não liberada pela política ativa; revisar materialização da nomeação ou credencial institucional.");
        }
        if (Boolean.TRUE.equals(securityEnvelope.get("lockedAfterCompletion"))) {
            alerts.add("Fluxo em modo somente leitura para envio dentro do processo após cumprimento integral do oficial.");
        }
        return new OficialJusticaProcessoWorkbenchResponse(
                timeService.nowUtc(),
                processoId,
                processo.getNumeroProcesso(),
                decision.allowed(),
                decision.reason(),
                contextEnvelopeService.processEnvelope(usuario, processo, principal, organizationalScopeService.resolve(usuario, vinculos), organizationalScopeService),
                new OficialJusticaProcessoWorkbenchResponse.Summary(
                        processo.getTribunal(),
                        processo.getVara(),
                        processo.getRito() != null ? processo.getRito().name() : "COMUM_ORDINARIO",
                        processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                        processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                        processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                        processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : "PUBLICO",
                        composeResumoCompleto(processo, principal),
                        principal != null ? firstNonBlank(principal.getBaseLegal(), processo.getSubmissionBlueprintStatus(), processo.getConnectorSubmissionMessage()) : firstNonBlank(processo.getSubmissionBlueprintStatus(), processo.getConnectorSubmissionMessage()),
                        resolveTargetName(processo, principal),
                        resolveLocalidadeAlvo(processo, principal),
                        (int) vinculos.stream().filter(this::isOpen).count(),
                        vinculos.stream().anyMatch(item -> urgencyScore(item) >= 4),
                        !Boolean.TRUE.equals(securityEnvelope.get("sendIntoProcessAllowed"))
                ),
                securityEnvelope,
                buildProcessFolderBuckets(processoId, vinculos),
                colorLegend(),
                buildCalculatorSuggestions(processo),
                buildAiAssist(processo, principal),
                pendencias,
                List.copyOf(alerts)
        );
    }

    private List<WorkItem> loadItems(Usuario usuario, int limit) {
        List<WorkItem> scoped = organizationalScopeService.filterByScope(usuario, vinculoService.vinculosDiretosUsuario(usuario.getId(), usuario.getTipoUsuario(), limit));
        return panelEgressService.reconcileVisibility(usuario, scoped).visibleItems();
    }

    private Map<Long, Map<String, Object>> buildSecurityByProcess(Usuario usuario, List<WorkItem> items) {
        LinkedHashMap<Long, Map<String, Object>> out = new LinkedHashMap<>();
        items.stream().map(WorkItem::getProcesso).filter(Objects::nonNull).forEach(processo -> out.computeIfAbsent(processo.getId(), ignored -> oficioSecurityService.envelope(processo, usuario, "OFICIAL_FILA_VIVA")));
        return out;
    }

    private OficialJusticaDiligenciaQueueResponse.Row buildRow(OficialJusticaOrganizationalScopeService.Scope scope,
                                                               WorkItem item,
                                                               Map<String, Object> securityEnvelope,
                                                               List<WorkItem> universe) {
        Processo processo = item.getProcesso();
        int urgency = urgencyScore(item);
        OficialJusticaExecucaoVivaService.ExecutionState execucao = execucaoVivaService.resolve(item, securityEnvelope);
        String pasta = firstNonBlank(execucao.folderCode(), classifyFolder(item));
        String calculator = resolveCalculatorSuggestion(processo);
        List<String> alerts = new ArrayList<>(execucao.alerts());
        if (isOverdue(item)) {
            alerts.add("PRAZO_FATAL_EXPIRADO");
        }
        if (!Boolean.TRUE.equals(securityEnvelope == null ? Boolean.FALSE : securityEnvelope.get("sendIntoProcessAllowed"))) {
            alerts.add(String.valueOf(securityEnvelope == null ? "lock_inexistente" : securityEnvelope.get("blockedReason")));
        }
        return new OficialJusticaDiligenciaQueueResponse.Row(
                item.getId(),
                item.getProcessoId(),
                processo != null ? processo.getNumeroProcesso() : null,
                processo != null ? processo.getTribunal() : null,
                organizationalScopeService.resolveVaraDisplay(processo, item),
                organizationalScopeService.resolveLotacaoLabel(scope, processo, item),
                processo != null && processo.getRito() != null ? processo.getRito().name() : "COMUM_ORDINARIO",
                processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                firstNonBlank(execucao.colorToken(), resolveColorToken(processo, item)),
                pasta,
                classifyCategory(item),
                classifyPriority(urgency),
                execucao.statusOperacional(),
                execucao.statusLabel(),
                execucao.colorToken(),
                item.getDueAt(),
                execucao.lastEventAt(),
                Math.max(execucao.attempts(), inferAttempts(item, universe)),
                firstNonBlankInstant(execucao.returnWindow(), inferReturnWindow(item)),
                Boolean.TRUE.equals(securityEnvelope == null ? Boolean.FALSE : securityEnvelope.get("sendIntoProcessAllowed")),
                securityEnvelope == null ? null : String.valueOf(securityEnvelope.get("blockedReason")),
                resolveTargetName(processo, item),
                processo != null ? firstNonBlank(processo.getComarca(), item.getComarca()) : item.getComarca(),
                composeResumoCurto(processo),
                firstNonBlank(item.getBaseLegal(), processo != null ? processo.getSubmissionBlueprintStatus() : null, processo != null ? processo.getConnectorSubmissionMessage() : null),
                calculator,
                execucao.toMap(),
                contextEnvelopeService.processEnvelope(contextFactory.build().usuario(), processo, item, scope, organizationalScopeService),
                List.copyOf(alerts.stream().filter(Objects::nonNull).distinct().toList()),
                quickActions(item)
        );
    }

    private Instant firstNonBlankInstant(Instant primary, Instant fallback) {
        return primary != null ? primary : fallback;
    }

    private Map<String, Object> quickActions(WorkItem item) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (item != null && item.getProcessoId() != null) {
            out.put("acessoProcessoPath", "/api/v1/oficial-justica/processos-nomeados/" + item.getProcessoId() + "/acesso");
            out.put("workbenchPath", "/api/v1/oficial-justica/processos-nomeados/" + item.getProcessoId() + "/workbench");
            out.put("localizadorPath", "/api/v1/oficial-justica/localizador/processos/" + item.getProcessoId() + "/alvos/PASSIVO/rastreio");
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildFilterSummary(OficialJusticaOrganizationalScopeService.Scope scope, List<WorkItem> items) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("ritos", items.stream().map(item -> item.getProcesso() != null && item.getProcesso().getRito() != null ? item.getProcesso().getRito().name() : "COMUM_ORDINARIO").distinct().sorted().toList());
        out.put("ramos", items.stream().map(item -> item.getProcesso() != null && item.getProcesso().getRamoDireito() != null ? item.getProcesso().getRamoDireito().name() : "NAO_CLASSIFICADO").distinct().sorted().toList());
        out.put("fases", items.stream().map(item -> item.getProcesso() != null && item.getProcesso().getFaseAtual() != null ? item.getProcesso().getFaseAtual().name() : "NAO_CLASSIFICADA").distinct().sorted().toList());
        out.put("status", items.stream().map(item -> item.getProcesso() != null && item.getProcesso().getStatusProcesso() != null ? item.getProcesso().getStatusProcesso().name() : "NAO_CLASSIFICADO").distinct().sorted().toList());
        out.put("varas", organizationalScopeService.availableVaras(items));
        out.put("lotacao", organizationalScopeService.toMap(scope));
        out.put("pastas", items.stream().map(this::classifyFolder).distinct().sorted().toList());
        return Collections.unmodifiableMap(out);
    }

    private List<OficialJusticaDiligenciaQueueResponse.FilterGroup> buildFilters(OficialJusticaOrganizationalScopeService.Scope scope, List<WorkItem> items) {
        return List.of(
                new OficialJusticaDiligenciaQueueResponse.FilterGroup("rito", "Rito processual", items.stream().map(item -> item.getProcesso() != null && item.getProcesso().getRito() != null ? item.getProcesso().getRito().name() : "COMUM_ORDINARIO").distinct().sorted().toList()),
                new OficialJusticaDiligenciaQueueResponse.FilterGroup("vara", "Vara / lotação", organizationalScopeService.availableVaras(items)),
                new OficialJusticaDiligenciaQueueResponse.FilterGroup("ramo", "Ramo do direito", items.stream().map(item -> item.getProcesso() != null && item.getProcesso().getRamoDireito() != null ? item.getProcesso().getRamoDireito().name() : "NAO_CLASSIFICADO").distinct().sorted().toList()),
                new OficialJusticaDiligenciaQueueResponse.FilterGroup("fase", "Fase processual", items.stream().map(item -> item.getProcesso() != null && item.getProcesso().getFaseAtual() != null ? item.getProcesso().getFaseAtual().name() : "NAO_CLASSIFICADA").distinct().sorted().toList()),
                new OficialJusticaDiligenciaQueueResponse.FilterGroup("pasta", "Pasta operacional", items.stream().map(this::classifyFolder).distinct().sorted().toList()),
                new OficialJusticaDiligenciaQueueResponse.FilterGroup("prioridade", "Prioridade", items.stream().map(item -> classifyPriority(urgencyScore(item))).distinct().sorted().toList()),
                new OficialJusticaDiligenciaQueueResponse.FilterGroup("lotacao_ativa", "Recorte institucional", List.of(scope.label()))
        );
    }

    private List<OficialJusticaDiligenciaQueueResponse.RitoBucket> buildRitoBuckets(List<WorkItem> items) {
        return organizationalScopeService.processNumbersByRito(items).entrySet().stream()
                .map(entry -> new OficialJusticaDiligenciaQueueResponse.RitoBucket(entry.getKey(), entry.getValue().size(), entry.getValue()))
                .toList();
    }

    private OficialJusticaDiligenciaQueueResponse.Scope toQueueScope(OficialJusticaOrganizationalScopeService.Scope scope) {
        return new OficialJusticaDiligenciaQueueResponse.Scope(scope.mode(), scope.label(), scope.institutionManaged(), scope.cobreTodasAsVaras(), scope.varas().stream().map(this::humanizeScopeLabel).toList(), scope.unidades());
    }

    private int countDistinctRitos(List<OficialJusticaDiligenciaQueueResponse.Row> rows) {
        return (int) rows.stream().map(OficialJusticaDiligenciaQueueResponse.Row::rito).filter(Objects::nonNull).distinct().count();
    }

    private int countDistinctVaras(List<OficialJusticaDiligenciaQueueResponse.Row> rows) {
        return (int) rows.stream().map(OficialJusticaDiligenciaQueueResponse.Row::vara).filter(Objects::nonNull).distinct().count();
    }

    private List<Map<String, Object>> buildFolderSummary(List<WorkItem> items) {
        return buildFolderBuckets(items).stream()
                .map(bucket -> {
                    LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                    out.put("code", bucket.code());
                    out.put("label", bucket.label());
                    out.put("count", bucket.count());
                    out.put("colorToken", bucket.colorToken());
                    return Collections.unmodifiableMap(out);
                })
                .toList();
    }

    private List<OficialJusticaDiligenciaQueueResponse.FolderBucket> buildFolderBuckets(List<WorkItem> items) {
        LinkedHashMap<String, Long> counts = items.stream().collect(Collectors.groupingBy(this::classifyFolder, LinkedHashMap::new, Collectors.counting()));
        return List.of(
                folderBucket("ATRASADAS", "Atrasadas", counts),
                folderBucket("CUMPRIR_HOJE", "Cumprir hoje", counts),
                folderBucket("AGUARDANDO_RETORNO", "Aguardando retorno", counts),
                folderBucket("OFICIOS_NO_PROCESSO", "Ofícios no processo", counts),
                folderBucket("AGUARDANDO_CARTORIO", "Aguardando cartório", counts),
                folderBucket("CONCLUIDAS", "Concluídas", counts)
        );
    }

    private OficialJusticaDiligenciaQueueResponse.FolderBucket folderBucket(String code, String label, Map<String, Long> counts) {
        return new OficialJusticaDiligenciaQueueResponse.FolderBucket(code, label, counts.getOrDefault(code, 0L).intValue(), switch (code) {
            case "ATRASADAS" -> "VERMELHO";
            case "CUMPRIR_HOJE" -> "AMARELO";
            case "AGUARDANDO_RETORNO" -> "LARANJA";
            case "OFICIOS_NO_PROCESSO" -> "AZUL";
            case "AGUARDANDO_CARTORIO" -> "ROXO";
            default -> "VERDE";
        });
    }

    private List<OficialJusticaProcessoWorkbenchResponse.FolderBucket> buildProcessFolderBuckets(Long processoId, List<WorkItem> vinculos) {
        Map<String, Long> counts = vinculos.stream().collect(Collectors.groupingBy(this::classifyFolder, LinkedHashMap::new, Collectors.counting()));
        return List.of(
                new OficialJusticaProcessoWorkbenchResponse.FolderBucket("PROCESSO_ATIVO", "Visão do processo", vinculos.size(), "AZUL", "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/acesso"),
                new OficialJusticaProcessoWorkbenchResponse.FolderBucket("PENDENCIAS", "Pendências", counts.getOrDefault("CUMPRIR_HOJE", 0L).intValue() + counts.getOrDefault("ATRASADAS", 0L).intValue(), "AMARELO", "/api/v1/oficial-justica/pendencias-operacionais"),
                new OficialJusticaProcessoWorkbenchResponse.FolderBucket("OFICIOS", "Ofícios", counts.getOrDefault("OFICIOS_NO_PROCESSO", 0L).intValue(), "ROXO", "/api/v1/oficial-justica/oficios/execucoes"),
                new OficialJusticaProcessoWorkbenchResponse.FolderBucket("RASTREIO", "Rastreio", 1, "CINZA_AZULADO", "/api/v1/oficial-justica/localizador/processos/" + processoId + "/alvos/PASSIVO/rastreio"),
                new OficialJusticaProcessoWorkbenchResponse.FolderBucket("CONCLUIDAS", "Concluídas", counts.getOrDefault("CONCLUIDAS", 0L).intValue(), "VERDE", "/api/v1/oficial-justica/processos-nomeados")
        );
    }

    private Map<String, Object> buildCalculatorSuggestions(Processo processo) {
        String domain = resolveCalculatorSuggestion(processo);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("suggestedDomain", domain);
        out.put("workspacePath", "/api/v1/processual/calculos/workspace");
        out.put("catalogPath", "/api/v1/processual/calculos/catalogo/frontend");
        out.put("iaFinanceiraPath", "/api/v1/processual/calculos/ia-financeira/command");
        out.put("justification", calculatorJustification(processo, domain));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> calculatorEntry() {
        return Map.of(
                "enabled", Boolean.TRUE,
                "workspacePath", "/api/v1/processual/calculos/workspace",
                "catalogPath", "/api/v1/processual/calculos/catalogo/frontend",
                "domains", List.of("TRABALHISTA_CLT", "FAZENDA_TRIBUTARIO", "CUSTAS_PROCESSUAIS", "FEDERAL_PREVIDENCIARIO_CJF")
        );
    }

    private Map<String, Object> buildAiAssist(Processo processo, WorkItem principal) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "ASSISTENCIA_OPERACIONAL_GOVERNADA");
        out.put("nomeNormalizado", normalizeHumanName(resolveTargetName(processo, principal)));
        out.put("localidadeBase", resolveLocalidadeAlvo(processo, principal));
        out.put("confidence", inferConfidence(processo, principal));
        out.put("strategy", List.of(
                "normalizar_nome_com_alias_controlado",
                "priorizar_endereco_por_cpf_estruturado_e_territorio",
                "exigir_revisao_humana_quando_houver_homonimia_ou_baixa_confianca",
                "nunca_publicar_dados_sensiveis_sem_contexto_formal"
        ));
        out.put("nextAction", principal == null ? "revisar_vinculo_operacional" : nextAction(principal));
        if (processo != null && processo.getId() != null) {
            out.put("rastreioSugeridoPath", "/api/v1/oficial-justica/localizador/processos/" + processo.getId() + "/alvos/PASSIVO/rastreio");
        }
        try {
            if (processo != null && processo.getId() != null) {
                OficialJusticaPessoaRastreioResponse rastreio = enderecoTriageService.rastrearProcessoAlvo(processo.getId(), "PASSIVO", false, true, false);
                out.put("target", targetMap(
                        rastreio.target() != null ? rastreio.target().polo() : "PASSIVO",
                        rastreio.target() != null ? rastreio.target().nome() : resolveTargetName(processo, principal),
                        rastreio.target() != null ? rastreio.target().cpfMascarado() : null
                ));
                if (rastreio.alerts() != null && !rastreio.alerts().isEmpty()) {
                    out.put("rastreioAlerts", rastreio.alerts());
                }
            }
        } catch (Exception ignored) {
            out.put("target", targetMap("PASSIVO", resolveTargetName(processo, principal), null));
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> targetMap(String polo, String nome, String cpfMascarado) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("polo", polo == null ? "PASSIVO" : polo);
        out.put("nome", nome == null ? "ALVO_NAO_IDENTIFICADO" : nome);
        if (cpfMascarado != null && !cpfMascarado.isBlank()) {
            out.put("cpfMascarado", cpfMascarado);
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> colorLegend() {
        return Map.of(
                "VERMELHO", "prazo vencido ou urgência máxima",
                "AMARELO", "cumprimento dentro da janela do dia",
                "LARANJA", "retorno recomendado ou tentativa frustrada",
                "AZUL", "movimento regular do processo e ofícios internos",
                "ROXO", "dependência cartorária, juntada ou expedição complementar",
                "VERDE", "ato concluído ou baixa operacional",
                "CINZA_AZULADO", "apenas leitura, rastreio ou conferência"
        );
    }

    private boolean matchesRito(WorkItem item, String rito) {
        if (rito == null || rito.isBlank() || "TODOS".equalsIgnoreCase(rito)) {
            return true;
        }
        String actual = item.getProcesso() != null && item.getProcesso().getRito() != null ? item.getProcesso().getRito().name() : "COMUM_ORDINARIO";
        return actual.equalsIgnoreCase(rito.trim());
    }

    private boolean matchesVara(WorkItem item, String vara) {
        if (vara == null || vara.isBlank() || "TODAS".equalsIgnoreCase(vara)) {
            return true;
        }
        String actual = organizationalScopeService.resolveVaraDisplay(item.getProcesso(), item);
        return actual != null && actual.equalsIgnoreCase(vara.trim());
    }

    private boolean matchesPasta(WorkItem item, String pasta) {
        if (pasta == null || pasta.isBlank() || "TODAS".equalsIgnoreCase(pasta)) {
            return true;
        }
        return classifyFolder(item).equalsIgnoreCase(pasta.trim());
    }

    private boolean matchesPrioridade(WorkItem item, String prioridade) {
        if (prioridade == null || prioridade.isBlank() || "TODAS".equalsIgnoreCase(prioridade)) {
            return true;
        }
        return classifyPriority(urgencyScore(item)).equalsIgnoreCase(prioridade.trim());
    }

    private boolean matchesOnlyPending(WorkItem item, Boolean onlyPending) {
        return !Boolean.TRUE.equals(onlyPending) || isOpen(item);
    }

    private boolean isSendBlocked(WorkItem item, Map<Long, Map<String, Object>> securityByProcess) {
        Map<String, Object> envelope = securityByProcess.get(item.getProcessoId());
        return !Boolean.TRUE.equals(envelope == null ? Boolean.FALSE : envelope.get("sendIntoProcessAllowed"));
    }

    private int urgencyScore(WorkItem item) {
        int score = 1;
        if (item == null) {
            return score;
        }
        if (isOverdue(item)) {
            score += 3;
        }
        if (item.getPrioridade() != null) {
            score += Math.max(0, 5 - Math.min(item.getPrioridade(), 5));
        }
        if (commons.titleContains(item, "CITACAO", "INTIMACAO", "MANDADO", "PENHORA", "BUSCA")) {
            score += 1;
        }
        return Math.min(score, 5);
    }

    private boolean isOverdue(WorkItem item) {
        return item != null && item.getDueAt() != null && item.getDueAt().isBefore(timeService.nowUtc()) && isOpen(item);
    }

    private boolean isOpen(WorkItem item) {
        return item != null && (item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO);
    }

    private String classifyPriority(int urgency) {
        return switch (urgency) {
            case 5 -> "CRITICA";
            case 4 -> "ALTA";
            case 3 -> "MEDIA";
            default -> "ORDINARIA";
        };
    }

    private String classifyFolder(WorkItem item) {
        if (item == null) {
            return "CUMPRIR_HOJE";
        }
        if (item.getStatus() == WorkItemStatus.CONCLUIDO || item.getStatus() == WorkItemStatus.CANCELADO) {
            return "CONCLUIDAS";
        }
        if (isOverdue(item)) {
            return "ATRASADAS";
        }
        if (commons.titleContains(item, "RETORNO", "FRUSTRADO", "NEGATIVO")) {
            return "AGUARDANDO_RETORNO";
        }
        if (commons.titleContains(item, "OFICIO", "RESPOSTA A OFICIO")) {
            return "OFICIOS_NO_PROCESSO";
        }
        if (commons.titleContains(item, "JUNTADA", "RECEBER E JUNTAR", "CARTORIO")) {
            return "AGUARDANDO_CARTORIO";
        }
        return "CUMPRIR_HOJE";
    }

    private String classifyCategory(WorkItem item) {
        if (item == null) {
            return "DILIGENCIA";
        }
        if (commons.titleContains(item, "CITACAO")) {
            return "CITACAO";
        }
        if (commons.titleContains(item, "INTIMACAO")) {
            return "INTIMACAO";
        }
        if (commons.titleContains(item, "PENHORA", "AVALIACAO")) {
            return "PENHORA_AVALIACAO";
        }
        if (commons.titleContains(item, "OFICIO")) {
            return "OFICIO";
        }
        return "MANDADO_DILIGENCIA";
    }

    private Instant inferReturnWindow(WorkItem item) {
        if (item == null || item.getDueAt() == null) {
            return null;
        }
        if (commons.titleContains(item, "FRUSTRADO", "NEGATIVO", "RETORNO")) {
            return item.getDueAt().plus(1, ChronoUnit.DAYS);
        }
        if (commons.titleContains(item, "PENHORA", "AVALIACAO")) {
            return item.getDueAt().plus(12, ChronoUnit.HOURS);
        }
        return item.getDueAt().plus(6, ChronoUnit.HOURS);
    }

    private int inferAttempts(WorkItem item, List<WorkItem> universe) {
        if (item == null || item.getProcessoId() == null) {
            return 0;
        }
        long attempts = universe.stream()
                .filter(other -> !Objects.equals(other.getId(), item.getId()))
                .filter(other -> Objects.equals(other.getProcessoId(), item.getProcessoId()))
                .filter(other -> commons.titleContains(other, "CERTIDAO", "NEGATIVO", "FRUSTRADO", "CUMPRIDO", "RETORNO"))
                .count();
        return Math.toIntExact(Math.min(attempts, Integer.MAX_VALUE));
    }

    private String resolveColorToken(Processo processo, WorkItem item) {
        if (item != null && (item.getStatus() == WorkItemStatus.CONCLUIDO || item.getStatus() == WorkItemStatus.CANCELADO)) {
            return "VERDE";
        }
        if (isOverdue(item)) {
            return "VERMELHO";
        }
        if (commons.titleContains(item, "RETORNO", "FRUSTRADO", "NEGATIVO")) {
            return "LARANJA";
        }
        if (commons.titleContains(item, "OFICIO", "JUNTADA")) {
            return "ROXO";
        }
        StatusProcesso status = processo == null ? null : processo.getStatusProcesso();
        if (status == StatusProcesso.CONCLUSO || status == StatusProcesso.AUDIENCIA_DESIGNADA || status == StatusProcesso.EM_ANDAMENTO) {
            return "AZUL";
        }
        return "AMARELO";
    }

    private String composeResumoCurto(Processo processo) {
        if (processo == null) {
            return "Sem resumo processual disponível.";
        }
        return firstNonBlank(trim(processo.getResumoIA(), 220), trim(processo.getObjetoProcessual(), 220), trim(processo.getPedidoPrincipal(), 220), trim(processo.getAssunto(), 220), "Sem resumo processual consolidado.");
    }

    private String composeResumoCompleto(Processo processo, WorkItem principal) {
        List<String> parts = new ArrayList<>();
        add(parts, processo != null ? processo.getAssunto() : null);
        add(parts, processo != null ? processo.getResumoIA() : null);
        add(parts, processo != null ? processo.getPedidoPrincipal() : null);
        add(parts, processo != null ? processo.getObjetoProcessual() : null);
        add(parts, principal != null ? principal.getTitulo() : null);
        add(parts, principal != null ? principal.getDescricao() : null);
        String joined = String.join(" ", parts).trim();
        return joined.isBlank() ? "Resumo processual ainda não consolidado para o Oficial de Justiça." : trim(joined, 1800);
    }

    private String resolveTargetName(Processo processo, WorkItem item) {
        boolean ativo = item != null && commons.titleContains(item, "AUTOR", "REQUERENTE", "EXEQUENTE");
        return ativo
                ? firstNonBlank(processo != null ? processo.getParteAutoraNome() : null, processo != null ? processo.getParteReuNome() : null, "ALVO_NAO_IDENTIFICADO")
                : firstNonBlank(processo != null ? processo.getParteReuNome() : null, processo != null ? processo.getParteAutoraNome() : null, "ALVO_NAO_IDENTIFICADO");
    }

    private String resolveLocalidadeAlvo(Processo processo, WorkItem item) {
        return firstNonBlank(
                processo != null ? processo.getComarca() : null,
                item != null ? item.getComarca() : null,
                processo != null ? processo.getUf() : null,
                "LOCALIDADE_NAO_INFORMADA"
        );
    }

    private String normalizeHumanName(String value) {
        if (value == null || value.isBlank()) {
            return "ALVO_NAO_IDENTIFICADO";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.toLowerCase(Locale.ROOT).split(" ");
        return Arrays.stream(parts)
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String inferConfidence(Processo processo, WorkItem principal) {
        int score = 0;
        if (processo != null && processo.getParteReuCpf() != null && !processo.getParteReuCpf().isBlank()) {
            score += 2;
        }
        if (processo != null && processo.getComarca() != null && !processo.getComarca().isBlank()) {
            score += 1;
        }
        if (principal != null && principal.getDueAt() != null) {
            score += 1;
        }
        return score >= 4 ? "ALTA" : score >= 2 ? "MEDIA" : "BAIXA";
    }

    private String nextAction(WorkItem item) {
        if (item == null) {
            return "revisar_fila";
        }
        if (commons.titleContains(item, "OFICIO")) {
            return "revisar_entrega_e_confirmacao";
        }
        if (commons.titleContains(item, "PENHORA", "AVALIACAO")) {
            return "validar_calculo_e_avaliacao";
        }
        if (commons.titleContains(item, "CITACAO", "INTIMACAO")) {
            return "planejar_saida_para_cumprimento";
        }
        return "executar_diligencia_ou_certificar";
    }

    private String resolveCalculatorSuggestion(Processo processo) {
        RitoProcessual rito = processo == null ? null : processo.getRito();
        if (rito == null) {
            return "SEM_RELEVANCIA_CALCULATORIA_IMEDIATA";
        }
        if (rito.isTrabalhista()) {
            return "TRABALHISTA_CLT";
        }
        if (rito.isTribFazenda() || rito.isExecucaoFiscalEstrita()) {
            return "FAZENDA_TRIBUTARIO";
        }
        if (rito.isPrevidenciario()) {
            return "FEDERAL_PREVIDENCIARIO_CJF";
        }
        if (rito.name().contains("EXECUCAO") || rito.name().contains("CUMPRIMENTO")) {
            return "CUSTAS_PROCESSUAIS";
        }
        return "SEM_RELEVANCIA_CALCULATORIA_IMEDIATA";
    }

    private String calculatorJustification(Processo processo, String domain) {
        if (processo == null) {
            return "Sem processo associado para recomendar calculadora específica.";
        }
        return switch (domain) {
            case "TRABALHISTA_CLT" -> "O rito trabalhista costuma exigir conferência de verbas, execução e atualização em trilha auditável.";
            case "FAZENDA_TRIBUTARIO" -> "O processo fazendário/tributário pode exigir juros, correção e memória oficial para atos executivos.";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "O caso previdenciário pode demandar apoio em atrasados e referência econômica oficial.";
            case "CUSTAS_PROCESSUAIS" -> "A fase executiva ou de cumprimento pode demandar custas, despesas e cálculo acessório.";
            default -> "Nenhuma memória de cálculo foi classificada como prioritária para o contexto atual do oficial.";
        };
    }

    private boolean isSpecialRito(Processo processo) {
        RitoProcessual rito = processo == null ? null : processo.getRito();
        return rito != null && (rito.isJuizado() || rito.isEspecialConstitucional() || rito.isEleitoral() || rito.isMilitar());
    }

    private String humanizeScopeLabel(String scopeLabel) {
        if (scopeLabel == null || scopeLabel.isBlank()) {
            return scopeLabel;
        }
        String[] parts = scopeLabel.toLowerCase(Locale.ROOT).split(" ");
        return Arrays.stream(parts)
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String territorio(Usuario usuario) {
        return firstNonBlank(usuario.getUf(), "UF") + ':' + firstNonBlank(usuario.getComarca(), "COMARCA");
    }

    private static String firstNonBlank(String... values) {
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

    private static void add(List<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private static String trim(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= max ? normalized : normalized.substring(0, Math.max(0, max - 1)) + '…';
    }
}
