package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPendenciaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoAcessoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoNomeadoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaPortfolioProcessualService {

    private static final List<String> PENDENCIA_COLUMNS = List.of(
            "workItemId",
            "processoNumero",
            "tribunal",
            "vara",
            "lotacao",
            "rito",
            "tipoPendencia",
            "prioridadeOperacional",
            "prazoFatalEm",
            "acessoProcessoPermitido",
            "fundamentoAcesso",
            "proximaAcao"
    );

    private static final List<String> PROCESSO_COLUMNS = List.of(
            "processoId",
            "processoNumero",
            "tribunal",
            "vara",
            "lotacao",
            "rito",
            "baseNomeacao",
            "acessoProcessoPermitido",
            "fundamentoAcesso",
            "workItemVinculoId",
            "statusVinculo",
            "possuiPendenciaAtiva",
            "proximaAcao"
    );

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final PjbAuthorizationService authorizationService;
    private final OficialJusticaProcessoVinculoService vinculoService;
    private final PjbTimeService timeService;
    private final OficialJusticaOrganizationalScopeService organizationalScopeService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final OficialJusticaPanelEgressService panelEgressService;
    private final OficialJusticaCommunicationFormalModelService communicationFormalModelService;

    public OficialJusticaPortfolioProcessualService(PerfilDashboardContextFactory contextFactory,
                                                    PainelServiceCommons commons,
                                                    PjbAuthorizationService authorizationService,
                                                    OficialJusticaProcessoVinculoService vinculoService,
                                                    PjbTimeService timeService,
                                                    OficialJusticaOrganizationalScopeService organizationalScopeService,
                                                    OficialJusticaContextEnvelopeService contextEnvelopeService,
                                                    OficialJusticaPanelEgressService panelEgressService,
                                                    OficialJusticaCommunicationFormalModelService communicationFormalModelService) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.commons = Objects.requireNonNull(commons);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.vinculoService = Objects.requireNonNull(vinculoService);
        this.timeService = Objects.requireNonNull(timeService);
        this.organizationalScopeService = Objects.requireNonNull(organizationalScopeService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.panelEgressService = Objects.requireNonNull(panelEgressService);
        this.communicationFormalModelService = Objects.requireNonNull(communicationFormalModelService);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painelResumoPendencias() {
        Usuario usuario = contextFactory.build().usuario();
        List<WorkItem> items = pendenciasBase(usuario, 80);
        OficialJusticaOrganizationalScopeService.Scope scope = organizationalScopeService.resolve(usuario, items);
        long atrasadas = items.stream().filter(this::isOverdue).count();
        long criticas = items.stream().filter(item -> urgencyRank(item) >= 4).count();
        long aguardandoRastreio = items.stream().filter(item -> classifyPending(item).equals("RASTREIO_PREVIO")).count();
        long aguardandoJuntada = items.stream().filter(item -> classifyPending(item).equals("JUNTADA_CARTORARIA")).count();
        long aguardandoConfirmacao = items.stream().filter(item -> classifyPending(item).equals("CONFIRMACAO_EXTERNA")).count();
        long comAcesso = items.stream().filter(item -> hasReadAccess(item.getProcesso()).allowed()).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "PLANILHA_PENDENCIAS_OPERACIONAIS_V2");
        out.put("activationPath", "/api/v1/oficial-justica/pendencias-operacionais?limit=20&rito=TODOS&vara=TODAS&somentePendentes=true");
        out.put("scope", organizationalScopeService.toMap(scope));
        out.put("totalPendencias", items.size());
        out.put("atrasadas", atrasadas);
        out.put("criticas", criticas);
        out.put("aguardandoRastreio", aguardandoRastreio);
        out.put("aguardandoJuntada", aguardandoJuntada);
        out.put("aguardandoConfirmacaoExterna", aguardandoConfirmacao);
        out.put("comAcessoProcessual", comAcesso);
        out.put("semAcessoProcessual", Math.max(0L, items.size() - comAcesso));
        out.put("dependsOnProcessProgress", Boolean.TRUE);
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(usuario, scope));
        out.put("columns", PENDENCIA_COLUMNS);
        out.put("filtros", List.of(
                Map.of("key", "rito", "values", organizationalScopeService.availableRitos(items)),
                Map.of("key", "vara", "values", organizationalScopeService.availableVaras(items)),
                Map.of("key", "lotacao", "values", organizationalScopeService.availableLotacoes(usuario, items))
        ));
        out.put("numerosProcessosOrganizadosPorRito", organizationalScopeService.processNumbersByRito(items));
        return Collections.unmodifiableMap(out);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painelResumoProcessosNomeados() {
        Usuario usuario = contextFactory.build().usuario();
        List<WorkItem> vinculos = vinculosBase(usuario, 160);
        OficialJusticaOrganizationalScopeService.Scope scope = organizationalScopeService.resolve(usuario, vinculos);
        Map<Long, List<WorkItem>> grouped = groupedByProcess(vinculos);
        long comSigilo = grouped.values().stream().map(list -> list.getFirst().getProcesso()).filter(Objects::nonNull).filter(this::sigiloso).count();
        long comPendenciaAtiva = grouped.values().stream().filter(list -> list.stream().anyMatch(this::isOpen)).count();
        long acessoLiberado = grouped.values().stream().map(list -> list.getFirst().getProcesso()).filter(Objects::nonNull).filter(processo -> hasReadAccess(processo).allowed()).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "PORTFOLIO_PROCESSOS_NOMEADOS_V2");
        out.put("activationPath", "/api/v1/oficial-justica/processos-nomeados?limit=20&rito=TODOS&vara=TODAS&somentePendentes=false");
        out.put("singleAccessPath", "/api/v1/oficial-justica/processos-nomeados/{processoId}/acesso");
        out.put("scope", organizationalScopeService.toMap(scope));
        out.put("totalProcessos", grouped.size());
        out.put("comSigilo", comSigilo);
        out.put("comPendenciaAtiva", comPendenciaAtiva);
        out.put("comAcessoLiberado", acessoLiberado);
        out.put("comAcessoRestrito", Math.max(0L, grouped.size() - acessoLiberado));
        out.put("grantsReadWhenNamed", Boolean.TRUE);
        out.put("columns", PROCESSO_COLUMNS);
        out.put("filtros", List.of(
                Map.of("key", "rito", "values", organizationalScopeService.availableRitos(vinculos)),
                Map.of("key", "vara", "values", organizationalScopeService.availableVaras(vinculos)),
                Map.of("key", "lotacao", "values", organizationalScopeService.availableLotacoes(usuario, vinculos))
        ));
        out.put("numerosProcessosOrganizadosPorRito", organizationalScopeService.processNumbersByRito(vinculos));
        return Collections.unmodifiableMap(out);
    }

    @Transactional(readOnly = true)
    public OficialJusticaPendenciaOperacionalResponse pendencias(int limit, String rito, String vara, Boolean somentePendentes) {
        Usuario usuario = contextFactory.build().usuario();
        int safeLimit = Math.max(1, Math.min(limit, 30));
        List<WorkItem> base = pendenciasBase(usuario, Math.max(40, safeLimit * 4));
        OficialJusticaOrganizationalScopeService.Scope scope = organizationalScopeService.resolve(usuario, base);
        List<WorkItem> items = base.stream()
                .filter(item -> matchesRito(item, rito))
                .filter(item -> matchesVara(item, vara))
                .filter(item -> !Boolean.TRUE.equals(somentePendentes) || isOpen(item))
                .sorted(Comparator.comparingInt(this::urgencyRank).reversed().thenComparing(WorkItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(safeLimit)
                .toList();
        List<String> alerts = new ArrayList<>();
        List<OficialJusticaPendenciaOperacionalResponse.PendenciaRow> rows = items.stream().map(item -> buildPendenciaRow(scope, item)).toList();
        if (rows.stream().anyMatch(row -> !row.acessoProcessoPermitido())) {
            alerts.add("Há pendências operacionais sem leitura processual liberada automaticamente; conferir se a nomeação/vínculo já foi materializado no fluxo do oficial.");
        }
        int atrasadas = (int) rows.stream().filter(row -> row.prazoFatalEm() != null && !row.prazoFatalEm().isAfter(timeService.nowUtc())).count();
        int criticas = (int) rows.stream().filter(row -> "CRITICA".equals(row.prioridadeOperacional())).count();
        int aguardandoRastreio = (int) rows.stream().filter(row -> "RASTREIO_PREVIO".equals(row.tipoPendencia())).count();
        int aguardandoJuntada = (int) rows.stream().filter(row -> "JUNTADA_CARTORARIA".equals(row.tipoPendencia())).count();
        int aguardandoConfirmacao = (int) rows.stream().filter(row -> "CONFIRMACAO_EXTERNA".equals(row.tipoPendencia())).count();
        int comAcesso = (int) rows.stream().filter(OficialJusticaPendenciaOperacionalResponse.PendenciaRow::acessoProcessoPermitido).count();
        return new OficialJusticaPendenciaOperacionalResponse(
                composeTerritorio(usuario),
                timeService.nowUtc(),
                toPendenciaScope(scope),
                new OficialJusticaPendenciaOperacionalResponse.Summary(
                        rows.size(),
                        atrasadas,
                        criticas,
                        aguardandoRastreio,
                        aguardandoJuntada,
                        aguardandoConfirmacao,
                        comAcesso,
                        Math.max(0, rows.size() - comAcesso),
                        countDistinct(rows.stream().map(OficialJusticaPendenciaOperacionalResponse.PendenciaRow::rito).toList()),
                        countDistinct(rows.stream().map(OficialJusticaPendenciaOperacionalResponse.PendenciaRow::vara).toList())
                ),
                PENDENCIA_COLUMNS,
                buildPendenciaFilters(scope, base),
                rows,
                List.copyOf(alerts)
        );
    }

    @Transactional(readOnly = true)
    public OficialJusticaProcessoNomeadoResponse processosNomeados(int limit, String rito, String vara, Boolean somentePendentes) {
        Usuario usuario = contextFactory.build().usuario();
        int safeLimit = Math.max(1, Math.min(limit, 40));
        List<WorkItem> vinculos = vinculosBase(usuario, Math.max(80, safeLimit * 4));
        OficialJusticaOrganizationalScopeService.Scope scope = organizationalScopeService.resolve(usuario, vinculos);
        Map<Long, List<WorkItem>> grouped = groupedByProcess(vinculos);
        List<OficialJusticaProcessoNomeadoResponse.ProcessoRow> rows = grouped.values().stream()
                .map(list -> buildProcessoRow(scope, list))
                .filter(row -> matchesRito(row.rito(), rito))
                .filter(row -> matchesVara(row.vara(), vara))
                .filter(row -> !Boolean.TRUE.equals(somentePendentes) || row.possuiPendenciaAtiva())
                .sorted(Comparator.comparing(OficialJusticaProcessoNomeadoResponse.ProcessoRow::prazoFatalEm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OficialJusticaProcessoNomeadoResponse.ProcessoRow::processoId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
        int comSigilo = (int) rows.stream().filter(row -> row.alerts().contains("PROCESSO_SIGILOSO")).count();
        int comPendencia = (int) rows.stream().filter(OficialJusticaProcessoNomeadoResponse.ProcessoRow::possuiPendenciaAtiva).count();
        int comAcesso = (int) rows.stream().filter(OficialJusticaProcessoNomeadoResponse.ProcessoRow::acessoProcessoPermitido).count();
        List<String> alerts = new ArrayList<>();
        if (rows.isEmpty()) {
            alerts.add("Nenhum processo com vínculo operacional direto do oficial foi materializado ainda nesta visão.");
        }
        return new OficialJusticaProcessoNomeadoResponse(
                composeTerritorio(usuario),
                timeService.nowUtc(),
                toProcessoScope(scope),
                new OficialJusticaProcessoNomeadoResponse.Summary(
                        rows.size(),
                        comSigilo,
                        comPendencia,
                        comAcesso,
                        Math.max(0, rows.size() - comAcesso),
                        countDistinct(rows.stream().map(OficialJusticaProcessoNomeadoResponse.ProcessoRow::rito).toList()),
                        countDistinct(rows.stream().map(OficialJusticaProcessoNomeadoResponse.ProcessoRow::vara).toList())
                ),
                PROCESSO_COLUMNS,
                buildProcessoFilters(scope, vinculos),
                buildProcessoRitoBuckets(vinculos),
                rows,
                List.copyOf(alerts)
        );
    }

    @Transactional(readOnly = true)
    public OficialJusticaProcessoAcessoResponse acessoProcessoNomeado(Long processoId) {
        Usuario usuario = contextFactory.build().usuario();
        List<WorkItem> vinculos = organizationalScopeService.filterByScope(usuario, vinculoService.vinculosDiretosProcesso(processoId, usuario.getId(), usuario.getTipoUsuario(), 20));
        Processo processo = vinculos.stream()
                .findFirst()
                .map(WorkItem::getProcesso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo nomeado para oficial", processoId));
        AuthzDecision decision = hasReadAccess(processo);
        LinkedHashMap<String, Object> processoMap = new LinkedHashMap<>();
        processoMap.put("numeroProcesso", processo.getNumeroProcesso());
        processoMap.put("tribunal", processo.getTribunal());
        processoMap.put("vara", processo.getVara());
        processoMap.put("status", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        processoMap.put("faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        processoMap.put("sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : NivelSigilo.PUBLICO.name());
        processoMap.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        processoMap.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
        processoMap.put("assunto", processo.getAssunto());
        processoMap.put("classeProcessual", processo.getClasseProcessual());
        List<OficialJusticaProcessoAcessoResponse.VinculoRow> vinculoRows = vinculos.stream().map(item -> new OficialJusticaProcessoAcessoResponse.VinculoRow(
                item.getId(),
                item.getTitulo(),
                item.getType() != null ? item.getType().name() : null,
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getDueAt(),
                item.getQueueCode(),
                item.getInboxKey(),
                item.getTemplateCode()
        )).toList();
        List<OficialJusticaProcessoAcessoResponse.PendenciaRelacionada> pendencias = vinculos.stream()
                .filter(this::isOpen)
                .map(item -> new OficialJusticaProcessoAcessoResponse.PendenciaRelacionada(
                        item.getId(),
                        item.getTitulo(),
                        item.getStatus() != null ? item.getStatus().name() : null,
                        item.getDueAt(),
                        nextAction(item)
                ))
                .toList();
        List<String> alerts = new ArrayList<>();
        if (!decision.allowed()) {
            alerts.add("Leitura processual ainda não liberada pela política ativa; conferir materialização do vínculo direto do oficial no fluxo do processo.");
        }
        if (sigiloso(processo)) {
            alerts.add("PROCESSO_SIGILOSO");
        }
        return new OficialJusticaProcessoAcessoResponse(
                timeService.nowUtc(),
                processoId,
                processo.getNumeroProcesso(),
                decision.allowed(),
                accessReason(processo, decision),
                filterNulls(processoMap),
                vinculoRows,
                pendencias,
                List.copyOf(alerts)
        );
    }

    private OficialJusticaPendenciaOperacionalResponse.PendenciaRow buildPendenciaRow(OficialJusticaOrganizationalScopeService.Scope scope, WorkItem item) {
        Processo processo = item.getProcesso();
        AuthzDecision decision = hasReadAccess(processo);
        List<String> dependencias = processDependencies(processo, item);
        List<String> alerts = new ArrayList<>();
        if (sigiloso(processo)) {
            alerts.add("PROCESSO_SIGILOSO");
        }
        if (!decision.allowed()) {
            alerts.add("ACESSO_PROCESSUAL_PENDENTE_DE_MATERIALIZACAO");
        }
        return new OficialJusticaPendenciaOperacionalResponse.PendenciaRow(
                item.getId(),
                item.getProcessoId(),
                processo != null ? processo.getNumeroProcesso() : null,
                processo != null ? processo.getTribunal() : null,
                organizationalScopeService.resolveVaraDisplay(processo, item),
                organizationalScopeService.resolveLotacaoLabel(scope, processo, item),
                processo != null && processo.getRito() != null ? processo.getRito().name() : "COMUM_ORDINARIO",
                processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                classifyPending(item),
                classifyPriority(item),
                item.getDueAt(),
                decision.allowed(),
                accessReason(processo, decision),
                nextAction(item),
                dependencias,
                enrichProcessEnvelope(contextFactory.build().usuario(), processo, item, scope),
                List.copyOf(alerts)
        );
    }

    private OficialJusticaProcessoNomeadoResponse.ProcessoRow buildProcessoRow(OficialJusticaOrganizationalScopeService.Scope scope, List<WorkItem> vinculos) {
        WorkItem head = vinculos.getFirst();
        Processo processo = head.getProcesso();
        AuthzDecision decision = hasReadAccess(processo);
        boolean possuiPendenciaAtiva = vinculos.stream().anyMatch(this::isOpen);
        List<String> alerts = new ArrayList<>();
        if (sigiloso(processo)) {
            alerts.add("PROCESSO_SIGILOSO");
        }
        if (!decision.allowed()) {
            alerts.add("ACESSO_PROCESSUAL_RESTRITO");
        }
        Instant prazoFatal = vinculos.stream()
                .map(WorkItem::getDueAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new OficialJusticaProcessoNomeadoResponse.ProcessoRow(
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                processo != null ? processo.getTribunal() : null,
                organizationalScopeService.resolveVaraDisplay(processo, head),
                organizationalScopeService.resolveLotacaoLabel(scope, processo, head),
                processo != null && processo.getRito() != null ? processo.getRito().name() : "COMUM_ORDINARIO",
                processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                prazoFatal,
                baseNomeacao(head),
                decision.allowed(),
                accessReason(processo, decision),
                head.getId(),
                head.getType() != null ? head.getType().name() : null,
                head.getStatus() != null ? head.getStatus().name() : null,
                possuiPendenciaAtiva,
                possuiPendenciaAtiva ? nextAction(vinculos.stream().filter(this::isOpen).findFirst().orElse(head)) : "Consultar processo e concluir pendências vinculadas quando cabível.",
                enrichProcessEnvelope(contextFactory.build().usuario(), processo, head, scope),
                List.copyOf(alerts)
        );
    }

    private List<WorkItem> pendenciasBase(Usuario usuario, int limit) {
        List<WorkItem> scoped = organizationalScopeService.filterByScope(usuario, commons.inboxHibrido(usuario, limit).stream().filter(this::isOfficialOperationalItem).toList());
        return panelEgressService.reconcileVisibility(usuario, scoped).visibleItems();
    }

    private List<WorkItem> vinculosBase(Usuario usuario, int limit) {
        List<WorkItem> scoped = organizationalScopeService.filterByScope(usuario, vinculoService.vinculosDiretosUsuario(usuario.getId(), usuario.getTipoUsuario(), limit));
        return panelEgressService.reconcileVisibility(usuario, scoped).visibleItems();
    }

    private Map<Long, List<WorkItem>> groupedByProcess(List<WorkItem> vinculos) {
        return vinculos.stream()
                .filter(item -> item.getProcessoId() != null && item.getProcesso() != null)
                .collect(Collectors.groupingBy(WorkItem::getProcessoId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<OficialJusticaPendenciaOperacionalResponse.FilterGroup> buildPendenciaFilters(OficialJusticaOrganizationalScopeService.Scope scope, List<WorkItem> items) {
        return List.of(
                new OficialJusticaPendenciaOperacionalResponse.FilterGroup("rito", "Rito processual", organizationalScopeService.availableRitos(items)),
                new OficialJusticaPendenciaOperacionalResponse.FilterGroup("vara", "Vara / lotação", organizationalScopeService.availableVaras(items)),
                new OficialJusticaPendenciaOperacionalResponse.FilterGroup("justicaAxis", "Justiça / malha do oficial", unique(items.stream().map(item -> communicationFormalModelService.resolveJusticaAxis(item.getProcesso(), item, contextFactory.build().usuario())).toList())),
                new OficialJusticaPendenciaOperacionalResponse.FilterGroup("naturezaComunicacao", "Natureza da diligência pessoal", unique(items.stream().map(item -> communicationFormalModelService.resolveNaturezaComunicacao(item.getProcesso(), item, null)).toList())),
                new OficialJusticaPendenciaOperacionalResponse.FilterGroup("lotacao", "Recorte institucional", List.of(scope.label()))
        );
    }

    private List<OficialJusticaProcessoNomeadoResponse.FilterGroup> buildProcessoFilters(OficialJusticaOrganizationalScopeService.Scope scope, List<WorkItem> items) {
        return List.of(
                new OficialJusticaProcessoNomeadoResponse.FilterGroup("rito", "Rito processual", organizationalScopeService.availableRitos(items)),
                new OficialJusticaProcessoNomeadoResponse.FilterGroup("vara", "Vara / lotação", organizationalScopeService.availableVaras(items)),
                new OficialJusticaProcessoNomeadoResponse.FilterGroup("justicaAxis", "Justiça / malha do oficial", unique(items.stream().map(item -> communicationFormalModelService.resolveJusticaAxis(item.getProcesso(), item, contextFactory.build().usuario())).toList())),
                new OficialJusticaProcessoNomeadoResponse.FilterGroup("naturezaComunicacao", "Natureza da diligência pessoal", unique(items.stream().map(item -> communicationFormalModelService.resolveNaturezaComunicacao(item.getProcesso(), item, null)).toList())),
                new OficialJusticaProcessoNomeadoResponse.FilterGroup("lotacao", "Recorte institucional", List.of(scope.label()))
        );
    }


    private Map<String, Object> enrichProcessEnvelope(Usuario usuario, Processo processo, WorkItem item, OficialJusticaOrganizationalScopeService.Scope scope) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(contextEnvelopeService.processEnvelope(usuario, processo, item, scope, organizationalScopeService));
        out.put("formalModel", communicationFormalModelService.buildProfile(processo, item, usuario));
        return Collections.unmodifiableMap(out);
    }

    private List<String> unique(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private List<OficialJusticaProcessoNomeadoResponse.RitoBucket> buildProcessoRitoBuckets(List<WorkItem> items) {
        return organizationalScopeService.processNumbersByRito(items).entrySet().stream()
                .map(entry -> new OficialJusticaProcessoNomeadoResponse.RitoBucket(entry.getKey(), entry.getValue().size(), entry.getValue()))
                .toList();
    }

    private OficialJusticaPendenciaOperacionalResponse.Scope toPendenciaScope(OficialJusticaOrganizationalScopeService.Scope scope) {
        return new OficialJusticaPendenciaOperacionalResponse.Scope(scope.mode(), scope.label(), scope.institutionManaged(), scope.cobreTodasAsVaras(), scope.varas(), scope.unidades());
    }

    private OficialJusticaProcessoNomeadoResponse.Scope toProcessoScope(OficialJusticaOrganizationalScopeService.Scope scope) {
        return new OficialJusticaProcessoNomeadoResponse.Scope(scope.mode(), scope.label(), scope.institutionManaged(), scope.cobreTodasAsVaras(), scope.varas(), scope.unidades());
    }

    private boolean isOfficialOperationalItem(WorkItem item) {
        if (item == null) {
            return false;
        }
        return commons.titleContains(item, "MANDADO", "CITACAO", "INTIMACAO", "BUSCA", "PENHORA", "OFICIO", "CERTIDAO", "DILIGENCIA", "JUNTAR", "RECEBER")
                || item.getAssignedRole() == TipoUsuario.OFICIAL_JUSTICA
                || item.getAssignedRole() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
    }

    private boolean isOpen(WorkItem item) {
        return item != null && item.getStatus() != null && item.getStatus() != WorkItemStatus.CONCLUIDO && item.getStatus() != WorkItemStatus.CANCELADO;
    }

    private boolean isOverdue(WorkItem item) {
        return item != null && item.getDueAt() != null && !item.getDueAt().isAfter(timeService.nowUtc());
    }

    private int urgencyRank(WorkItem item) {
        if (item == null || item.getDueAt() == null) {
            return 1;
        }
        long hours = ChronoUnit.HOURS.between(timeService.nowUtc(), item.getDueAt());
        if (hours <= 0) {
            return 5;
        }
        if (hours <= 24) {
            return 4;
        }
        if (hours <= 72) {
            return 3;
        }
        if (hours <= 168) {
            return 2;
        }
        return 1;
    }

    private String classifyPriority(WorkItem item) {
        return switch (urgencyRank(item)) {
            case 5, 4 -> "CRITICA";
            case 3 -> "ALTA";
            case 2 -> "MEDIA";
            default -> "OPERACIONAL";
        };
    }

    private String classifyPending(WorkItem item) {
        if (item == null) {
            return "OPERACIONAL";
        }
        if (commons.titleContains(item, "JUNTAR", "RECEBER", "CARTORIO")) {
            return "JUNTADA_CARTORARIA";
        }
        if (commons.titleContains(item, "CONFIRMACAO", "ACK", "MALHA", "EXTERNA", "OFICIO")) {
            return "CONFIRMACAO_EXTERNA";
        }
        if (commons.titleContains(item, "MANDADO", "CITACAO", "INTIMACAO", "BUSCA", "DILIGENCIA", "PENHORA")) {
            return "RASTREIO_PREVIO";
        }
        if (commons.titleContains(item, "CERTIDAO")) {
            return "CERTIFICACAO_OPERACIONAL";
        }
        return "OPERACIONAL";
    }

    private String accessReason(Processo processo, AuthzDecision decision) {
        if (processo == null) {
            return "SEM_PROCESSO_ASSOCIADO";
        }
        if (decision.allowed()) {
            return sigiloso(processo) ? "OFICIAL_NOMEADO_COM_LEITURA_LIBERADA_NO_SIGILO" : "PROCESSO_COM_LEITURA_LIBERADA";
        }
        return sigiloso(processo) ? "SIGILO_SEM_VINCULO_DIRETO_MATERIALIZADO" : "LEITURA_AINDA_NAO_LIBERADA_PELA_POLITICA_ATIVA";
    }

    private String nextAction(WorkItem item) {
        String kind = classifyPending(item);
        return switch (kind) {
            case "RASTREIO_PREVIO" -> "Abrir rastreio do alvo, revisar endereço e encaixar na rota do dia antes da diligência.";
            case "JUNTADA_CARTORARIA" -> "Conferir peça, anexos e provocar recebimento/juntada cartorária no fluxo do processo.";
            case "CONFIRMACAO_EXTERNA" -> "Acompanhar canal externo, confirmar ACK pendente e reconciliar a execução rastreável.";
            case "CERTIFICACAO_OPERACIONAL" -> "Concluir certidão operacional com documentos vinculados e formalizar no processo.";
            default -> "Abrir o processo vinculado e seguir a próxima ação operacional do work item.";
        };
    }

    private String baseNomeacao(WorkItem item) {
        if (item == null) {
            return "SEM_VINCULO";
        }
        if (item.getTemplateCode() != null && !item.getTemplateCode().isBlank()) {
            return item.getTemplateCode();
        }
        if (item.getTitulo() != null && !item.getTitulo().isBlank()) {
            return item.getTitulo();
        }
        return "VINCULO_OPERACIONAL_DIRETO";
    }

    private List<String> processDependencies(Processo processo, WorkItem item) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (processo == null) {
            out.add("PROCESSO_NAO_ASSOCIADO");
            return List.copyOf(out);
        }
        if (processo.getStatusProcesso() == null || processo.getStatusProcesso().name().contains("SUSPENS")) {
            out.add("PROCESSO_COM_STATUS_SENSIVEL_PARA_DILIGENCIA");
        }
        if (processo.getFaseAtual() == null) {
            out.add("FASE_PROCESSUAL_NAO_IDENTIFICADA");
        }
        if (sigiloso(processo)) {
            out.add("LEITURA_DEPENDE_DE_VINCULO_DIRETO_DO_OFICIAL");
        }
        if (item != null && item.getDueAt() != null && !item.getDueAt().isAfter(timeService.nowUtc())) {
            out.add("PRAZO_FATAL_IMEDIATO_OU_ATRASADO");
        }
        if (commons.titleContains(item, "OFICIO", "ACK", "MALHA")) {
            out.add("DEPENDE_DE_CONFIRMACAO_EXTERNA_OU_CARTORARIA");
        }
        return List.copyOf(out);
    }

    private AuthzDecision hasReadAccess(Processo processo) {
        if (processo == null) {
            return AuthzDecision.deny("processo_nulo", "oficial-portfolio");
        }
        return authorizationService.canReadProcesso(processo);
    }

    private boolean sigiloso(Processo processo) {
        return processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial();
    }

    private boolean matchesRito(WorkItem item, String rito) {
        String actual = item != null && item.getProcesso() != null && item.getProcesso().getRito() != null ? item.getProcesso().getRito().name() : "COMUM_ORDINARIO";
        return matchesRito(actual, rito);
    }

    private boolean matchesRito(String actual, String rito) {
        if (rito == null || rito.isBlank() || "TODOS".equalsIgnoreCase(rito)) {
            return true;
        }
        return actual != null && actual.equalsIgnoreCase(rito.trim());
    }

    private boolean matchesVara(WorkItem item, String vara) {
        return matchesVara(organizationalScopeService.resolveVaraDisplay(item != null ? item.getProcesso() : null, item), vara);
    }

    private boolean matchesVara(String actual, String vara) {
        if (vara == null || vara.isBlank() || "TODAS".equalsIgnoreCase(vara)) {
            return true;
        }
        return actual != null && actual.equalsIgnoreCase(vara.trim());
    }

    private String composeTerritorio(Usuario usuario) {
        if (usuario == null) {
            return "NAO_INFORMADO";
        }
        String comarca = usuario.getComarca();
        String uf = usuario.getUf();
        if (comarca != null && !comarca.isBlank() && uf != null && !uf.isBlank()) {
            return comarca + '/' + uf;
        }
        if (uf != null && !uf.isBlank()) {
            return uf;
        }
        return "NAO_INFORMADO";
    }

    private int countDistinct(List<String> values) {
        return (int) values.stream().filter(Objects::nonNull).distinct().count();
    }

    private static Map<String, Object> filterNulls(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null) {
            return out;
        }
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return Collections.unmodifiableMap(out);
    }
}
