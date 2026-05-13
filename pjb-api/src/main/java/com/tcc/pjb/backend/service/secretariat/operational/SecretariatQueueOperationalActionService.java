package com.tcc.pjb.backend.service.secretariat.operational;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAttendanceRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueCompletionEventRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueOperationalActionResponse;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueParticipantNotificationRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueProcessReturnRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueVenueConfirmationRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecretariatQueueOperationalActionService {

    private final SecretariatQueueItemRepository repository;
    private final SecretariatInstitutionalVisibilityService visibilityService;
    private final ObjectMapper mapper;
    private final WorkItemRepository workItemRepository;
    private final ProcessoRepository processoRepository;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final OutboxPublisher outboxPublisher;
    private final OperationalNotificationProofService notificationProofService;

    public SecretariatQueueOperationalActionService(SecretariatQueueItemRepository repository,
                                                    SecretariatInstitutionalVisibilityService visibilityService,
                                                    ObjectMapper mapper,
                                                    WorkItemRepository workItemRepository,
                                                    ProcessoRepository processoRepository,
                                                    InstitutionalActorRoutingService institutionalActorRoutingService,
                                                    OutboxPublisher outboxPublisher,
                                                    OperationalNotificationProofService notificationProofService) {
        this.repository = Objects.requireNonNull(repository);
        this.visibilityService = Objects.requireNonNull(visibilityService);
        this.mapper = Objects.requireNonNull(mapper);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.notificationProofService = Objects.requireNonNull(notificationProofService);
    }

    @Transactional
    public SecretariatQueueOperationalActionResponse confirmVenue(Long workItemId, SecretariatQueueVenueConfirmationRequest request, String actor) {
        SecretariatQueueItem item = loadAccessibleItem(workItemId);
        Instant now = firstInstant(request == null ? null : request.confirmedAt(), Instant.now());
        LinkedHashMap<String, Object> metadata = parseMetadata(item.getMetadataJson());
        LinkedHashMap<String, Object> venue = ensureMutableMap(metadata, "venue");
        putIfPresent(venue, "modality", request == null ? null : request.modality());
        putIfPresent(venue, "locationLabel", request == null ? null : request.locationLabel());
        putIfPresent(venue, "roomLabel", request == null ? null : request.roomLabel());
        putIfPresent(venue, "virtualLink", request == null ? null : request.virtualLink());
        String confirmationStatus = firstNonBlank(normalizeCode(request == null ? null : request.confirmationStatus()), "LOCAL_CONFIRMADO");
        venue.put("confirmationStatus", confirmationStatus);
        venue.put("confirmedAt", now.toString());
        putIfPresent(metadata, "venueConfirmationStatus", confirmationStatus);
        touchExecutionState(item);
        Map<String, Object> checkpoint = appendAudit(metadata, "VENUE_CONFIRMATION", actor, now, compactMap(
            "confirmationStatus", confirmationStatus,
            "modality", venue.get("modality"),
            "locationLabel", venue.get("locationLabel"),
            "roomLabel", venue.get("roomLabel"),
            "virtualLink", venue.get("virtualLink"),
            "note", request == null ? null : request.note()
        ));
        return persistAndRespond(item, metadata, checkpoint, "VENUE_CONFIRMATION", null, null);
    }

    @Transactional
    public OperationalStepUpChallengeResponse issueParticipantNotificationChallenge(Long workItemId) {
        SecretariatQueueItem item = loadAccessibleItem(workItemId);
        Processo processo = loadProcesso(item.getProcessoId());
        return notificationProofService.issueChallenge(processo, workItemId, "SECRETARIAT_PARTICIPANT_NOTIFICATION", "SECRETARIAT", "SECRETARIA_JUDICIARIA");
    }

    @Transactional
    public SecretariatQueueOperationalActionResponse confirmParticipantNotification(Long workItemId, SecretariatQueueParticipantNotificationRequest request, String actor) {
        SecretariatQueueItem item = loadAccessibleItem(workItemId);
        Instant now = firstInstant(request == null ? null : request.notifiedAt(), Instant.now());
        LinkedHashMap<String, Object> metadata = parseMetadata(item.getMetadataJson());
        LinkedHashMap<String, Object> notification = ensureMutableMap(metadata, "participantNotification");
        long readyCount = nonNegative(request == null ? null : request.readyCount(), defaultLong(metadata.get("contactReadyCount")));
        long missingCount = nonNegative(request == null ? null : request.missingCount(), defaultLong(metadata.get("contactMissingCount")));
        long pendingCount = nonNegative(request == null ? null : request.pendingCount(), Math.max(0L, countParticipants(metadata) - readyCount));
        String status = firstNonBlank(normalizeCode(request == null ? null : request.status()), deriveParticipantNotificationStatus(readyCount, pendingCount, missingCount));
        notification.put("status", status);
        notification.put("readyCount", readyCount);
        notification.put("pendingCount", pendingCount);
        notification.put("missingCount", missingCount);
        notification.put("notifiedAt", now.toString());
        putIfPresent(notification, "channel", request == null ? null : request.channel());
        metadata.put("participantNotificationStatus", status);
        metadata.put("contactReadyCount", readyCount);
        metadata.put("contactMissingCount", missingCount);
        touchExecutionState(item);
        Processo processo = loadProcesso(item.getProcessoId());
        OperationalNotificationProofService.GeneratedNotificationProof generatedProof = notificationProofService.materializeProof(
            processo,
            workItemId,
            "SECRETARIAT_PARTICIPANT_NOTIFICATION",
            "SECRETARIA_JUDICIARIA",
            "CARTA",
            request == null ? null : request.channel(),
            request == null ? null : request.formaIntimacao(),
            request == null ? null : request.provaResumo(),
            request == null ? List.of() : request.evidenceReferences(),
            nestedMap(metadata, "contactEnvelope"),
            request == null ? null : request.challengeId(),
            request == null ? null : request.otpCode(),
            request == null ? null : request.note()
        );
        LinkedHashMap<String, Object> generatedDocument = ensureMutableMap(metadata, "generatedNotificationDocument");
        generatedDocument.clear();
        generatedDocument.putAll(generatedProof.document());
        Map<String, Object> checkpoint = appendAudit(metadata, "PARTICIPANT_NOTIFICATION", actor, now, compactMap(
            "status", status,
            "readyCount", readyCount,
            "pendingCount", pendingCount,
            "missingCount", missingCount,
            "channel", request == null ? null : request.channel(),
            "proofNoteId", generatedProof.processNoteId(),
            "proofHash", generatedProof.document().get("contentHash"),
            "note", request == null ? null : request.note()
        ));
        return persistAndRespond(item, metadata, checkpoint, "PARTICIPANT_NOTIFICATION", null, generatedProof.document());
    }

    @Transactional
    public SecretariatQueueOperationalActionResponse registerAttendance(Long workItemId, SecretariatQueueAttendanceRequest request, String actor) {
        SecretariatQueueItem item = loadAccessibleItem(workItemId);
        Instant now = firstInstant(request == null ? null : request.registeredAt(), Instant.now());
        LinkedHashMap<String, Object> metadata = parseMetadata(item.getMetadataJson());
        String attendanceStatus = normalizeCode(request == null ? null : request.attendanceStatus());
        if (attendanceStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "attendanceStatus é obrigatório");
        }
        metadata.put("attendanceStatus", attendanceStatus);
        List<Map<String, Object>> entries = ensureMutableMapList(metadata, "attendanceEntries");
        entries.add(new LinkedHashMap<>(compactMap(
            "attendanceStatus", attendanceStatus,
            "role", request == null ? null : request.role(),
            "name", request == null ? null : request.name(),
            "registeredAt", now.toString(),
            "actor", safeActor(actor),
            "note", request == null ? null : request.note()
        )));
        if (List.of("PRESENTE", "AUSENTE", "JUSTIFICADO", "REALIZADO").contains(attendanceStatus)) {
            metadata.put("operationalConfirmationStatus", "CUMPRIDO");
        }
        touchExecutionState(item);
        Map<String, Object> checkpoint = appendAudit(metadata, "ATTENDANCE", actor, now, compactMap(
            "attendanceStatus", attendanceStatus,
            "role", request == null ? null : request.role(),
            "name", request == null ? null : request.name(),
            "note", request == null ? null : request.note()
        ));
        return persistAndRespond(item, metadata, checkpoint, "ATTENDANCE", null, null);
    }

    @Transactional
    public SecretariatQueueOperationalActionResponse registerCompletionEvent(Long workItemId, SecretariatQueueCompletionEventRequest request, String actor) {
        SecretariatQueueItem item = loadAccessibleItem(workItemId);
        Instant now = firstInstant(request == null ? null : request.occurredAt(), Instant.now());
        LinkedHashMap<String, Object> metadata = parseMetadata(item.getMetadataJson());
        String eventCode = request == null ? null : trimToNull(request.eventCode());
        if (eventCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventCode é obrigatório");
        }
        String completionStatus = firstNonBlank(normalizeCode(request == null ? null : request.completionEventStatus()), "EVENTO_REAL_REGISTRADO");
        metadata.put("completionEvent", eventCode);
        metadata.put("completionEventStatus", completionStatus);
        metadata.put("completionEventOccurredAt", now.toString());
        metadata.put("completionOccurredAt", now.toString());
        metadata.put("operationalConfirmationStatus", "CUMPRIDO");
        if (trimToNull(resolveProcessReturnRoute(metadata)) != null) {
            metadata.put("processReturnStatus", "PRONTO_PARA_RETORNO");
            metadata.put("autoReturnReady", Boolean.TRUE);
            touchExecutionState(item);
        } else {
            item.setStatus("CONCLUIDO");
        }
        Map<String, Object> checkpoint = appendAudit(metadata, "COMPLETION_EVENT", actor, now, compactMap(
            "eventCode", eventCode,
            "completionEventStatus", completionStatus,
            "processReturnRoute", resolveProcessReturnRoute(metadata),
            "autoReturnReady", metadata.get("autoReturnReady"),
            "note", request == null ? null : request.note()
        ));
        return persistAndRespond(item, metadata, checkpoint, "COMPLETION_EVENT", null, null);
    }

    @Transactional
    public SecretariatQueueOperationalActionResponse executeProcessReturn(Long workItemId, SecretariatQueueProcessReturnRequest request, String actor) {
        SecretariatQueueItem item = loadAccessibleItem(workItemId);
        Instant now = firstInstant(request == null ? null : request.returnedAt(), Instant.now());
        LinkedHashMap<String, Object> metadata = parseMetadata(item.getMetadataJson());
        String processReturnRoute = resolveProcessReturnRoute(metadata);
        if (trimToNull(processReturnRoute) == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item sem rota de retorno ao processo");
        }
        boolean ready = Boolean.TRUE.equals(metadata.get("autoReturnReady"))
            || "PRONTO_PARA_RETORNO".equalsIgnoreCase(stringValue(metadata.get("processReturnStatus")))
            || "EVENTO_REAL_REGISTRADO".equalsIgnoreCase(stringValue(metadata.get("completionEventStatus")));
        if (!ready) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item ainda não está pronto para retorno ao processo");
        }

        Processo processo = loadProcesso(item.getProcessoId());
        WorkItem rootWorkItem = loadRootWorkItem(item.getWorkItemId());
        InstitutionalActorRoutingService.InstitutionalRoute reentryRoute = resolveReentryRoute(processo.getId(), metadata);
        WorkItem reentryWorkItem = materializeReentryWorkItem(item, processo, metadata, reentryRoute, now, actor, request == null ? null : request.note());
        concludeRootWorkItem(rootWorkItem, metadata, now, actor, request == null ? null : request.note(), reentryWorkItem, reentryRoute);

        String processReturnStatus = firstNonBlank(normalizeCode(request == null ? null : request.processReturnStatus()), "RETORNO_EXECUTADO");
        LinkedHashMap<String, Object> processReturn = ensureMutableMap(metadata, "processReturn");
        processReturn.put("route", processReturnRoute);
        processReturn.put("status", processReturnStatus);
        processReturn.put("executedAt", now.toString());
        processReturn.put("actor", safeActor(actor));
        processReturn.put("targetInboxKey", reentryRoute.inboxKey());
        processReturn.put("targetQueueCode", reentryRoute.queueCode());
        processReturn.put("targetAssignedRole", reentryRoute.assignedRole() == null ? null : reentryRoute.assignedRole().name());
        processReturn.put("targetPanelRoute", resolveTargetPanelRoute(metadata, reentryRoute));
        processReturn.put("reentryWorkItemId", reentryWorkItem.getId());
        metadata.put("processReturnStatus", processReturnStatus);
        metadata.put("returnStatus", processReturnStatus);
        metadata.put("autoReturnReady", Boolean.FALSE);
        metadata.put("reentryWorkItemId", reentryWorkItem.getId());
        metadata.put("reentryInboxKey", reentryRoute.inboxKey());
        metadata.put("reentryQueueCode", reentryRoute.queueCode());
        metadata.put("reentryAssignedRole", reentryRoute.assignedRole() == null ? null : reentryRoute.assignedRole().name());
        metadata.put("reentryActionAxis", reentryRoute.routeAxis());
        metadata.put("reentryTargetPanelRoute", resolveTargetPanelRoute(metadata, reentryRoute));
        metadata.put("rootWorkItemStatus", rootWorkItem.getStatus() == null ? null : rootWorkItem.getStatus().name());
        metadata.put("rootWorkItemConcludedAt", now.toString());
        item.setStatus("CONCLUIDO");
        Map<String, Object> checkpoint = appendAudit(metadata, "PROCESS_RETURN", actor, now, compactMap(
            "processReturnStatus", processReturnStatus,
            "route", processReturnRoute,
            "reentryInboxKey", reentryRoute.inboxKey(),
            "reentryQueueCode", reentryRoute.queueCode(),
            "reentryWorkItemId", reentryWorkItem.getId(),
            "targetPanelRoute", resolveTargetPanelRoute(metadata, reentryRoute),
            "note", request == null ? null : request.note()
        ));
        return persistAndRespond(item, metadata, checkpoint, "PROCESS_RETURN", reentryRoute.inboxKey(), null);
    }

    private Processo loadProcesso(Long processoId) {
        if (processoId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item sem processo vinculado");
        }
        return processoRepository.findContextoArquiteturalById(processoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "processo de origem não encontrado para retorno institucional"));
    }

    private WorkItem loadRootWorkItem(Long workItemId) {
        if (workItemId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item sem work item raiz para baixa transacional");
        }
        return workItemRepository.findLockedDetailedById(workItemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "work item raiz não encontrado para baixa transacional"));
    }

    private InstitutionalActorRoutingService.InstitutionalRoute resolveReentryRoute(Long processoId, Map<String, Object> metadata) {
        String authorityClass = firstNonBlank(
            stringValue(metadata.get("authorityClass")),
            stringValue(metadata.get("authorityAxis")),
            stringValue(metadata.get("authorityScope"))
        );
        String actionAxis = firstNonBlank(
            stringValue(metadata.get("reentryActionAxis")),
            "RETORNO_PROCESSO"
        );
        String normalizedAuthority = normalizeCode(authorityClass);
        String normalizedReturnRoute = normalizeCode(resolveProcessReturnRoute(metadata));
        if (containsAny(normalizedAuthority, "MINISTRO") || containsAny(normalizedReturnRoute, "MINISTRO", "CORTE_SUPERIOR")) {
            return safeReentryRoute(institutionalActorRoutingService.superiorCourt(processoId, actionAxis, false), actionAxis);
        }
        if (containsAny(normalizedAuthority, "DESEMBARGADOR") || containsAny(normalizedReturnRoute, "DESEMBARGADOR", "COLEGIADO", "PLENARIO")) {
            return safeReentryRoute(institutionalActorRoutingService.colegiado(processoId, actionAxis), actionAxis);
        }
        return safeReentryRoute(institutionalActorRoutingService.gabineteDecision(processoId, actionAxis), actionAxis);
    }

    private InstitutionalActorRoutingService.InstitutionalRoute safeReentryRoute(InstitutionalActorRoutingService.InstitutionalRoute route, String actionAxis) {
        if (route != null) {
            return route;
        }
        Map<String, Object> fallbackMetadata = new LinkedHashMap<>();
        fallbackMetadata.put("fallback", Boolean.TRUE);
        fallbackMetadata.put("reason", "institutional-route-unavailable");
        return new InstitutionalActorRoutingService.InstitutionalRoute(
            "SECRETARIA_RETORNO",
            "SEC:TJCE:FORTALEZA:COMUM:CE:morada-nova:1a-vara",
            TipoUsuario.SERVIDOR_FORUM,
            normalizeCode(firstNonBlank(actionAxis, "RETORNO_PROCESSO")),
            "FALLBACK_SECRETARIA_RETORNO",
            "Rota institucional indisponível; retorno materializado em fila segura de secretaria.",
            fallbackMetadata
        );
    }

    private WorkItem materializeReentryWorkItem(SecretariatQueueItem item,
                                                Processo processo,
                                                Map<String, Object> metadata,
                                                InstitutionalActorRoutingService.InstitutionalRoute route,
                                                Instant now,
                                                String actor,
                                                String note) {
        String templateCode = buildReentryTemplateCode(route);
        Optional<WorkItem> latest = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode);
        if (latest.isPresent() && latest.get().getStatus() != WorkItemStatus.CONCLUIDO && latest.get().getStatus() != WorkItemStatus.CANCELADO) {
            return latest.get();
        }
        WorkItem workItem = WorkItem.builder()
            .processo(processo)
            .faseOrigem(processo.getFaseAtual())
            .templateCode(templateCode)
            .type(resolveReentryWorkItemType(metadata))
            .titulo(buildReentryTitle(processo, metadata))
            .descricao(buildReentryDescription(item, metadata, route, actor, note, now))
            .queueCode(firstNonBlank(route == null ? null : route.queueCode(), "SECRETARIA_RETORNO"))
            .inboxKey(firstNonBlank(route == null ? null : route.inboxKey(), "SEC:TJCE:FORTALEZA:COMUM:CE:morada-nova:1a-vara"))
            .assignedRole(route == null || route.assignedRole() == null ? TipoUsuario.SERVIDOR_FORUM : route.assignedRole())
            .status(WorkItemStatus.PENDENTE)
            .prioridade(resolveReentryPriority(item.getPrioridade(), metadata))
            .blocking(true)
            .dueAt(resolveReentryDueAt(item.getDueAt(), now))
            .uf(trimToNull(processo.getUf()))
            .comarca(firstNonBlank(processo.getComarca(), route == null ? null : stringValue(route.metadata().get("forumSeat"))))
            .baseLegal(route == null ? "retorno operacional sem rota institucional materializada" : route.rationale())
            .build();
        return workItemRepository.save(workItem);
    }

    private void concludeRootWorkItem(WorkItem rootWorkItem,
                                      Map<String, Object> metadata,
                                      Instant now,
                                      String actor,
                                      String note,
                                      WorkItem reentryWorkItem,
                                      InstitutionalActorRoutingService.InstitutionalRoute route) {
        if (rootWorkItem.getStatus() != WorkItemStatus.CONCLUIDO) {
            rootWorkItem.setStatus(WorkItemStatus.CONCLUIDO);
        }
        StringBuilder description = new StringBuilder(firstNonBlank(rootWorkItem.getDescricao(), ""));
        if (description.length() > 0) {
            description.append("\n\n");
        }
        description.append("[RETORNO_PROCESSO] ")
            .append(now)
            .append(" | ator=").append(safeActor(actor))
            .append(" | targetInbox=").append(firstNonBlank(route == null ? null : route.inboxKey(), "SEM_INBOX"))
            .append(" | targetQueue=").append(firstNonBlank(route == null ? null : route.queueCode(), "SEM_QUEUE"))
            .append(" | reentryWorkItemId=").append(reentryWorkItem.getId());
        String targetPanelRoute = resolveTargetPanelRoute(metadata, route);
        if (targetPanelRoute != null) {
            description.append(" | targetPanel=").append(targetPanelRoute);
        }
        if (trimToNull(note) != null) {
            description.append(" | note=").append(trimToNull(note));
        }
        rootWorkItem.setDescricao(description.toString());
        workItemRepository.save(rootWorkItem);
    }

    private String buildReentryTemplateCode(InstitutionalActorRoutingService.InstitutionalRoute route) {
        return "SECRETARIA:RETORNO_PROCESSO:" + normalizeTemplateToken(route == null ? null : route.routeAxis());
    }

    private String buildReentryTitle(Processo processo, Map<String, Object> metadata) {
        String eventTrack = firstNonBlank(stringValue(metadata.get("eventTrack")), stringValue(metadata.get("stage")), "RETORNO_PROCESSO");
        String numero = firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero());
        return "Retorno ao órgão julgador após cumprimento - " + eventTrack + (numero == null ? "" : " - " + numero);
    }

    private String buildReentryDescription(SecretariatQueueItem item,
                                           Map<String, Object> metadata,
                                           InstitutionalActorRoutingService.InstitutionalRoute route,
                                           String actor,
                                           String note,
                                           Instant now) {
        StringBuilder description = new StringBuilder();
        description.append("Retorno institucional materializado automaticamente pela secretaria.")
            .append("\norigemWorkItem=").append(item.getWorkItemId())
            .append("\nqueueOrigem=").append(firstNonBlank(item.getQueueCode(), "SEM_QUEUE"))
            .append("\nactor=").append(safeActor(actor))
            .append("\ninstante=").append(now)
            .append("\ntrack=").append(firstNonBlank(stringValue(metadata.get("eventTrack")), stringValue(metadata.get("stage")), "RETORNO_PROCESSO"))
            .append("\nrouteAxis=").append(firstNonBlank(route == null ? null : route.routeAxis(), "RETORNO_PROCESSO"))
            .append("\ntargetInbox=").append(firstNonBlank(route == null ? null : route.inboxKey(), "SEM_INBOX"))
            .append("\ntargetQueue=").append(firstNonBlank(route == null ? null : route.queueCode(), "SEM_QUEUE"));
        String targetPanelRoute = resolveTargetPanelRoute(metadata, route);
        if (targetPanelRoute != null) {
            description.append("\ntargetPanel=").append(targetPanelRoute);
        }
        if (trimToNull(note) != null) {
            description.append("\nnotaSecretaria=").append(trimToNull(note));
        }
        return description.toString();
    }

    private WorkItemType resolveReentryWorkItemType(Map<String, Object> metadata) {
        String eventTrack = firstNonBlank(stringValue(metadata.get("eventTrack")), stringValue(metadata.get("stage")), "RETORNO_PROCESSO");
        return switch (normalizeCode(eventTrack)) {
            case "AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA" -> WorkItemType.AUDIENCIA;
            case "COMUNICACAO_PROCESSUAL" -> WorkItemType.INTIMACAO;
            case "CUMPRIMENTO_EXECUCAO" -> WorkItemType.CUMPRIMENTO_SENTENCA;
            case "PERICIA_TECNICA" -> WorkItemType.PERICIA;
            case "SANEAMENTO_PROCESSUAL" -> WorkItemType.JUNTADA;
            default -> WorkItemType.OUTRO;
        };
    }

    private Integer resolveReentryPriority(Integer currentPriority, Map<String, Object> metadata) {
        Integer explicit = intValue(metadata.get("prioridade"));
        int base = explicit != null ? explicit : (currentPriority == null ? 2 : currentPriority);
        if (Boolean.TRUE.equals(metadata.get("blocking")) || "URGENTE".equals(normalizeCode(stringValue(metadata.get("priorityBand"))))) {
            return 1;
        }
        return Math.max(1, Math.min(base, 3));
    }

    private Instant resolveReentryDueAt(Instant currentDueAt, Instant now) {
        if (currentDueAt != null && currentDueAt.isAfter(now.minus(1, ChronoUnit.HOURS))) {
            return currentDueAt;
        }
        return now.plus(4, ChronoUnit.HOURS);
    }

    private String resolveTargetPanelRoute(Map<String, Object> metadata, InstitutionalActorRoutingService.InstitutionalRoute route) {
        return firstNonBlank(
            stringValue(metadata.get("reentryTargetPanelRoute")),
            stringValue(metadata.get("authorityInstitutionalLandingPath")),
            stringValue(metadata.get("institutionalLandingPath")),
            stringValue(metadata.get("processReturnRoute")),
            route == null ? null : stringValue(route.metadata().get("institutionalLandingPath"))
        );
    }

    private SecretariatQueueItem loadAccessibleItem(Long workItemId) {
        SecretariatQueueItem item = repository.findLockedByWorkItemId(workItemId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("SecretariatQueueItem", workItemId));
        visibilityService.requireInboxAccess(item.getInboxKey());
        return item;
    }

    private SecretariatQueueOperationalActionResponse persistAndRespond(SecretariatQueueItem item,
                                                                        LinkedHashMap<String, Object> metadata,
                                                                        Map<String, Object> checkpoint,
                                                                        String actionCode,
                                                                        String targetInboxKey,
                                                                        Map<String, Object> generatedDocument) {
        item.setMetadataJson(writeJson(metadata));
        item.setUpdatedAt(Instant.now());
        SecretariatQueueItem saved = repository.save(item);
        notifyLive(saved.getInboxKey(), actionCode, compactMap(
            "event", actionCode,
            "workItemId", saved.getWorkItemId(),
            "processoId", saved.getProcessoId(),
            "status", saved.getStatus(),
            "processReturnStatus", metadata.get("processReturnStatus")
        ));
        if (trimToNull(targetInboxKey) != null) {
            notifyLive(targetInboxKey, "PROCESS_RETURN_EXECUTED", compactMap(
                "event", "PROCESS_RETURN_EXECUTED",
                "sourceWorkItemId", saved.getWorkItemId(),
                "processoId", saved.getProcessoId(),
                "reentryWorkItemId", metadata.get("reentryWorkItemId"),
                "targetPanelRoute", resolveTargetPanelRoute(metadata, null)
            ));
        }
        return new SecretariatQueueOperationalActionResponse(
            saved.getWorkItemId(),
            saved.getProcessoId(),
            saved.getInboxKey(),
            actionCode,
            saved.getStatus(),
            firstNonBlank(stringValue(metadata.get("operationalConfirmationStatus")), stringValue(metadata.get("confirmationStatus")), "NAO_APLICAVEL"),
            firstNonBlank(nestedString(metadata, "venue", "confirmationStatus"), stringValue(metadata.get("venueConfirmationStatus")), "NAO_APLICAVEL"),
            firstNonBlank(nestedString(metadata, "participantNotification", "status"), stringValue(metadata.get("participantNotificationStatus")), "NAO_APLICAVEL"),
            firstNonBlank(stringValue(metadata.get("attendanceStatus")), "NAO_APLICAVEL"),
            firstNonBlank(stringValue(metadata.get("completionEventStatus")), "NAO_APLICAVEL"),
            firstNonBlank(stringValue(metadata.get("processReturnStatus")), stringValue(metadata.get("returnStatus")), "NAO_APLICAVEL"),
            Boolean.TRUE.equals(metadata.get("autoReturnReady")),
            saved.getUpdatedAt(),
            OperationalApiRoutes.secretariatQueuePanel(saved.getInboxKey()),
            OperationalApiRoutes.secretariatQueueAgenda(saved.getInboxKey()),
            resolveTargetPanelRoute(metadata, null),
            longValue(metadata.get("reentryWorkItemId")),
            immutableMap(checkpoint),
            immutableMap(metadata),
            generatedDocument == null ? Map.of() : immutableMap(generatedDocument)
        );
    }

    private void notifyLive(String inboxKey, String eventCode, Map<String, Object> payload) {
        if (trimToNull(inboxKey) == null) {
            return;
        }
        outboxPublisher.enqueueSecretariatLive(inboxKey, eventCode, Instant.now(), Collections.unmodifiableMap(payload));
    }

    private void touchExecutionState(SecretariatQueueItem item) {
        if (item.getStatus() == null || "PENDENTE".equalsIgnoreCase(item.getStatus())) {
            item.setStatus("EM_EXECUCAO");
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return mapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> ensureMutableMap(Map<String, Object> source, String key) {
        Object raw = source.get(key);
        if (raw instanceof LinkedHashMap<?, ?> linked) {
            return (LinkedHashMap<String, Object>) linked;
        }
        if (raw instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                if (k != null) {
                    copy.put(String.valueOf(k), v);
                }
            });
            source.put(key, copy);
            return copy;
        }
        LinkedHashMap<String, Object> created = new LinkedHashMap<>();
        source.put(key, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> ensureMutableMapList(Map<String, Object> source, String key) {
        Object raw = source.get(key);
        List<Map<String, Object>> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
                    map.forEach((k, v) -> {
                        if (k != null) {
                            copy.put(String.valueOf(k), v);
                        }
                    });
                    out.add(copy);
                }
            }
        }
        source.put(key, out);
        return out;
    }

    private Map<String, Object> appendAudit(LinkedHashMap<String, Object> metadata, String actionCode, String actor, Instant occurredAt, Map<String, Object> details) {
        List<Map<String, Object>> trail = ensureMutableMapList(metadata, "operationalAuditTrail");
        LinkedHashMap<String, Object> checkpoint = new LinkedHashMap<>();
        checkpoint.put("actionCode", actionCode);
        checkpoint.put("actor", safeActor(actor));
        checkpoint.put("occurredAt", occurredAt.toString());
        checkpoint.put("details", details == null ? Map.of() : Map.copyOf(details));
        trail.add(checkpoint);
        if (trail.size() > 80) {
            trail.subList(0, trail.size() - 80).clear();
        }
        metadata.put("lastOperationalCheckpoint", checkpoint);
        metadata.put("lastOperationalAction", actionCode);
        metadata.put("lastOperationalActor", safeActor(actor));
        metadata.put("lastOperationalAt", occurredAt.toString());
        return checkpoint;
    }

    private String deriveParticipantNotificationStatus(long readyCount, long pendingCount, long missingCount) {
        if (missingCount > 0L) {
            return "CONTATOS_INCOMPLETOS";
        }
        if (readyCount > 0L && pendingCount <= 0L) {
            return "PARTICIPANTES_INTIMADOS";
        }
        if (readyCount > 0L) {
            return "PENDENTE_INTIMACAO";
        }
        return "AGUARDANDO_DADOS_PARTICIPANTES";
    }

    private long countParticipants(Map<String, Object> metadata) {
        long count = 0L;
        Map<String, Object> envelope = nestedMap(metadata, "contactEnvelope");
        if (!nestedMap(envelope, "autor").isEmpty()) {
            count++;
        }
        if (!nestedMap(envelope, "reu").isEmpty()) {
            count++;
        }
        Object advogados = envelope.get("advogados");
        if (advogados instanceof List<?> list) {
            count += list.stream().filter(Map.class::isInstance).count();
        }
        if (count == 0L) {
            Object participants = metadata.get("participants");
            if (participants instanceof List<?> list) {
                count = list.stream().filter(Map.class::isInstance).count();
            }
        }
        return count;
    }

    private String resolveProcessReturnRoute(Map<String, Object> metadata) {
        return firstNonBlank(
            stringValue(metadata.get("processReturnRoute")),
            stringValue(metadata.get("authorityReturnRoute")),
            stringValue(metadata.get("originReturnRoute"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return Map.of();
        }
        Object raw = source.get(key);
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String nestedString(Map<String, Object> source, String key, String nestedKey) {
        return stringValue(nestedMap(source, key).get(nestedKey));
    }

    private static boolean containsAny(String value, String... tokens) {
        if (value == null || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeTemplateToken(String raw) {
        String normalized = normalizeCode(raw);
        return normalized == null ? "BASE" : normalized.replaceAll("[^A-Z0-9_]+", "_");
    }

    private static Map<String, Object> compactMap(Object... values) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (values == null) {
            return out;
        }
        for (int index = 0; index + 1 < values.length; index += 2) {
            Object key = values[index];
            Object value = values[index + 1];
            if (key != null && value != null) {
                String normalizedKey = String.valueOf(key);
                if (!normalizedKey.isBlank()) {
                    out.put(normalizedKey, value);
                }
            }
        }
        return out;
    }


    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static String safeActor(String actor) {
        return firstNonBlank(trimToNull(actor), "SECRETARIA");
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && !key.isBlank() && value != null) {
            target.put(key, value);
        }
    }

    private static String normalizeCode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        return trimToNull(String.valueOf(raw));
    }

    private static Integer intValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Instant firstInstant(Instant preferred, Instant fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static Long longValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return Math.max(0L, number.longValue());
        }
        try {
            return Math.max(0L, Long.parseLong(String.valueOf(raw).trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private static long defaultLong(Object raw) {
        Long value = longValue(raw);
        return value == null ? 0L : value;
    }

    private static long nonNegative(Integer preferred, long fallback) {
        if (preferred == null) {
            return Math.max(0L, fallback);
        }
        return Math.max(0L, preferred.longValue());
    }
}
