package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.forum.ForumOfficialReturnInboxService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaPanelEgressService {

    private static final Set<TipoUsuario> OFFICIAL_ROLES = Set.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);

    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService routingService;
    private final PainelServiceCommons commons;
    private final PjbTimeService timeService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final ForumOfficialReturnInboxService forumOfficialReturnInboxService;

    public OficialJusticaPanelEgressService(WorkItemRepository workItemRepository,
                                            InstitutionalActorRoutingService routingService,
                                            PainelServiceCommons commons,
                                            PjbTimeService timeService,
                                            OficialJusticaContextEnvelopeService contextEnvelopeService,
                                            ForumOfficialReturnInboxService forumOfficialReturnInboxService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.routingService = Objects.requireNonNull(routingService);
        this.commons = Objects.requireNonNull(commons);
        this.timeService = Objects.requireNonNull(timeService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.forumOfficialReturnInboxService = Objects.requireNonNull(forumOfficialReturnInboxService);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @PjbTransactionalBudget(operation = "oficial-justica.panel-egress.reconcile-visibility", maxMillis = 8000)
    public VisibilitySnapshot reconcileVisibility(Usuario usuario, List<WorkItem> source) {
        List<WorkItem> incoming = source == null ? List.of() : source.stream().filter(Objects::nonNull).toList();
        if (incoming.isEmpty()) {
            return new VisibilitySnapshot(List.of(), 0, List.of());
        }
        if (usuario == null || usuario.getId() == null || usuario.getTipoUsuario() == null || !OFFICIAL_ROLES.contains(usuario.getTipoUsuario())) {
            return new VisibilitySnapshot(filterVisible(incoming), 0, List.of());
        }
        LinkedHashMap<Long, List<WorkItem>> grouped = incoming.stream()
                .filter(item -> item.getProcessoId() != null && item.getProcesso() != null)
                .collect(Collectors.groupingBy(WorkItem::getProcessoId, LinkedHashMap::new, Collectors.toList()));
        LinkedHashSet<Long> removedProcessIds = new LinkedHashSet<>();
        ArrayList<Map<String, Object>> transfers = new ArrayList<>();
        ArrayList<WorkItem> dirty = new ArrayList<>();
        for (Map.Entry<Long, List<WorkItem>> entry : grouped.entrySet()) {
            EgressDecision decision = resolveDecision(usuario, entry.getValue());
            if (decision == null) {
                continue;
            }
            WorkItem forumDeskItem = ensureForumDeskItem(usuario, decision, entry.getValue());
            removedProcessIds.add(entry.getKey());
            transfers.add(decisionDigest(usuario, decision, forumDeskItem));
            for (WorkItem item : entry.getValue()) {
                boolean changed = false;
                if (!item.isSemInteresse()) {
                    item.setSemInteresse(true);
                    changed = true;
                }
                if (decision.cancelOpenOfficialItems() && isOpen(item) && item.getStatus() != WorkItemStatus.CANCELADO) {
                    item.setStatus(WorkItemStatus.CANCELADO);
                    changed = true;
                }
                if (changed) {
                    dirty.add(item);
                }
            }
        }
        if (!dirty.isEmpty()) {
            workItemRepository.saveAll(dirty);
        }
        List<WorkItem> visible = filterVisible(incoming).stream()
                .filter(item -> item.getProcessoId() == null || !removedProcessIds.contains(item.getProcessoId()))
                .toList();
        return new VisibilitySnapshot(visible, removedProcessIds.size(), List.copyOf(transfers));
    }

    private EgressDecision resolveDecision(Usuario usuario, List<WorkItem> officialItems) {
        if (officialItems == null || officialItems.isEmpty()) {
            return null;
        }
        Processo processo = officialItems.getFirst().getProcesso();
        if (processo == null || processo.getId() == null) {
            return null;
        }
        List<WorkItem> processUniverse = workItemRepository.findAllByProcesso(processo.getId());
        boolean hasOpenOfficial = officialItems.stream().anyMatch(item -> !item.isSemInteresse() && isOpen(item));
        boolean hasVisibleOfficial = officialItems.stream().anyMatch(item -> !item.isSemInteresse());
        boolean activeForeignDemand = processUniverse.stream().anyMatch(item -> !item.isSemInteresse() && !isOfficialRole(item.getAssignedRole()) && isOpen(item));
        boolean closed = isClosedProcess(processo);
        boolean redirected = !closed && !hasOpenOfficial && activeForeignDemand;
        boolean nextDemand = !closed && !hasOpenOfficial && hasVisibleOfficial && isNextDemandProcess(processo);
        if (!closed && !redirected && !nextDemand) {
            return null;
        }
        EgressReason reason = closed ? EgressReason.ARQUIVAMENTO_OU_BAIXA : redirected ? EgressReason.DEMANDA_REDIRECIONADA : EgressReason.PROXIMA_DEMANDA_PROCESSUAL;
        InstitutionalActorRoutingService.InstitutionalRoute route = routingService.secretaryExecution(processo.getId(), reason.actionAxis(processo));
        String templateCode = "BALCAO_OFICIAL_AUTODESTINO:" + usuario.getId() + ':' + reason.code();
        return new EgressDecision(processo, reason, route, templateCode, true, summarize(usuario, processo, officialItems, processUniverse, reason));
    }

    private WorkItem ensureForumDeskItem(Usuario usuario, EgressDecision decision, List<WorkItem> officialItems) {
        ForumOfficialReturnInboxService.ReturnEnvelope compartment = forumOfficialReturnInboxService.classify(decision.processo(), usuario, officialItems == null || officialItems.isEmpty() ? null : officialItems.getFirst(), decision.reason().code(), decision.reason().label(), decision.route());
        String templateCode = decision.templateCode() + ":" + compartment.compartmentCode();
        Optional<WorkItem> existing = workItemRepository.findLatestByProcessoIdAndTemplateCode(decision.processo().getId(), templateCode);
        if (existing.isPresent() && existing.get().getStatus() != WorkItemStatus.CANCELADO) {
            return existing.get();
        }
        Instant dueAt = decision.reason() == EgressReason.ARQUIVAMENTO_OU_BAIXA
                ? timeService.nowUtc().plus(12, ChronoUnit.HOURS)
                : timeService.nowUtc().plus(2, ChronoUnit.HOURS);
        WorkItem deskItem = WorkItem.builder()
                .processo(decision.processo())
                .faseOrigem(decision.processo().getFaseAtual())
                .templateCode(templateCode)
                .type(WorkItemType.JUNTADA)
                .titulo(compartment.title())
                .descricao(forumOfficialReturnInboxService.renderSummary(compartment, decision.summary()))
                .queueCode(compartment.queueCode())
                .inboxKey(compartment.inboxKey())
                .assignedRole(decision.route().assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(decision.reason().priority())
                .blocking(false)
                .dueAt(dueAt)
                .uf(decision.processo().getUf())
                .comarca(firstNonBlank(decision.processo().getComarca(), usuario.getComarca()))
                .baseLegal(decision.reason().baseLegal(decision.processo(), decision.route()))
                .build();
        WorkItem saved = workItemRepository.save(deskItem);
        String message = "Processo " + processNumber(decision.processo()) + " removido automaticamente do painel do Oficial e encaminhado para " + decision.reason().compartmentLabel(decision.processo()) + '.';
        commons.publishUserHistory(usuario, "OFICIAL", "OFICIAL_AUTODESTINO_BALCAO", message, decision.processo(), saved.getId());
        commons.publishTerritoryHistory(usuario, "OFICIAL", "OFICIAL_AUTODESTINO_BALCAO", message, decision.processo(), saved.getId());
        return saved;
    }

    private Map<String, Object> decisionDigest(Usuario usuario, EgressDecision decision, WorkItem deskItem) {
        ForumOfficialReturnInboxService.ReturnEnvelope compartment = forumOfficialReturnInboxService.classify(decision.processo(), usuario, deskItem, decision.reason().code(), decision.reason().label(), decision.route());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", decision.processo().getId());
        out.put("processoNumero", processNumber(decision.processo()));
        out.put("motivo", decision.reason().code());
        out.put("motivoLabel", decision.reason().label());
        out.put("compartimento", compartment.compartmentCode());
        out.put("compartimentoLabel", compartment.compartmentLabel());
        out.put("filaUnidade", compartment.queueCode());
        out.put("folderCode", compartment.folderCode());
        out.put("queueCode", deskItem != null ? deskItem.getQueueCode() : compartment.queueCode());
        out.put("inboxKey", deskItem != null ? deskItem.getInboxKey() : compartment.inboxKey());
        out.put("workItemId", deskItem != null ? deskItem.getId() : null);
        out.put("movedAt", timeService.nowUtc());
        out.put("unidadeContexto", compartment.unitContext());
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(usuario, null));
        out.put("reativavelPorReintimacao", Boolean.TRUE);
        return out.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
    }

    private String summarize(Usuario usuario, Processo processo, List<WorkItem> officialItems, List<WorkItem> processUniverse, EgressReason reason) {
        long openOfficial = officialItems.stream().filter(this::isOpen).count();
        long doneOfficial = officialItems.stream().filter(this::isDone).count();
        long foreignOpen = processUniverse.stream().filter(item -> !isOfficialRole(item.getAssignedRole()) && isOpen(item)).count();
        String latestTitles = officialItems.stream()
                .sorted(Comparator.comparing(WorkItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(WorkItem::getTitulo)
                .filter(Objects::nonNull)
                .limit(3)
                .collect(Collectors.joining(" | "));
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        parts.add("Resumo compacto do balcão do fórum/tribunal para retorno automático do Oficial de Justiça.");
        parts.add("Oficial responsável: " + firstNonBlank(usuario != null ? usuario.getNome() : null, "OFICIAL_NAO_IDENTIFICADO") + '.');
        parts.add("Tipo do oficial: " + contextEnvelopeService.officialTypeLabel(usuario != null ? usuario.getTipoUsuario() : null) + '.');
        parts.add("Processo " + processNumber(processo) + '.');
        parts.add("Vara: " + firstNonBlank(processo.getVara(), "VARA_NAO_IDENTIFICADA") + '.');
        parts.add("Fórum: " + contextEnvelopeService.resolveForum(processo, contextEnvelopeService.resolveEsfera(usuario, processo, usuario != null ? usuario.getTipoUsuario() : null), firstNonBlank(processo.getComarca(), usuario != null ? usuario.getComarca() : null)) + '.');
        parts.add("Cidade: " + firstNonBlank(processo.getComarca(), usuario != null ? usuario.getComarca() : null, "CIDADE_NAO_IDENTIFICADA") + "/" + firstNonBlank(processo.getUf(), usuario != null ? usuario.getUf() : null, "UF") + '.');
        parts.add("Tribunal: " + firstNonBlank(processo.getTribunal(), "TRIBUNAL_NAO_IDENTIFICADO") + '.');
        parts.add("Região judicial: " + contextEnvelopeService.resolveRegiaoJudicial(processo.getTribunal(), firstNonBlank(processo.getUf(), usuario != null ? usuario.getUf() : null), contextEnvelopeService.resolveEsfera(usuario, processo, usuario != null ? usuario.getTipoUsuario() : null)) + '.');
        parts.add("Motivo: " + reason.label() + '.');
        parts.add("Compartimento: " + reason.compartmentLabel(processo) + '.');
        parts.add("Rito: " + (processo.getRito() == null ? "COMUM_ORDINARIO" : processo.getRito().name()) + '.');
        if (processo.getStatusProcesso() != null) {
            parts.add("Status atual: " + processo.getStatusProcesso().name() + '.');
        }
        if (processo.getFaseAtual() != null) {
            parts.add("Fase atual: " + processo.getFaseAtual().name() + '.');
        }
        parts.add("Itens oficiais abertos: " + openOfficial + ". Itens oficiais consolidados: " + doneOfficial + '.');
        if (foreignOpen > 0) {
            parts.add("Há " + foreignOpen + " itens ativos em outras mesas/cartórios para continuidade do processo.");
        }
        if (latestTitles != null && !latestTitles.isBlank()) {
            parts.add("Últimos títulos operacionais: " + latestTitles + '.');
        }
        return trim(String.join(" ", parts), 1800);
    }

    private List<WorkItem> filterVisible(List<WorkItem> items) {
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.isSemInteresse())
                .filter(item -> item.getProcesso() != null)
                .filter(item -> !isClosedProcess(item.getProcesso()))
                .toList();
    }

    private boolean isClosedProcess(Processo processo) {
        StatusProcesso status = processo == null ? null : processo.getStatusProcesso();
        return status != null && (status.isArquivadoOuBaixado() || status.isTransitado() || status.isEncerrado());
    }

    private boolean isNextDemandProcess(Processo processo) {
        StatusProcesso status = processo == null ? null : processo.getStatusProcesso();
        return status != null && (status.isPosDecisao() || status.isExecutorio() || status == StatusProcesso.CONCLUSO);
    }

    private boolean isOfficialRole(TipoUsuario tipoUsuario) {
        return tipoUsuario != null && OFFICIAL_ROLES.contains(tipoUsuario);
    }

    private boolean isOpen(WorkItem item) {
        return item != null && (item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO);
    }

    private boolean isDone(WorkItem item) {
        return item != null && (item.getStatus() == WorkItemStatus.CONCLUIDO || item.getStatus() == WorkItemStatus.CANCELADO);
    }

    private String processNumber(Processo processo) {
        return firstNonBlank(processo == null ? null : processo.getNumeroProcesso(), processo == null ? null : processo.getNumero(), processo == null ? null : processo.getNumeroUnificado(), "PROCESSO_NAO_IDENTIFICADO");
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

    private String trim(String value, int limit) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= Math.max(8, limit)) {
            return normalized;
        }
        return normalized.substring(0, Math.max(8, limit) - 1) + '…';
    }

    public record VisibilitySnapshot(
            List<WorkItem> visibleItems,
            int autoRemovedCount,
            List<Map<String, Object>> dispatchedToDesk
    ) {
        public VisibilitySnapshot {
            visibleItems = visibleItems == null ? List.of() : List.copyOf(visibleItems);
            dispatchedToDesk = dispatchedToDesk == null ? List.of() : List.copyOf(dispatchedToDesk);
        }
    }

    private record EgressDecision(
            Processo processo,
            EgressReason reason,
            InstitutionalActorRoutingService.InstitutionalRoute route,
            String templateCode,
            boolean cancelOpenOfficialItems,
            String summary
    ) {
    }

    private enum EgressReason {
        ARQUIVAMENTO_OU_BAIXA,
        DEMANDA_REDIRECIONADA,
        PROXIMA_DEMANDA_PROCESSUAL;

        String code() {
            return name();
        }

        String label() {
            return switch (this) {
                case ARQUIVAMENTO_OU_BAIXA -> "Arquivamento, baixa ou encerramento terminal";
                case DEMANDA_REDIRECIONADA -> "Processo já seguiu para outra mesa ou secretaria";
                case PROXIMA_DEMANDA_PROCESSUAL -> "Cumprimento do Oficial encerrado e próxima demanda processual já identificada";
            };
        }

        String actionAxis(Processo processo) {
            return switch (this) {
                case ARQUIVAMENTO_OU_BAIXA -> "ARQUIVAMENTO_BALCAO_OFICIAL";
                case DEMANDA_REDIRECIONADA -> isTribunalTransition(processo) ? "TRIAGEM_TRIBUNAL_BALCAO_OFICIAL" : "RETORNO_FORUM_BALCAO_OFICIAL";
                case PROXIMA_DEMANDA_PROCESSUAL -> isTribunalTransition(processo) ? "TRIAGEM_TRIBUNAL_BALCAO_OFICIAL" : "PROXIMA_DEMANDA_CARTORIO_OFICIAL";
            };
        }

        String title(Processo processo) {
            return switch (this) {
                case ARQUIVAMENTO_OU_BAIXA -> "Guardar movimentações do Oficial e consolidar arquivamento/baixa";
                case DEMANDA_REDIRECIONADA -> "Receber retorno do Oficial e redistribuir para a mesa competente";
                case PROXIMA_DEMANDA_PROCESSUAL -> "Receber retorno do Oficial e preparar próxima demanda processual";
            };
        }

        int priority() {
            return switch (this) {
                case ARQUIVAMENTO_OU_BAIXA -> 4;
                case DEMANDA_REDIRECIONADA -> 2;
                case PROXIMA_DEMANDA_PROCESSUAL -> 3;
            };
        }

        String compartmentLabel(Processo processo) {
            return switch (this) {
                case ARQUIVAMENTO_OU_BAIXA -> "BALCAO_FORUM_ARQUIVAMENTO_CONTROLADO";
                case DEMANDA_REDIRECIONADA -> isTribunalTransition(processo) ? "BALCAO_TRIBUNAL_TRANSICAO" : "BALCAO_FORUM_REDISTRIBUICAO";
                case PROXIMA_DEMANDA_PROCESSUAL -> isTribunalTransition(processo) ? "BALCAO_TRIBUNAL_PROXIMA_DEMANDA" : "BALCAO_CARTORIO_PROXIMA_DEMANDA";
            };
        }

        String baseLegal(Processo processo, InstitutionalActorRoutingService.InstitutionalRoute route) {
            String tribunal = processo == null ? null : processo.getTribunal();
            return switch (this) {
                case ARQUIVAMENTO_OU_BAIXA -> "Retorno automático do Oficial para guarda leve e saneamento terminal do processo em " + firstNonBlankStatic(tribunal, route == null ? null : route.topologyKey(), "TRIBUNAL_NAO_IDENTIFICADO");
                case DEMANDA_REDIRECIONADA -> "Retorno automático do Oficial para redistribuição organizada à mesa seguinte sem persistir carga desnecessária no painel do Oficial.";
                case PROXIMA_DEMANDA_PROCESSUAL -> "Encaminhamento leve ao balcão do fórum/tribunal para a próxima demanda após encerramento do ciclo operacional do Oficial.";
            };
        }

        private static boolean isTribunalTransition(Processo processo) {
            String tribunal = processo == null || processo.getTribunal() == null ? "" : processo.getTribunal().toUpperCase(Locale.ROOT);
            StatusProcesso status = processo == null ? null : processo.getStatusProcesso();
            return tribunal.startsWith("TRF")
                    || tribunal.startsWith("TJ")
                    || tribunal.startsWith("TRE")
                    || tribunal.startsWith("TRT")
                    || tribunal.startsWith("STM")
                    || tribunal.startsWith("STJ")
                    || tribunal.startsWith("STF")
                    || status != null && status.isRecursalOuEmbargos();
        }

        private static String firstNonBlankStatic(String... values) {
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
    }
}
