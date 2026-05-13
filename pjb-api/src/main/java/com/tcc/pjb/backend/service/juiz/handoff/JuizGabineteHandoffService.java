package com.tcc.pjb.backend.service.juiz.handoff;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.assessor.guardrails.AssessorGabineteGuardRailService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JuizGabineteHandoffService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final JuizProcessoGuardRailService juizGuardRailService;
    private final AssessorGabineteGuardRailService assessorGuardRailService;
    private final JuizGabineteRoutingResolver routingResolver;
    private final AuditLedgerService auditLedgerService;

    public JuizGabineteHandoffService(CurrentUserService currentUserService,
                                      ProcessoRepository processoRepository,
                                      WorkItemRepository workItemRepository,
                                      JuizProcessoGuardRailService juizGuardRailService,
                                      AssessorGabineteGuardRailService assessorGuardRailService,
                                      JuizGabineteRoutingResolver routingResolver,
                                      AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.juizGuardRailService = Objects.requireNonNull(juizGuardRailService);
        this.assessorGuardRailService = Objects.requireNonNull(assessorGuardRailService);
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public HandoffSnapshot snapshot(Long processoId) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        JuizProcessoGuardRailService.GuardRailSnapshot judgeGuard = juizGuardRailService.avaliar(processoId, "HANDOFF_GABINETE");
        AssessorGabineteGuardRailService.AssessorProcessoGuardRailSnapshot assessorGuard = actor.isAssessor()
                ? assessorGuardRailService.avaliarProcesso(processoId)
                : null;
        List<WorkItem> allItems = workItemRepository.findAllByProcesso(processoId);
        List<WorkItem> activeItems = allItems.stream().filter(this::isActive).toList();

        List<HandoffLaneView> gabinete = new ArrayList<>();
        List<HandoffLaneView> assessoria = new ArrayList<>();
        List<HandoffLaneView> secretaria = new ArrayList<>();
        List<HandoffLaneView> drift = new ArrayList<>();
        List<HandoffHistoryEntry> history = buildHistory(allItems, routing);
        List<HandoffSignal> signals = new ArrayList<>();

        WorkItem judgeCapture = findActiveJudgeCapture(activeItems, routing);
        WorkItem gabineteToAssessoria = findActiveByPrefix(activeItems, handoffPrefixGabineteAssessoria(routing, processoId));
        WorkItem assessoriaToGabinete = findActiveByPrefix(activeItems, handoffPrefixAssessoriaGabinete(routing, processoId));
        WorkItem secretariaPending = findActiveByPrefix(activeItems, handoffPrefixGabineteSecretaria(routing, processoId));

        for (WorkItem item : activeItems) {
            HandoffLaneView view = toLaneView(item, routing, actor);
            switch (view.lane()) {
                case "GABINETE" -> gabinete.add(view);
                case "ASSESSORIA" -> assessoria.add(view);
                case "SECRETARIA" -> secretaria.add(view);
                default -> drift.add(view);
            }
        }

        if (judgeCapture == null) {
            signals.add(signal("CAPTURA_GABINETE", "ALTA", false,
                    "Não existe captura ativa do gabinete para este processo."));
        } else {
            signals.add(signal("CAPTURA_GABINETE", "INFO", true,
                    "Captura ativa do gabinete encontrada na mesa correta."));
        }
        if (gabineteToAssessoria == null) {
            signals.add(signal("HANDOFF_ASSESSORIA", "MEDIA", false,
                    "Ainda não existe handoff formal do gabinete para a assessoria."));
        } else {
            signals.add(signal("HANDOFF_ASSESSORIA", "INFO", true,
                    "Handoff formal do gabinete para a assessoria está ativo."));
        }
        if (assessoriaToGabinete == null) {
            signals.add(signal("RETORNO_ASSESSORIA", "MEDIA", true,
                    "A assessoria ainda não devolveu a minuta para revisão do gabinete."));
        } else {
            signals.add(signal("RETORNO_ASSESSORIA", "INFO", true,
                    "Existe retorno formal da assessoria para revisão do gabinete."));
        }
        if (secretariaPending == null) {
            signals.add(signal("RETORNO_SECRETARIA", "INFO", true,
                    "Não há handoff de retorno para a secretaria pendente neste momento."));
        } else {
            signals.add(signal("RETORNO_SECRETARIA", "MEDIA", false,
                    "Há handoff do gabinete para a secretaria aguardando tratamento cartorário."));
        }
        if (!drift.isEmpty()) {
            signals.add(signal("DRIFT_HANDOFF", "ALTA", false,
                    "Foram detectados itens fora das lanes topológicas de gabinete, assessoria ou secretaria."));
        } else {
            signals.add(signal("DRIFT_HANDOFF", "INFO", true,
                    "Sem drift entre as lanes topológicas do handoff."));
        }

        String recommendedAction = resolveRecommendedAction(actor, judgeCapture, gabineteToAssessoria, assessoriaToGabinete, secretariaPending, drift.isEmpty(), judgeGuard.allowed());
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("gabineteItems", gabinete.size());
        metrics.put("assessoriaItems", assessoria.size());
        metrics.put("secretariaItems", secretaria.size());
        metrics.put("driftItems", drift.size());
        metrics.put("historyEntries", history.size());
        metrics.put("judgeCaptureActive", judgeCapture != null);
        metrics.put("formalAssessoriaHandoff", gabineteToAssessoria != null);
        metrics.put("assessoriaReturnActive", assessoriaToGabinete != null);
        metrics.put("secretariaReturnActive", secretariaPending != null);
        metrics.put("recommendedAction", recommendedAction);
        metrics.put("routeKey", routing.routeKey());
        metrics.put("organizationalPath", routing.organizationalPath());
        metrics.entrySet().removeIf(entry -> entry.getValue() == null);

        return new HandoffSnapshot(
                actor.getId(),
                actor.getNome(),
                actor.getTipoUsuario() == null ? null : actor.getTipoUsuario().name(),
                processo.getId(),
                numeroProcesso(processo),
                routing,
                judgeGuard,
                assessorGuard,
                List.copyOf(gabinete),
                List.copyOf(assessoria),
                List.copyOf(secretaria),
                List.copyOf(drift),
                List.copyOf(history),
                List.copyOf(signals),
                recommendedAction,
                Map.copyOf(metrics)
        );
    }

    @Transactional
    public HandoffAction encaminharParaAssessoria(Long processoId, String tarefa, String observacao) {
        Usuario actor = requireJudgeActor();
        Processo processo = loadProcesso(processoId);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = juizGuardRailService.requireAtuacaoPermitida(processo, actor, "HANDOFF_GABINETE_PARA_ASSESSORIA");
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        List<WorkItem> activeItems = workItemRepository.findAllByProcesso(processoId).stream().filter(this::isActive).toList();
        WorkItem judgeCapture = findActiveJudgeCapture(activeItems, routing);
        if (judgeCapture == null || judgeCapture.getAssignedUser() == null || !Objects.equals(judgeCapture.getAssignedUser().getId(), actor.getId())) {
            throw new AccessDeniedPjbException("O processo precisa estar capturado pelo magistrado correto antes do handoff para a assessoria.");
        }
        concludeMatching(activeItems, item -> startsWith(item.getTemplateCode(), handoffPrefixGabineteAssessoria(routing, processoId), handoffPrefixAssessoriaGabinete(routing, processoId)));
        String templateCode = handoffPrefixGabineteAssessoria(routing, processoId);
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, templateCode)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        Instant dueAt = resolveDueInstant(routing.captureSla() == null ? null : routing.captureSla().dividedBy(2), 6);
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(WorkItemType.DISTRIBUICAO);
        item.setTitulo("Handoff do gabinete para a assessoria — " + numeroProcesso(processo));
        item.setDescricao(buildAssessoriaDescription(actor, processo, routing, tarefa, observacao));
        item.setQueueCode(routing.advisoryDesk());
        item.setInboxKey(routing.gabineteInboxKey());
        item.setAssignedRole(resolveAssessoriaRole(actor));
        item.setAssignedUser(null);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(resolvePriority(processo));
        item.setBlocking(true);
        item.setUf(firstNonBlank(processo.getUf(), actor.getUf()));
        item.setComarca(firstNonBlank(processo.getComarca(), actor.getComarca()));
        item.setBaseLegal("Handoff formal do gabinete para a assessoria com isolamento topológico, captura válida e histórico auditável.");
        item.setDueAt(dueAt);
        WorkItem saved = workItemRepository.save(item);
        appendLedger("GABINETE_HANDOFF_ASSESSORIA", processoId, actor, routing, saved.getTemplateCode(), tarefa, observacao);
        return new HandoffAction(
                saved.getId(),
                processoId,
                numeroProcesso(processo),
                actor.getId(),
                actor.getNome(),
                "GABINETE_PARA_ASSESSORIA",
                routing,
                saved.getTemplateCode(),
                saved.getQueueCode(),
                saved.getInboxKey(),
                saved.getDueAt(),
                List.of(
                        "Handoff formal registrado para a assessoria.",
                        "Lane de assessoria e inbox do gabinete aplicados.",
                        "Guard rails do magistrado validados antes do encaminhamento."
                )
        );
    }

    @Transactional
    public HandoffAction devolverParaGabinete(Long processoId, String observacao) {
        Usuario actor = requireAssessorActor();
        Processo processo = loadProcesso(processoId);
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        AssessorGabineteGuardRailService.AssessorProcessoGuardRailSnapshot guard = assessorGuardRailService.avaliarProcesso(processoId);
        if (!"ASSESSORIA_LIBERADA_PARA_MINUTACAO".equals(guard.recommendedAction()) && !"REVISAR_RETORNO_ASSESSORIA".equals(guard.recommendedAction())
                && guard.compatibleItems().isEmpty()) {
            throw new AccessDeniedPjbException("A assessoria não possui lane compatível e handoff formal para devolver a minuta ao gabinete.");
        }
        List<WorkItem> activeItems = workItemRepository.findAllByProcesso(processoId).stream().filter(this::isActive).toList();
        WorkItem judgeCapture = findActiveJudgeCapture(activeItems, routing);
        concludeMatching(activeItems, item -> startsWith(item.getTemplateCode(), handoffPrefixGabineteAssessoria(routing, processoId), handoffPrefixAssessoriaGabinete(routing, processoId))
                || matchesAny(item.getQueueCode(), routing.advisoryDesk()));
        String templateCode = handoffPrefixAssessoriaGabinete(routing, processoId);
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, templateCode)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        Instant dueAt = resolveDueInstant(guard.routing().captureSla(), 4);
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(WorkItemType.DISTRIBUICAO);
        item.setTitulo("Retorno da assessoria para o gabinete — " + numeroProcesso(processo));
        item.setDescricao(buildGabineteReturnDescription(actor, processo, routing, observacao));
        item.setQueueCode(routing.gabineteDesk());
        item.setInboxKey(routing.gabineteInboxKey());
        item.setAssignedRole(judgeCapture != null && judgeCapture.getAssignedRole() != null ? judgeCapture.getAssignedRole() : TipoUsuario.MAGISTRADO);
        item.setAssignedUser(judgeCapture == null ? null : judgeCapture.getAssignedUser());
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(resolvePriority(processo));
        item.setBlocking(true);
        item.setUf(firstNonBlank(processo.getUf(), actor.getUf()));
        item.setComarca(firstNonBlank(processo.getComarca(), actor.getComarca()));
        item.setBaseLegal("Retorno formal da assessoria para revisão do gabinete, preservando trilha topológica e histórico de reencaminhamento.");
        item.setDueAt(dueAt);
        WorkItem saved = workItemRepository.save(item);
        appendLedger("ASSESSORIA_RETORNO_GABINETE", processoId, actor, routing, saved.getTemplateCode(), null, observacao);
        return new HandoffAction(
                saved.getId(),
                processoId,
                numeroProcesso(processo),
                actor.getId(),
                actor.getNome(),
                "ASSESSORIA_PARA_GABINETE",
                routing,
                saved.getTemplateCode(),
                saved.getQueueCode(),
                saved.getInboxKey(),
                saved.getDueAt(),
                List.of(
                        "Retorno formal da assessoria para o gabinete registrado.",
                        "Inbox do gabinete restaurado para revisão do magistrado.",
                        "Histórico auditável do reencaminhamento preservado."
                )
        );
    }

    @Transactional
    public HandoffAction encaminharParaSecretaria(Long processoId, String destino, String observacao) {
        Usuario actor = requireJudgeActor();
        Processo processo = loadProcesso(processoId);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = juizGuardRailService.requireAtuacaoPermitida(processo, actor, "HANDOFF_GABINETE_PARA_SECRETARIA");
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        List<WorkItem> activeItems = workItemRepository.findAllByProcesso(processoId).stream().filter(this::isActive).toList();
        WorkItem judgeCapture = findActiveJudgeCapture(activeItems, routing);
        if (judgeCapture == null || judgeCapture.getAssignedUser() == null || !Objects.equals(judgeCapture.getAssignedUser().getId(), actor.getId())) {
            throw new AccessDeniedPjbException("A liberação para a secretaria exige captura ativa do magistrado competente.");
        }
        activeItems.stream()
                .filter(item -> Objects.equals(item.getAssignedUser() == null ? null : item.getAssignedUser().getId(), actor.getId()))
                .forEach(this::concludeItem);
        concludeMatching(activeItems, item -> startsWith(item.getTemplateCode(), handoffPrefixGabineteAssessoria(routing, processoId), handoffPrefixAssessoriaGabinete(routing, processoId), handoffPrefixGabineteSecretaria(routing, processoId)));

        String stage = normalizeStage(destino);
        String templateCode = handoffPrefixGabineteSecretaria(routing, processoId) + ':' + stage;
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, templateCode)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        SecretariatOperationalRoutingProfile secretariat = routing.secretariatRouting();
        Instant dueAt = switch (stage) {
            case "SANEAMENTO" -> resolveDueInstant(secretariat.saneamentoSla(), 8);
            case "AUDIENCIA" -> resolveDueInstant(secretariat.audiencePreparationSla(), 8);
            default -> resolveDueInstant(secretariat.receiptSla(), 8);
        };
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(WorkItemType.DISTRIBUICAO);
        item.setTitulo("Handoff do gabinete para a secretaria — " + stage + " — " + numeroProcesso(processo));
        item.setDescricao(buildSecretariatDescription(actor, processo, routing, stage, observacao));
        item.setQueueCode(resolveSecretariatQueue(stage, secretariat));
        item.setInboxKey(resolveSecretariatInbox(stage, secretariat));
        item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        item.setAssignedUser(null);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(resolvePriority(processo));
        item.setBlocking(false);
        item.setUf(firstNonBlank(processo.getUf(), actor.getUf()));
        item.setComarca(firstNonBlank(processo.getComarca(), actor.getComarca()));
        item.setBaseLegal("Handoff formal do gabinete para a secretaria topológica correta, com histórico de reencaminhamento e fila compatível.");
        item.setDueAt(dueAt);
        WorkItem saved = workItemRepository.save(item);
        appendLedger("GABINETE_HANDOFF_SECRETARIA", processoId, actor, routing, saved.getTemplateCode(), stage, observacao);
        return new HandoffAction(
                saved.getId(),
                processoId,
                numeroProcesso(processo),
                actor.getId(),
                actor.getNome(),
                "GABINETE_PARA_SECRETARIA",
                routing,
                saved.getTemplateCode(),
                saved.getQueueCode(),
                saved.getInboxKey(),
                saved.getDueAt(),
                List.of(
                        "Handoff formal para a secretaria registrado.",
                        "Fila cartorária compatível aplicada para o estágio " + stage + '.',
                        "Histórico de reencaminhamento preservado no ledger e no work item."
                )
        );
    }

    private List<HandoffHistoryEntry> buildHistory(List<WorkItem> items, JuizGabineteRoutingProfile routing) {
        return items.stream()
                .filter(item -> item.getTemplateCode() != null)
                .filter(item -> startsWith(item.getTemplateCode(), "HANDOFF:", routing.captureTemplateCode(item.getProcessoId()), routing.releaseTemplateCode("EXECUCAO", item.getProcessoId()), routing.releaseTemplateCode("SANEAMENTO", item.getProcessoId()), routing.releaseTemplateCode("AUDIENCIA", item.getProcessoId())))
                .sorted(Comparator.comparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> new HandoffHistoryEntry(
                        item.getId(),
                        item.getTemplateCode(),
                        item.getCreatedAt(),
                        item.getUpdatedAt(),
                        item.getQueueCode(),
                        item.getInboxKey(),
                        item.getAssignedRole() == null ? null : item.getAssignedRole().name(),
                        item.getAssignedUser() == null ? null : item.getAssignedUser().getNome(),
                        item.getStatus() == null ? null : item.getStatus().name(),
                        summarize(item.getDescricao(), item.getBaseLegal(), item.getTitulo())
                ))
                .toList();
    }

    private void concludeMatching(List<WorkItem> activeItems, java.util.function.Predicate<WorkItem> predicate) {
        activeItems.stream().filter(predicate).forEach(this::concludeItem);
    }

    private void concludeItem(WorkItem item) {
        item.setStatus(WorkItemStatus.CONCLUIDO);
        item.setBlocking(false);
        workItemRepository.save(item);
    }

    private WorkItem findActiveJudgeCapture(List<WorkItem> activeItems, JuizGabineteRoutingProfile routing) {
        return activeItems.stream()
                .filter(item -> startsWith(item.getTemplateCode(), "JUIZ:GABINETE:CAPTURA:"))
                .filter(item -> matchesAny(item.getQueueCode(), routing.gabineteDesk()))
                .findFirst()
                .orElse(null);
    }

    private WorkItem findActiveByPrefix(List<WorkItem> activeItems, String prefix) {
        return activeItems.stream()
                .filter(item -> startsWith(item.getTemplateCode(), prefix))
                .findFirst()
                .orElse(null);
    }

    private HandoffLaneView toLaneView(WorkItem item, JuizGabineteRoutingProfile routing, Usuario actor) {
        String lane = resolveLane(item, routing);
        boolean assignedToActor = item.getAssignedUser() != null && Objects.equals(item.getAssignedUser().getId(), actor.getId());
        boolean compatible = switch (lane) {
            case "GABINETE" -> item.getAssignedUser() == null || assignedToActor || !item.getAssignedUser().isMagistrado();
            case "ASSESSORIA" -> startsWith(item.getTemplateCode(), "HANDOFF:GABINETE:ASSESSORIA:", "HANDOFF:ASSESSORIA:GABINETE:");
            case "SECRETARIA" -> true;
            default -> false;
        };
        return new HandoffLaneView(
                item.getId(),
                lane,
                item.getTemplateCode(),
                item.getTitulo(),
                item.getQueueCode(),
                item.getInboxKey(),
                item.getAssignedRole() == null ? null : item.getAssignedRole().name(),
                item.getAssignedUser() == null ? null : item.getAssignedUser().getId(),
                item.getAssignedUser() == null ? null : item.getAssignedUser().getNome(),
                item.getStatus() == null ? null : item.getStatus().name(),
                item.getDueAt(),
                item.isBlocking(),
                compatible,
                lane.equals("DRIFT") ? "FORA_DA_TOPOLOGIA" : "OK"
        );
    }

    private String resolveLane(WorkItem item, JuizGabineteRoutingProfile routing) {
        if (matchesAny(item.getQueueCode(), routing.gabineteDesk(), routing.coordinationDesk(), routing.hearingDesk(), routing.redistributionDesk())
                || matchesAny(item.getInboxKey(), routing.gabineteInboxKey())) {
            if (matchesAny(item.getQueueCode(), routing.advisoryDesk()) || startsWith(item.getTemplateCode(), "HANDOFF:GABINETE:ASSESSORIA:", "HANDOFF:ASSESSORIA:GABINETE:")) {
                return "ASSESSORIA";
            }
            return "GABINETE";
        }
        SecretariatOperationalRoutingProfile secretariat = routing.secretariatRouting();
        if (matchesAny(item.getQueueCode(), secretariat.receiptQueueCode(), secretariat.saneamentoQueueCode(), secretariat.audienceQueueCode(), secretariat.executionQueueCode())
                || matchesAny(item.getInboxKey(), secretariat.receiptInboxKey(), secretariat.saneamentoInboxKey(), secretariat.audienceInboxKey(), secretariat.executionInboxKey())) {
            return "SECRETARIA";
        }
        return "DRIFT";
    }

    private String resolveRecommendedAction(Usuario actor,
                                            WorkItem judgeCapture,
                                            WorkItem gabineteToAssessoria,
                                            WorkItem assessoriaToGabinete,
                                            WorkItem secretariaPending,
                                            boolean noDrift,
                                            boolean judgeGuardAllowed) {
        if (!judgeGuardAllowed && actor.isMagistrado()) {
            return "BLOQUEAR_ATUACAO_DO_GABINETE";
        }
        if (actor.isMagistrado() && judgeCapture == null) {
            return "CAPTURAR_PROCESSO_NO_GABINETE";
        }
        if (actor.isMagistrado() && gabineteToAssessoria == null && assessoriaToGabinete == null) {
            return "ENCAMINHAR_FORMALMENTE_PARA_ASSESSORIA";
        }
        if (actor.isAssessor() && gabineteToAssessoria == null) {
            return "AGUARDAR_HANDOFF_FORMAL_DO_GABINETE";
        }
        if (actor.isAssessor() && assessoriaToGabinete == null) {
            return "DEVOLVER_MINUTA_AO_GABINETE";
        }
        if (actor.isMagistrado() && secretariaPending == null && assessoriaToGabinete != null) {
            return "REVISAR_RETORNO_E_ENCAMINHAR_SECRETARIA";
        }
        if (!noDrift) {
            return "ESTABILIZAR_FLUXO_TOPOLOGICO";
        }
        return "HANDOFF_TOPOLOGICO_ESTAVEL";
    }

    private void appendLedger(String eventCode,
                              Long processoId,
                              Usuario actor,
                              JuizGabineteRoutingProfile routing,
                              String templateCode,
                              String qualifier,
                              String observacao) {
        String payload = String.join("#",
                String.valueOf(processoId),
                String.valueOf(actor.getId()),
                safe(eventCode),
                safe(templateCode),
                safe(routing.routeKey()),
                safe(qualifier),
                safe(observacao));
        auditLedgerService.appendSafely(
                eventCode,
                "PROCESSO",
                String.valueOf(processoId),
                Hashes.sha256Hex(payload),
                "actor=" + actor.getId() + ", routeKey=" + routing.routeKey() + ", template=" + templateCode
        );
    }

    private String buildAssessoriaDescription(Usuario actor,
                                              Processo processo,
                                              JuizGabineteRoutingProfile routing,
                                              String tarefa,
                                              String observacao) {
        return "Gabinete encaminhou formalmente o processo " + numeroProcesso(processo)
                + " para a assessoria topológica " + safe(routing.advisoryDesk())
                + ". Tarefa: " + safe(firstNonBlank(tarefa, "MINUTA_DECISORIA"))
                + ". Observação: " + safe(firstNonBlank(observacao, "SEM_OBSERVACAO"))
                + ". Magistrado responsável: " + safe(actor.getNome()) + '.';
    }

    private String buildGabineteReturnDescription(Usuario actor,
                                                  Processo processo,
                                                  JuizGabineteRoutingProfile routing,
                                                  String observacao) {
        return "Assessoria devolveu formalmente o processo " + numeroProcesso(processo)
                + " para o gabinete " + safe(routing.gabineteDesk())
                + ". Observação: " + safe(firstNonBlank(observacao, "MINUTA_PRONTA_PARA_REVISAO"))
                + ". Assessor responsável: " + safe(actor.getNome()) + '.';
    }

    private String buildSecretariatDescription(Usuario actor,
                                               Processo processo,
                                               JuizGabineteRoutingProfile routing,
                                               String stage,
                                               String observacao) {
        return "Gabinete encaminhou formalmente o processo " + numeroProcesso(processo)
                + " para a secretaria topológica " + safe(routing.secretariatRouting().secretariatCode())
                + " no estágio " + stage
                + ". Observação: " + safe(firstNonBlank(observacao, "SEM_OBSERVACAO"))
                + ". Magistrado responsável: " + safe(actor.getNome()) + '.';
    }

    private TipoUsuario resolveAssessoriaRole(Usuario actor) {
        if (actor.getTipoUsuario() == TipoUsuario.MINISTRO) {
            return TipoUsuario.ASSESSOR_MINISTRO;
        }
        if (actor.getTipoUsuario() == TipoUsuario.DESEMBARGADOR || actor.getTipoUsuario() == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return TipoUsuario.ASSESSOR_DESEMBARGADOR;
        }
        return TipoUsuario.ASSESSOR_JUDICIAL;
    }

    private int resolvePriority(Processo processo) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != com.tcc.pjb.backend.model.entity.enums.NivelSigilo.PUBLICO) {
            return 1;
        }
        if (isUrgente(processo)) {
            return 1;
        }
        return 2;
    }

    private boolean isUrgente(Processo processo) {
        if (processo == null) {
            return false;
        }
        return containsAny(processo.getAssunto(), "URG", "LIMINAR", "TUTELA", "CAUTELAR", "CUSTODIA", "MEDIDA_PROTETIVA")
                || containsAny(processo.getClasseProcessual(), "HABEAS", "MANDADO_DE_SEGURANCA", "TUTELA")
                || containsAny(safeName(processo.getStatusProcesso()), "URG", "PLANTAO")
                || containsAny(safeName(processo.getFaseAtual()), "CUSTODIA");
    }

    private String handoffPrefixGabineteAssessoria(JuizGabineteRoutingProfile routing, Long processoId) {
        return "HANDOFF:GABINETE:ASSESSORIA:" + routing.routeKey() + ':' + processoId;
    }

    private String handoffPrefixAssessoriaGabinete(JuizGabineteRoutingProfile routing, Long processoId) {
        return "HANDOFF:ASSESSORIA:GABINETE:" + routing.routeKey() + ':' + processoId;
    }

    private String handoffPrefixGabineteSecretaria(JuizGabineteRoutingProfile routing, Long processoId) {
        return "HANDOFF:GABINETE:SECRETARIA:" + routing.routeKey() + ':' + processoId;
    }

    private boolean isActive(WorkItem item) {
        return item != null && (item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO);
    }


    private Instant resolveDueInstant(java.time.Duration preferred, long fallbackHours) {
        return preferred == null ? Instant.now().plus(fallbackHours, ChronoUnit.HOURS) : Instant.now().plus(preferred);
    }

    private String resolveSecretariatQueue(String stage, SecretariatOperationalRoutingProfile routing) {
        return switch (stage) {
            case "SANEAMENTO" -> firstNonBlank(routing.saneamentoQueueCode(), routing.executionQueueCode(), routing.receiptQueueCode());
            case "AUDIENCIA" -> firstNonBlank(routing.audienceQueueCode(), routing.executionQueueCode(), routing.receiptQueueCode());
            default -> firstNonBlank(routing.executionQueueCode(), routing.receiptQueueCode(), routing.saneamentoQueueCode());
        };
    }

    private String resolveSecretariatInbox(String stage, SecretariatOperationalRoutingProfile routing) {
        return switch (stage) {
            case "SANEAMENTO" -> firstNonBlank(routing.saneamentoInboxKey(), routing.executionInboxKey(), routing.receiptInboxKey());
            case "AUDIENCIA" -> firstNonBlank(routing.audienceInboxKey(), routing.executionInboxKey(), routing.receiptInboxKey());
            default -> firstNonBlank(routing.executionInboxKey(), routing.receiptInboxKey(), routing.saneamentoInboxKey());
        };
    }

    private Usuario requireInstitutionalActor() {
        Usuario actor = currentUserService.getRequired();
        if (actor.getTipoUsuario() == null || !actor.getTipoUsuario().isInstitucional()) {
            throw new AccessDeniedPjbException("Apenas perfis institucionais podem consultar o handoff topológico.");
        }
        return actor;
    }

    private Usuario requireJudgeActor() {
        Usuario actor = currentUserService.getRequired();
        if (actor.getTipoUsuario() == null || !actor.getTipoUsuario().isMagistratura()) {
            throw new AccessDeniedPjbException("Apenas magistratura pode encaminhar handoff do gabinete.");
        }
        return actor;
    }

    private Usuario requireAssessorActor() {
        Usuario actor = currentUserService.getRequired();
        if (actor.getTipoUsuario() == null || !actor.getTipoUsuario().isAssessor()) {
            throw new AccessDeniedPjbException("Apenas assessoria pode devolver handoff ao gabinete.");
        }
        return actor;
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private String normalizeStage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "EXECUCAO";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return switch (normalized) {
            case "SANEAMENTO", "RECEBIMENTO", "TRIAGEM" -> "SANEAMENTO";
            case "PAUTA", "AUDIENCIA", "AUDIENCIAS" -> "AUDIENCIA";
            default -> "EXECUCAO";
        };
    }

    private HandoffSignal signal(String code, String level, boolean satisfied, String message) {
        boolean blocking = "CRITICA".equals(level) || "ALTA".equals(level);
        return new HandoffSignal(code, level, blocking, satisfied, message);
    }

    private String summarize(String descricao, String baseLegal, String titulo) {
        return firstNonBlank(titulo, descricao, baseLegal);
    }

    private boolean startsWith(String candidate, String... prefixes) {
        if (candidate == null || prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && candidate.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAny(String candidate, String... expected) {
        String normalizedCandidate = normalize(candidate);
        if (normalizedCandidate.isEmpty() || expected == null || expected.length == 0) {
            return false;
        }
        for (String value : expected) {
            if (!normalize(value).isEmpty() && Objects.equals(normalizedCandidate, normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private String numeroProcesso(Processo processo) {
        return firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), "PROCESSO");
    }

    private boolean containsAny(String source, String... tokens) {
        String normalized = normalize(source);
        if (normalized.isBlank() || tokens == null || tokens.length == 0) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && normalized.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private String safeName(Enum<?> value) {
        return value == null ? "" : value.name();
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

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    public record HandoffSnapshot(
            Long actorId,
            String actorNome,
            String actorRole,
            Long processoId,
            String numeroProcesso,
            JuizGabineteRoutingProfile routing,
            JuizProcessoGuardRailService.GuardRailSnapshot judgeGuardRail,
            AssessorGabineteGuardRailService.AssessorProcessoGuardRailSnapshot assessorGuardRail,
            List<HandoffLaneView> gabineteItems,
            List<HandoffLaneView> assessoriaItems,
            List<HandoffLaneView> secretariaItems,
            List<HandoffLaneView> driftItems,
            List<HandoffHistoryEntry> history,
            List<HandoffSignal> signals,
            String recommendedAction,
            Map<String, Object> metrics
    ) {
    }

    public record HandoffAction(
            Long workItemId,
            Long processoId,
            String numeroProcesso,
            Long actorId,
            String actorNome,
            String action,
            JuizGabineteRoutingProfile routing,
            String templateCode,
            String queueCode,
            String inboxKey,
            Instant dueAt,
            List<String> effects
    ) {
    }

    public record HandoffLaneView(
            Long id,
            String lane,
            String templateCode,
            String titulo,
            String queueCode,
            String inboxKey,
            String assignedRole,
            Long assignedUserId,
            String assignedUserNome,
            String status,
            Instant dueAt,
            boolean blocking,
            boolean compatible,
            String reason
    ) {
    }

    public record HandoffHistoryEntry(
            Long id,
            String templateCode,
            Instant createdAt,
            Instant updatedAt,
            String queueCode,
            String inboxKey,
            String assignedRole,
            String assignedUserNome,
            String status,
            String summary
    ) {
    }

    public record HandoffSignal(
            String code,
            String level,
            boolean blocking,
            boolean satisfied,
            String message
    ) {
    }
}
