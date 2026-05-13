package com.tcc.pjb.backend.service.forum;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.ForumOfficialReturnReactivationRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaContextEnvelopeService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaNotificationCenterService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInboxAccessService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ForumOfficialReturnOperationalService {

    private static final List<TipoUsuario> OFFICIAL_ROLES = List.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);
    private static final List<WorkItemStatus> ACTIVE_STATUSES = List.of(WorkItemStatus.PENDENTE, WorkItemStatus.EM_EXECUCAO);

    private final WorkItemRepository workItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUserService currentUserService;
    private final SecretariatInboxAccessService inboxAccessService;
    private final InstitutionalActorRoutingService routingService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final OficialJusticaNotificationCenterService notificationCenterService;
    private final PainelServiceCommons commons;
    private final PjbTimeService timeService;

    public ForumOfficialReturnOperationalService(WorkItemRepository workItemRepository,
                                                 UsuarioRepository usuarioRepository,
                                                 CurrentUserService currentUserService,
                                                 SecretariatInboxAccessService inboxAccessService,
                                                 InstitutionalActorRoutingService routingService,
                                                 OficialJusticaContextEnvelopeService contextEnvelopeService,
                                                 OficialJusticaNotificationCenterService notificationCenterService,
                                                 PainelServiceCommons commons,
                                                 PjbTimeService timeService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.inboxAccessService = Objects.requireNonNull(inboxAccessService);
        this.routingService = Objects.requireNonNull(routingService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.notificationCenterService = Objects.requireNonNull(notificationCenterService);
        this.commons = Objects.requireNonNull(commons);
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> inbox(String inboxKey, int limit) {
        String normalizedInbox = inboxAccessService.requireAccess(inboxKey);
        int safeLimit = Math.max(1, Math.min(limit, 80));
        Usuario operador = currentUserService.getRequired();
        List<WorkItem> items = workItemRepository.findOfficialReturnDeskItemsByInbox(normalizedInbox, ACTIVE_STATUSES, PageRequest.of(0, safeLimit));
        List<Map<String, Object>> rows = items.stream().map(this::toDeskRow).toList();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", timeService.nowUtc());
        out.put("mode", "FORUM_OFFICIAL_RETURN_INBOX_V1");
        out.put("inboxKey", normalizedInbox);
        out.put("total", workItemRepository.countOfficialReturnDeskItemsByInbox(normalizedInbox, ACTIVE_STATUSES));
        out.put("visible", rows.size());
        out.put("pendentes", items.stream().filter(item -> item.getStatus() == WorkItemStatus.PENDENTE).count());
        out.put("emExecucao", items.stream().filter(item -> item.getStatus() == WorkItemStatus.EM_EXECUCAO).count());
        out.put("processosDistintos", items.stream().map(WorkItem::getProcessoId).filter(Objects::nonNull).distinct().count());
        out.put("oficiaisDistintos", items.stream().map(this::resolveTargetOfficial).filter(Objects::nonNull).map(Usuario::getId).filter(Objects::nonNull).distinct().count());
        out.put("unidadeOperadora", Map.of(
                "usuarioId", operador.getId(),
                "usuarioNome", firstNonBlank(operador.getNome(), "USUARIO_NAO_IDENTIFICADO"),
                "perfil", operador.getTipoUsuario() != null ? operador.getTipoUsuario().name() : "SERVIDOR_JUDICIARIO",
                "inboxKey", normalizedInbox
        ));
        out.put("items", rows);
        out.put("reativacaoPathTemplate", OperationalApiRoutes.FORUM_BASE + OperationalApiRoutes.PATH_FORUM_OFFICIAL_RETURN_REACTIVATE);
        return safeCopy(out);
    }

    @Transactional
    public Map<String, Object> reativar(Long deskWorkItemId, ForumOfficialReturnReactivationRequest request) {
        WorkItem deskItem = workItemRepository.findById(deskWorkItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "retorno do oficial não encontrado"));
        validateDeskItem(deskItem);
        inboxAccessService.requireAccess(deskItem.getInboxKey());
        Usuario operador = currentUserService.getRequired();
        Processo processo = Objects.requireNonNull(deskItem.getProcesso(), "processo requerido");
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isEncerrado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "processo encerrado exige reabertura processual antes da reintimação do oficial");
        }
        Usuario oficial = resolveOfficialTarget(request, deskItem);
        TipoUsuario officialRole = officialRole(oficial);
        InstitutionalActorRoutingService.InstitutionalRoute route = routingService.officialJustice(processo.getId(), officialRole == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "REINTIMACAO_OFICIAL");
        WorkItem officialItem = reactivateOrCreateOfficialItem(deskItem, processo, oficial, route, request);
        boolean dispatched = shouldDispatchStandardReactivation(request)
                && notificationCenterService.dispatchProcessReactivation(officialItem, request == null ? null : request.origemReativacaoResolvida());
        if (request == null || !request.manterDeskAbertoResolvido()) {
            deskItem.setStatus(WorkItemStatus.CONCLUIDO);
        } else {
            deskItem.setStatus(WorkItemStatus.EM_EXECUCAO);
        }
        deskItem.setSemInteresse(true);
        workItemRepository.save(deskItem);
        String event = "REINTIMACAO_AUDITAVEL_OFICIAL";
        String message = "Retorno do Oficial consumido pela unidade e reativado no processo " + processNumber(processo) + " para " + firstNonBlank(oficial.getNome(), "OFICIAL_NAO_IDENTIFICADO") + ".";
        commons.publishUserHistory(operador, "FORUM", event, message, processo, officialItem.getId());
        commons.publishUserHistory(oficial, "OFICIAL", event, message, processo, officialItem.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REATIVADO_COM_SUCESSO");
        out.put("reactivationAuditHash", auditHash(deskItem, officialItem, operador, request));
        out.put("generatedAt", timeService.nowUtc());
        out.put("deskWorkItemId", deskItem.getId());
        out.put("deskStatus", deskItem.getStatus().name());
        out.put("processoId", processo.getId());
        out.put("processoNumero", processNumber(processo));
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(oficial, null));
        out.put("processoContexto", contextEnvelopeService.processEnvelope(oficial, processo, officialItem, null, null));
        LinkedHashMap<String, Object> reativacao = new LinkedHashMap<>();
        reativacao.put("workItemId", officialItem.getId());
        reativacao.put("templateCode", firstNonBlank(officialItem.getTemplateCode(), "REATIVACAO_OFICIAL_REINTIMACAO"));
        reativacao.put("queueCode", firstNonBlank(officialItem.getQueueCode(), route.queueCode(), "OFICIAL_REINTIMACAO_QUEUE"));
        reativacao.put("inboxKey", firstNonBlank(officialItem.getInboxKey(), route.inboxKey(), "OFICIAL_REINTIMACAO_INBOX"));
        reativacao.put("dueAt", officialItem.getDueAt());
        reativacao.put("status", officialItem.getStatus() != null ? officialItem.getStatus().name() : null);
        reativacao.put("notificacaoInstantanea", dispatched);
        reativacao.put("painelPath", OperationalApiRoutes.oficialJusticaNamedProcessWorkbench(processo.getId()));
        reativacao.put("agendaPath", OperationalApiRoutes.oficialJusticaAgendaOperacional());
        reativacao.put("balcaoVirtualPath", OperationalApiRoutes.oficialJusticaBalcaoVirtualRoom(processo.getId()));
        out.put("reativacao", safeCopy(reativacao));
        out.put("origemReativacao", request == null ? "SECRETARIA" : request.origemReativacaoResolvida());
        out.put("reativacaoAuditavel", Boolean.TRUE);
        out.put("reapareceNoPainelDoOficial", Boolean.TRUE);
        out.put("botaoCientePath", OperationalApiRoutes.oficialJusticaCienteIntimacao(processo.getId()));
        return safeCopy(out);
    }

    @Transactional
    public Map<String, Object> reativarPorExpedicaoAutomatica(Processo processo,
                                                              ForumOfficialReturnReactivationRequest request,
                                                              String destinatario,
                                                              String conteudo) {
        if (processo == null || processo.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "processo inválido para reativação automática do oficial");
        }
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isEncerrado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "processo encerrado exige reabertura processual antes da reintimação automática do oficial");
        }
        Usuario operador = currentUserService.getRequired();
        WorkItem deskItem = findDeskReturnCandidate(processo.getId(), request == null ? null : request.oficialId()).orElse(null);
        Usuario oficial = resolveAutomaticOfficialTarget(processo.getId(), request, deskItem);
        TipoUsuario officialRole = officialRole(oficial);
        InstitutionalActorRoutingService.InstitutionalRoute route = routingService.officialJustice(processo.getId(), officialRole == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "REINTIMACAO_OFICIAL_EXPEDICAO");
        WorkItem officialItem = reactivateOrCreateOfficialItem(deskItem, processo, oficial, route, request);
        if (conteudo != null && !conteudo.isBlank()) {
            String baseDescricao = firstNonBlank(officialItem.getDescricao(), "Reintimação automática do Oficial de Justiça.");
            officialItem.setDescricao(trim(baseDescricao + " Conteúdo da expedição: " + conteudo, 1900));
            officialItem = workItemRepository.save(officialItem);
        }
        if (deskItem != null) {
            deskItem.setStatus(request != null && request.manterDeskAbertoResolvido() ? WorkItemStatus.EM_EXECUCAO : WorkItemStatus.CONCLUIDO);
            deskItem.setSemInteresse(true);
            workItemRepository.save(deskItem);
        }
        boolean dispatched = shouldDispatchStandardReactivation(request)
                && notificationCenterService.dispatchProcessReactivation(officialItem, request == null ? null : request.origemReativacaoResolvida());
        String message = "Reintimação automática do Oficial materializada pela expedição do processo " + processNumber(processo) + " para " + firstNonBlank(oficial.getNome(), "OFICIAL_NAO_IDENTIFICADO") + ".";
        commons.publishUserHistory(operador, "SECRETARIA", "REINTIMACAO_AUTOMATICA_OFICIAL", message, processo, officialItem.getId());
        commons.publishUserHistory(oficial, "OFICIAL", "REINTIMACAO_AUTOMATICA_OFICIAL", message, processo, officialItem.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REATIVACAO_AUTOMATICA_CONCLUIDA");
        out.put("mode", "SECRETARIA_GABINETE_AUTO_REINTIMACAO_V1");
        out.put("reactivationAuditHash", auditHash(deskItem, officialItem, operador, request));
        out.put("generatedAt", timeService.nowUtc());
        out.put("destinatarioExpedicao", firstNonBlank(destinatario, "OFICIAL_DE_JUSTICA"));
        out.put("processoId", processo.getId());
        out.put("processoNumero", processNumber(processo));
        out.put("deskWorkItemId", deskItem != null ? deskItem.getId() : null);
        out.put("deskConsumido", deskItem != null);
        out.put("deskStatus", deskItem != null && deskItem.getStatus() != null ? deskItem.getStatus().name() : null);
        out.put("origemReativacao", request == null ? "SECRETARIA" : request.origemReativacaoResolvida());
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(oficial, null));
        out.put("processoContexto", contextEnvelopeService.processEnvelope(oficial, processo, officialItem, null, null));
        LinkedHashMap<String, Object> reativacao = new LinkedHashMap<>();
        reativacao.put("workItemId", officialItem.getId());
        reativacao.put("templateCode", firstNonBlank(officialItem.getTemplateCode(), "REATIVACAO_OFICIAL_REINTIMACAO"));
        reativacao.put("queueCode", firstNonBlank(officialItem.getQueueCode(), route.queueCode(), "OFICIAL_REINTIMACAO_QUEUE"));
        reativacao.put("inboxKey", firstNonBlank(officialItem.getInboxKey(), route.inboxKey(), "OFICIAL_REINTIMACAO_INBOX"));
        reativacao.put("status", officialItem.getStatus() != null ? officialItem.getStatus().name() : null);
        reativacao.put("dueAt", officialItem.getDueAt());
        reativacao.put("notificacaoInstantanea", dispatched);
        reativacao.put("reapareceNoPainelDoOficial", Boolean.TRUE);
        reativacao.put("painelPath", OperationalApiRoutes.oficialJusticaNamedProcessWorkbench(processo.getId()));
        reativacao.put("agendaPath", OperationalApiRoutes.oficialJusticaAgendaOperacional());
        reativacao.put("balcaoVirtualPath", OperationalApiRoutes.oficialJusticaBalcaoVirtualRoom(processo.getId()));
        reativacao.put("botaoCientePath", OperationalApiRoutes.oficialJusticaCienteIntimacao(processo.getId()));
        out.put("reativacao", safeCopy(reativacao));
        return safeCopy(out);
    }

    private boolean shouldDispatchStandardReactivation(ForumOfficialReturnReactivationRequest request) {
        String origin = request == null ? null : request.origemReativacaoResolvida();
        if (origin == null || origin.isBlank()) {
            return true;
        }
        return !origin.contains("JUIZO_DECISORIO") && !origin.contains("GABINETE_JUDICIAL");
    }

    private WorkItem reactivateOrCreateOfficialItem(WorkItem deskItem,
                                                    Processo processo,
                                                    Usuario oficial,
                                                    InstitutionalActorRoutingService.InstitutionalRoute route,
                                                    ForumOfficialReturnReactivationRequest request) {
        String templateCode = reactivationTemplateCode(deskItem, processo, oficial, request);
        Optional<WorkItem> existing = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode);
        if (existing.isPresent()) {
            WorkItem present = existing.get();
            if (present.getStatus() == WorkItemStatus.CANCELADO) {
                present.setStatus(WorkItemStatus.PENDENTE);
            }
            present.setSemInteresse(false);
            present.setAssignedUser(oficial);
            present.setAssignedRole(officialRole(oficial));
            present.setQueueCode(firstNonBlank(route.queueCode(), present.getQueueCode()));
            present.setInboxKey(firstNonBlank(route.inboxKey(), present.getInboxKey()));
            present.setDueAt(resolveDueAt(deskItem, request, present.getDueAt()));
            present.setBaseLegal(buildBaseLegal(processo, route, request));
            return workItemRepository.save(present);
        }
        List<WorkItem> historicalOfficialItems = workItemRepository.findAllByProcesso(processo.getId()).stream()
                .filter(item -> item.getAssignedUser() != null)
                .filter(item -> item.getAssignedUser().getId() != null && Objects.equals(item.getAssignedUser().getId(), oficial.getId()))
                .filter(item -> item.getAssignedRole() != null && OFFICIAL_ROLES.contains(item.getAssignedRole()))
                .toList();
        ArrayList<WorkItem> dirty = new ArrayList<>();
        for (WorkItem item : historicalOfficialItems) {
            boolean active = item.getStatus() != WorkItemStatus.CONCLUIDO && item.getStatus() != WorkItemStatus.CANCELADO;
            boolean otherReactivationCycle = item.getTemplateCode() != null
                    && item.getTemplateCode().startsWith("REATIVACAO_OFICIAL_REINTIMACAO:")
                    && !templateCode.equals(item.getTemplateCode());
            if (otherReactivationCycle && active) {
                item.setSemInteresse(true);
                item.setStatus(WorkItemStatus.CONCLUIDO);
                dirty.add(item);
                continue;
            }
            if (item.isSemInteresse() && active) {
                item.setSemInteresse(false);
                dirty.add(item);
            }
        }
        if (!dirty.isEmpty()) {
            workItemRepository.saveAll(dirty);
        }
        WorkItem created = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(templateCode)
                .type(WorkItemType.DILIGENCIA)
                .titulo(buildTitle(processo, oficial, request))
                .descricao(buildDescription(deskItem, processo, oficial, request))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(officialRole(oficial))
                .assignedUser(oficial)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(resolvePriority(deskItem))
                .blocking(false)
                .dueAt(resolveDueAt(deskItem, request, deskItem.getDueAt()))
                .uf(firstNonBlank(processo.getUf(), oficial.getUf()))
                .comarca(firstNonBlank(processo.getComarca(), oficial.getComarca()))
                .baseLegal(buildBaseLegal(processo, route, request))
                .semInteresse(false)
                .build();
        return workItemRepository.save(created);
    }

    private Optional<WorkItem> findDeskReturnCandidate(Long processoId, Long oficialId) {
        if (processoId == null) {
            return Optional.empty();
        }
        return workItemRepository.findAllByProcesso(processoId).stream()
                .filter(item -> item.getTemplateCode() != null && item.getTemplateCode().startsWith("BALCAO_OFICIAL_AUTODESTINO:"))
                .filter(item -> item.getStatus() != WorkItemStatus.CANCELADO)
                .filter(item -> oficialId == null || matchesOfficial(item, oficialId))
                .sorted(Comparator.comparing(WorkItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
    }

    private boolean matchesOfficial(WorkItem item, Long oficialId) {
        if (item == null || oficialId == null || item.getProcessoId() == null) {
            return false;
        }
        return workItemRepository.findAllByProcesso(item.getProcessoId()).stream()
                .filter(candidate -> candidate.getAssignedUser() != null && candidate.getAssignedUser().getId() != null)
                .filter(candidate -> candidate.getAssignedRole() != null && OFFICIAL_ROLES.contains(candidate.getAssignedRole()))
                .anyMatch(candidate -> Objects.equals(candidate.getAssignedUser().getId(), oficialId));
    }

    private Usuario resolveAutomaticOfficialTarget(Long processoId,
                                                   ForumOfficialReturnReactivationRequest request,
                                                   WorkItem deskItem) {
        if (request != null && request.oficialId() != null) {
            Usuario explicit = usuarioRepository.findById(request.oficialId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "oficial de justiça indicado não encontrado"));
            validateOfficial(explicit);
            return explicit;
        }
        Usuario fromDesk = resolveTargetOfficial(deskItem);
        if (fromDesk != null) {
            return fromDesk;
        }
        if (processoId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "não foi possível inferir o oficial responsável para a expedição automática");
        }
        return workItemRepository.findAllByProcesso(processoId).stream()
                .filter(item -> item.getAssignedUser() != null)
                .filter(item -> item.getAssignedRole() != null && OFFICIAL_ROLES.contains(item.getAssignedRole()))
                .sorted(Comparator.comparing(WorkItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(WorkItem::getAssignedUser)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "não foi possível inferir o oficial responsável; informe oficialId na expedição da reintimação"));
    }

    private String reactivationTemplateCode(WorkItem deskItem,
                                            Processo processo,
                                            Usuario oficial,
                                            ForumOfficialReturnReactivationRequest request) {
        String source = deskItem != null && deskItem.getId() != null ? String.valueOf(deskItem.getId()) : "AUTO_" + (processo != null ? processo.getId() : "SEM_PROCESSO");
        String due = request != null && request.dueAt() != null ? String.valueOf(request.dueAt().toEpochMilli()) : "SEM_DUE";
        return "REATIVACAO_OFICIAL_REINTIMACAO:" + source + ':' + oficial.getId() + ':' + due;
    }

    private void validateDeskItem(WorkItem deskItem) {
        if (deskItem.getTemplateCode() == null || !deskItem.getTemplateCode().startsWith("BALCAO_OFICIAL_AUTODESTINO:")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "work item informado não pertence ao balcão de retorno do oficial");
        }
        if (deskItem.getStatus() == WorkItemStatus.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "retorno do oficial cancelado não pode ser reativado");
        }
    }

    private Usuario resolveOfficialTarget(ForumOfficialReturnReactivationRequest request, WorkItem deskItem) {
        if (request != null && request.oficialId() != null) {
            Usuario explicit = usuarioRepository.findById(request.oficialId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "oficial de justiça indicado não encontrado"));
            validateOfficial(explicit);
            return explicit;
        }
        Usuario inferred = resolveTargetOfficial(deskItem);
        if (inferred != null) {
            return inferred;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "não foi possível inferir o oficial responsável; informe oficialId na reativação");
    }

    private Usuario resolveTargetOfficial(WorkItem deskItem) {
        if (deskItem == null || deskItem.getProcessoId() == null) {
            return null;
        }
        return workItemRepository.findAllByProcesso(deskItem.getProcessoId()).stream()
                .filter(item -> item.getAssignedUser() != null)
                .filter(item -> item.getAssignedRole() != null && OFFICIAL_ROLES.contains(item.getAssignedRole()))
                .sorted(Comparator.comparing(WorkItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(WorkItem::getAssignedUser)
                .findFirst()
                .orElse(null);
    }

    private void validateOfficial(Usuario usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getTipoUsuario() == null || !OFFICIAL_ROLES.contains(usuario.getTipoUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "usuário informado não é oficial de justiça válido");
        }
    }

    private TipoUsuario officialRole(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        return tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR ? TipoUsuario.OFICIAL_JUSTICA_AVALIADOR : TipoUsuario.OFICIAL_JUSTICA;
    }

    private Map<String, Object> toDeskRow(WorkItem deskItem) {
        Processo processo = deskItem.getProcesso();
        Usuario oficial = resolveTargetOfficial(deskItem);
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("deskWorkItemId", deskItem.getId());
        row.put("processoId", processo != null ? processo.getId() : null);
        row.put("processoNumero", processNumber(processo));
        row.put("titulo", deskItem.getTitulo());
        row.put("status", deskItem.getStatus() != null ? deskItem.getStatus().name() : null);
        row.put("prazoFatalEm", deskItem.getDueAt());
        row.put("queueCode", deskItem.getQueueCode());
        row.put("inboxKey", deskItem.getInboxKey());
        row.put("vara", processo != null ? processo.getVara() : null);
        row.put("forum", contextEnvelopeService.resolveForum(processo, contextEnvelopeService.resolveEsfera(oficial, processo, officialRole(oficial)), firstNonBlank(processo != null ? processo.getComarca() : null, oficial != null ? oficial.getComarca() : null)));
        row.put("cidade", firstNonBlank(processo != null ? processo.getComarca() : null, oficial != null ? oficial.getComarca() : null));
        row.put("uf", firstNonBlank(processo != null ? processo.getUf() : null, oficial != null ? oficial.getUf() : null));
        row.put("tribunal", contextEnvelopeService.resolveTribunalPrincipal(oficial, processo));
        row.put("regiaoJudicial", contextEnvelopeService.resolveRegiaoJudicial(contextEnvelopeService.resolveTribunalPrincipal(oficial, processo), firstNonBlank(processo != null ? processo.getUf() : null, oficial != null ? oficial.getUf() : null), contextEnvelopeService.resolveEsfera(oficial, processo, officialRole(oficial))));
        row.put("compartimento", compartmentFromTemplate(deskItem.getTemplateCode()));
        row.put("oficialResponsavel", oficial != null ? contextEnvelopeService.oficialEnvelope(oficial, null) : Map.of());
        row.put("reativacaoPath", OperationalApiRoutes.forumOfficialReturnReactivate(deskItem.getId()));
        row.put("reativacaoAuditavel", Boolean.TRUE);
        return safeCopy(row);
    }

    private String buildTitle(Processo processo, Usuario oficial, ForumOfficialReturnReactivationRequest request) {
        String origem = request == null ? "SECRETARIA" : request.origemReativacaoResolvida();
        return trim("Reintimação do Oficial de Justiça — " + processNumber(processo) + " — " + firstNonBlank(oficial != null ? oficial.getNome() : null, "OFICIAL_NAO_IDENTIFICADO") + " — " + origem, 220);
    }

    private String buildDescription(WorkItem deskItem, Processo processo, Usuario oficial, ForumOfficialReturnReactivationRequest request) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Reativação auditável da trilha do Oficial de Justiça a partir do balcão do fórum/tribunal.");
        parts.add("Processo: " + processNumber(processo) + '.');
        parts.add("Oficial responsável: " + firstNonBlank(oficial != null ? oficial.getNome() : null, "OFICIAL_NAO_IDENTIFICADO") + '.');
        parts.add("Vara: " + firstNonBlank(processo != null ? processo.getVara() : null, "VARA_NAO_IDENTIFICADA") + '.');
        parts.add("Fórum/unidade: " + contextEnvelopeService.resolveForum(processo, contextEnvelopeService.resolveEsfera(oficial, processo, officialRole(oficial)), firstNonBlank(processo != null ? processo.getComarca() : null, oficial != null ? oficial.getComarca() : null)) + '.');
        parts.add("Tribunal: " + contextEnvelopeService.resolveTribunalPrincipal(oficial, processo) + '.');
        parts.add("Origem da reintimação: " + (request == null ? "SECRETARIA" : request.origemReativacaoResolvida()) + '.');
        if (request != null && request.fundamentoResolvido() != null) {
            parts.add("Fundamento: " + request.fundamentoResolvido() + '.');
        }
        if (request != null && request.observacaoResolvida() != null) {
            parts.add("Observação operacional: " + request.observacaoResolvida() + '.');
        }
        if (deskItem != null && deskItem.getDescricao() != null && !deskItem.getDescricao().isBlank()) {
            parts.add("Resumo do retorno consumido: " + trim(deskItem.getDescricao(), 900));
        }
        return trim(parts.stream().filter(Objects::nonNull).collect(Collectors.joining(" ")), 1900);
    }

    private String buildBaseLegal(Processo processo,
                                  InstitutionalActorRoutingService.InstitutionalRoute route,
                                  ForumOfficialReturnReactivationRequest request) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Reativação auditável do Oficial por nova cadeia de intimação/cumprimento processual.");
        parts.add("Rota institucional: " + firstNonBlank(route.routeAxis(), "REINTIMACAO_OFICIAL") + '.');
        if (request != null && request.fundamentoResolvido() != null) {
            parts.add(request.fundamentoResolvido());
        }
        if (processo != null && processo.getStatusProcesso() != null) {
            parts.add("Status processual atual: " + processo.getStatusProcesso().name() + '.');
        }
        return trim(parts.stream().filter(Objects::nonNull).collect(Collectors.joining(" ")), 1200);
    }

    private Instant resolveDueAt(WorkItem deskItem, ForumOfficialReturnReactivationRequest request, Instant fallback) {
        if (request != null && request.dueAt() != null) {
            return request.dueAt();
        }
        if (deskItem != null && deskItem.getDueAt() != null) {
            return deskItem.getDueAt();
        }
        if (fallback != null) {
            return fallback;
        }
        return timeService.nowUtc().plusSeconds(48 * 3600);
    }

    private int resolvePriority(WorkItem deskItem) {
        Integer value = deskItem == null ? null : deskItem.getPrioridade();
        if (value == null) {
            return 2;
        }
        return Math.max(1, Math.min(value, 5));
    }

    private String compartmentFromTemplate(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return "PROXIMA_DEMANDA";
        }
        int index = templateCode.lastIndexOf(':');
        if (index < 0 || index + 1 >= templateCode.length()) {
            return templateCode;
        }
        return templateCode.substring(index + 1);
    }

    private String auditHash(WorkItem deskItem,
                             WorkItem officialItem,
                             Usuario operador,
                             ForumOfficialReturnReactivationRequest request) {
        String material = String.join("|",
                String.valueOf(deskItem != null ? deskItem.getId() : null),
                String.valueOf(officialItem != null ? officialItem.getId() : null),
                String.valueOf(operador != null ? operador.getId() : null),
                request == null ? "SECRETARIA" : firstNonBlank(request.origemReativacaoResolvida(), "SECRETARIA"),
                String.valueOf(timeService.nowUtc().toEpochMilli()));
        return Hashes.sha256HexPrefix(material, 32);
    }

    private Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }

    private String processNumber(Processo processo) {
        return firstNonBlank(processo != null ? processo.getNumeroProcesso() : null, processo != null ? processo.getNumero() : null, processo != null ? processo.getNumeroUnificado() : null, "PROCESSO_NAO_IDENTIFICADO");
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
        int safe = Math.max(16, limit);
        if (normalized.length() <= safe) {
            return normalized;
        }
        return normalized.substring(0, safe - 1) + '…';
    }
}
