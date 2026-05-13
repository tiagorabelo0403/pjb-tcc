package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacao;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarNativeOperationalEventAssemblerService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Fortaleza");
    private static final int WINDOW_LIMIT = 200;
    private static final Collection<PeritoNomeacaoStatus> PERITO_CALENDAR_STATUSES = EnumSet.of(
            PeritoNomeacaoStatus.NOMEADO,
            PeritoNomeacaoStatus.ACEITO,
            PeritoNomeacaoStatus.RECUSADO,
            PeritoNomeacaoStatus.REVOGADO
    );

    private static final List<String> SECRETARIAT_ACTIVE_STATUSES = List.of("PENDENTE", "EM_EXECUCAO");

    private final WorkItemRepository workItemRepository;
    private final PeritoNomeacaoRepository peritoNomeacaoRepository;
    private final DiligenciaOperadorCheckpointEventoRepository checkpointRepository;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final SecretariatQueueItemRepository secretariatQueueItemRepository;

    public CalendarNativeOperationalEventAssemblerService(WorkItemRepository workItemRepository,
                                                          PeritoNomeacaoRepository peritoNomeacaoRepository,
                                                          DiligenciaOperadorCheckpointEventoRepository checkpointRepository,
                                                          DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                                          SecretariatQueueItemRepository secretariatQueueItemRepository) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.peritoNomeacaoRepository = Objects.requireNonNull(peritoNomeacaoRepository);
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.secretariatQueueItemRepository = Objects.requireNonNull(secretariatQueueItemRepository);
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> assembleForUser(Usuario usuario,
                                                  LocalDate from,
                                                  LocalDate to,
                                                  Map<Long, String> numeroById) {
        if (usuario == null || usuario.getId() == null || from == null || to == null) {
            return List.of();
        }
        Instant fromInstant = from.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(DEFAULT_ZONE).minusSeconds(1).toInstant();
        Map<Long, String> processoMap = numeroById == null ? Map.of() : numeroById;
        LinkedHashMap<String, CalendarEventDto> events = new LinkedHashMap<>();
        LinkedHashMap<Long, WorkItem> workItems = loadWorkItems(usuario, fromInstant, toInstant);
        appendWorkItems(events, usuario, workItems.values(), processoMap);
        appendOfficialTelemetry(events, usuario, fromInstant, toInstant, workItems.values(), processoMap);
        appendSecretariatQueue(events, usuario, fromInstant, toInstant, workItems.values(), processoMap);
        appendPericia(events, usuario, from.atStartOfDay(), to.plusDays(1).atStartOfDay().minusSeconds(1), processoMap);
        return events.values().stream()
                .sorted(Comparator.comparing(CalendarEventDto::at)
                        .thenComparing(CalendarEventDto::title)
                        .thenComparing(event -> event.domainKey() == null ? "" : event.domainKey()))
                .toList();
    }

    private LinkedHashMap<Long, WorkItem> loadWorkItems(Usuario usuario,
                                                Instant fromInstant,
                                                Instant toInstant) {
        LinkedHashMap<Long, WorkItem> workItems = new LinkedHashMap<>();
        workItemRepository.findCalendarWindowByAssignedUser(usuario.getId(), fromInstant, toInstant, PageRequest.of(0, WINDOW_LIMIT))
                .forEach(item -> workItems.put(item.getId(), item));
        TipoUsuario tipoUsuario = usuario.getTipoUsuario();
        if (tipoUsuario != null) {
            workItemRepository.findCalendarWindowByRoleAndTerritory(tipoUsuario, usuario.getUf(), usuario.getComarca(), fromInstant, toInstant, PageRequest.of(0, WINDOW_LIMIT))
                    .forEach(item -> workItems.putIfAbsent(item.getId(), item));
        }
        return workItems;
    }

    private void appendWorkItems(Map<String, CalendarEventDto> out,
                                 Usuario usuario,
                                 Collection<WorkItem> workItems,
                                 Map<Long, String> numeroById) {
        for (WorkItem workItem : workItems) {
            CalendarEventDto event = toWorkItemEvent(usuario, workItem, numeroById);
            if (event != null && event.domainKey() != null) {
                out.putIfAbsent(event.domainKey(), event);
            }
        }
    }

    private void appendPericia(Map<String, CalendarEventDto> out,
                               Usuario usuario,
                               LocalDateTime from,
                               LocalDateTime to,
                               Map<Long, String> numeroById) {
        if (usuario.getTipoUsuario() == null || !usuario.getTipoUsuario().isPerito()) {
            return;
        }
        List<PeritoNomeacao> nomeacoes = peritoNomeacaoRepository.findByPerito_IdAndStatusInAndNomeadoEmBetweenOrderByNomeadoEmAsc(
                usuario.getId(),
                PERITO_CALENDAR_STATUSES,
                from,
                to
        );
        for (PeritoNomeacao nomeacao : nomeacoes) {
            CalendarEventDto event = toPericiaEvent(nomeacao, numeroById);
            if (event != null && event.domainKey() != null) {
                out.putIfAbsent(event.domainKey(), event);
            }
        }
    }

    private void appendOfficialTelemetry(Map<String, CalendarEventDto> out,
                                         Usuario usuario,
                                         Instant fromInstant,
                                         Instant toInstant,
                                         Collection<WorkItem> workItems,
                                         Map<Long, String> numeroById) {
        if (!isOfficialUser(usuario) || usuario.getId() == null || workItems.isEmpty()) {
            return;
        }
        List<Long> workItemIds = workItems.stream()
                .map(WorkItem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (workItemIds.isEmpty()) {
            return;
        }
        List<DiligenciaOperadorCheckpointEvento> checkpoints = checkpointRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByOccurredAtDesc(
                        usuario.getId(),
                        TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                        workItemIds)
                .stream()
                .filter(event -> event.getOccurredAt() != null)
                .filter(event -> !event.getOccurredAt().isBefore(fromInstant) && !event.getOccurredAt().isAfter(toInstant))
                .toList();
        checkpoints.stream()
                .map(event -> toOfficialCheckpointEvent(event, numeroById))
                .filter(Objects::nonNull)
                .forEach(event -> out.putIfAbsent(event.domainKey(), event));
        appendOfficialAttemptWindows(out, checkpoints, numeroById);
        List<DiligenciaOperadorEncerramento> encerramentos = encerramentoRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByCreatedAtDesc(
                        usuario.getId(),
                        TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                        workItemIds)
                .stream()
                .filter(event -> event.getCreatedAt() != null)
                .filter(event -> !event.getCreatedAt().isBefore(fromInstant) && !event.getCreatedAt().isAfter(toInstant))
                .toList();
        encerramentos.stream()
                .map(event -> toOfficialClosureEvent(event, numeroById))
                .filter(Objects::nonNull)
                .forEach(event -> out.putIfAbsent(event.domainKey(), event));
        appendOfficialReturnWindows(out, encerramentos, numeroById);
    }

    private void appendSecretariatQueue(Map<String, CalendarEventDto> out,
                                        Usuario usuario,
                                        Instant fromInstant,
                                        Instant toInstant,
                                        Collection<WorkItem> workItems,
                                        Map<Long, String> numeroById) {
        if (!supportsInstitutionalQueue(usuario)) {
            return;
        }
        List<String> inboxKeys = workItems.stream()
                .map(WorkItem::getInboxKey)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(this::looksLikeInstitutionalInbox)
                .distinct()
                .limit(24)
                .toList();
        if (inboxKeys.isEmpty()) {
            return;
        }
        secretariatQueueItemRepository.findCalendarWindowByInboxKeys(inboxKeys, SECRETARIAT_ACTIVE_STATUSES, fromInstant, toInstant, PageRequest.of(0, WINDOW_LIMIT))
                .stream()
                .map(item -> toSecretariatQueueEvent(item, numeroById))
                .filter(Objects::nonNull)
                .forEach(event -> out.putIfAbsent(event.domainKey(), event));
    }

    private CalendarEventDto toWorkItemEvent(Usuario usuario,
                                             WorkItem workItem,
                                             Map<Long, String> numeroById) {
        if (workItem == null || workItem.getId() == null) {
            return null;
        }
        Instant eventInstant = firstNonNull(workItem.getDueAt(), workItem.getUpdatedAt(), workItem.getCreatedAt());
        if (eventInstant == null) {
            return null;
        }
        LocalDateTime at = LocalDateTime.ofInstant(eventInstant, DEFAULT_ZONE);
        Long processoId = workItem.getProcessoId();
        String processoNumero = processoId == null ? null : numeroById.get(processoId);
        EventProfile profile = classifyWorkItem(usuario, workItem);
        String domainKey = "NATIVE:WORKITEM:" + workItem.getId() + ':' + profile.eventType();
        return new CalendarEventDto(
                profile.eventType(),
                -workItem.getId(),
                processoId,
                processoNumero,
                profile.title(firstNonBlank(workItem.getTitulo(), workItem.getTemplateCode(), "Evento operacional")),
                at,
                profile.color(at, workItem.isBlocking()),
                false,
                processoId == null ? "/api/v1/workitems/" + workItem.getId() : "/api/v1/processos/" + processoId,
                profile.body(workItem),
                domainKey,
                "WORKITEM"
        );
    }

    private CalendarEventDto toPericiaEvent(PeritoNomeacao nomeacao,
                                            Map<Long, String> numeroById) {
        if (nomeacao == null || nomeacao.getId() == null || nomeacao.getNomeadoEm() == null) {
            return null;
        }
        Long processoId = nomeacao.getProcesso() != null ? nomeacao.getProcesso().getId() : null;
        String processoNumero = processoId == null ? null : numeroById.get(processoId);
        String status = nomeacao.getStatus() == null ? "NOMEADO" : nomeacao.getStatus().name();
        String observacaoNormalizada = normalize(nomeacao.getObservacao());
        String eventType = resolvePericiaEventType(status, observacaoNormalizada);
        String title = switch (eventType) {
            case "PERICIA_ACEITE" -> "Aceite pericial";
            case "PERICIA_ENTREGA_TECNICA" -> "Entrega técnica pericial";
            case "PERICIA_LAUDO" -> "Laudo pericial";
            case "PERICIA_LAUDO_PENDENTE" -> "Laudo pendente";
            default -> switch (status) {
                case "ACEITO" -> "Perícia aceita";
                case "RECUSADO" -> "Perícia recusada";
                case "REVOGADO" -> "Perícia revogada";
                default -> "Perícia nomeada";
            };
        };
        LocalDateTime at = resolvePericiaDate(nomeacao, status);
        String body = String.join("\n",
                "categoria=" + eventType,
                "status=" + status,
                "processoId=" + (processoId == null ? "" : processoId),
                "nomeacaoId=" + nomeacao.getId(),
                "peritoId=" + (nomeacao.getPerito() == null || nomeacao.getPerito().getId() == null ? "" : nomeacao.getPerito().getId()),
                "respondidoEm=" + (nomeacao.getRespondidoEm() == null ? "" : nomeacao.getRespondidoEm()),
                "revogadoEm=" + (nomeacao.getRevogadoEm() == null ? "" : nomeacao.getRevogadoEm()),
                "observacao=" + sanitizeBody(nomeacao.getObservacao())
        );
        return new CalendarEventDto(
                eventType,
                -1000000L - nomeacao.getId(),
                processoId,
                processoNumero,
                title,
                at,
                switch (status) {
                    case "ACEITO" -> "GREEN";
                    case "RECUSADO", "REVOGADO" -> "RED";
                    default -> "BLUE";
                },
                false,
                "/api/v1/perito/nomeacoes",
                body,
                "NATIVE:PERICIA:" + nomeacao.getId() + ':' + eventType + ':' + status,
                "PERICIA"
        );
    }

    private CalendarEventDto toOfficialCheckpointEvent(DiligenciaOperadorCheckpointEvento checkpoint,
                                                     Map<Long, String> numeroById) {
        if (checkpoint == null || checkpoint.getId() == null || checkpoint.getOccurredAt() == null) {
            return null;
        }
        Long processoId = checkpoint.getProcessoId();
        String processoNumero = firstNonBlank(checkpoint.getProcessoNumero(), processoId == null ? null : numeroById.get(processoId));
        LocalDateTime at = LocalDateTime.ofInstant(checkpoint.getOccurredAt(), DEFAULT_ZONE);
        String eventType = switch (checkpoint.getCheckpointTipo()) {
            case TENTATIVA -> "MANDADO_TENTATIVA";
            case PARTIDA, CHEGADA -> "MANDADO_ROTA";
        };
        String title = switch (checkpoint.getCheckpointTipo()) {
            case TENTATIVA -> "Tentativa de diligência";
            case PARTIDA -> "Saída para diligência";
            case CHEGADA -> "Chegada georreferenciada";
        };
        String color = switch (checkpoint.getCheckpointTipo()) {
            case TENTATIVA -> "AMBER";
            case PARTIDA -> "BLUE";
            case CHEGADA -> checkpoint.isInsideGeofence() ? "GREEN" : "AMBER";
        };
        String body = String.join("\n",
                "categoria=MANDADO",
                "operacao=" + eventType,
                "checkpoint=" + checkpoint.getCheckpointTipo(),
                "classificacao=" + safe(checkpoint.getClassification()),
                "insideGeofence=" + checkpoint.isInsideGeofence(),
                "tentativa=" + checkpoint.getTentativaSequencia(),
                "distanciaMetros=" + checkpoint.getDistanceMeters(),
                "source=" + safe(checkpoint.getSource()),
                "referencia=" + safe(checkpoint.getDiligenceReference()),
                "processoId=" + safe(processoId),
                "workItemId=" + safe(checkpoint.getWorkItemId())
        );
        String detailsUrl = officialDetailsUrl(processoId);
        return new CalendarEventDto(
                eventType,
                -2000000L - checkpoint.getId(),
                processoId,
                processoNumero,
                title,
                at,
                color,
                false,
                detailsUrl,
                body,
                "NATIVE:OFICIAL:CHECKPOINT:" + checkpoint.getId() + ':' + eventType,
                "OFICIAL_TELEMETRIA"
        );
    }

    private CalendarEventDto toOfficialClosureEvent(DiligenciaOperadorEncerramento encerramento,
                                                    Map<Long, String> numeroById) {
        if (encerramento == null || encerramento.getId() == null || encerramento.getCreatedAt() == null) {
            return null;
        }
        Long processoId = encerramento.getProcessoId();
        String processoNumero = firstNonBlank(encerramento.getProcessoNumero(), processoId == null ? null : numeroById.get(processoId));
        LocalDateTime at = LocalDateTime.ofInstant(encerramento.getCreatedAt(), DEFAULT_ZONE);
        String eventType = switch (encerramento.getOutcome()) {
            case CUMPRIMENTO_POSITIVO -> "MANDADO_CERTIDAO";
            case CUMPRIMENTO_FRUSTRADO, DILIGENCIA_PARCIAL -> "MANDADO_RETORNO";
        };
        String title = switch (encerramento.getOutcome()) {
            case CUMPRIMENTO_POSITIVO -> "Certidão e cumprimento positivo";
            case CUMPRIMENTO_FRUSTRADO -> "Retorno de diligência frustrada";
            case DILIGENCIA_PARCIAL -> "Retorno de diligência parcial";
        };
        String color = encerramento.getOutcome() == DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO ? "GREEN" : "RED";
        String body = String.join("\n",
                "categoria=MANDADO",
                "operacao=" + eventType,
                "outcome=" + encerramento.getOutcome(),
                "referencia=" + safe(encerramento.getDiligenceReference()),
                "certidaoId=" + safe(encerramento.getCertidaoId()),
                "followupWorkItemId=" + safe(encerramento.getFollowupWorkItemId()),
                "documentos=" + (encerramento.getDocumentosVinculados() == null ? 0 : encerramento.getDocumentosVinculados()),
                "processoId=" + safe(processoId),
                "workItemId=" + safe(encerramento.getWorkItemId())
        );
        String detailsUrl = officialDetailsUrl(processoId);
        return new CalendarEventDto(
                eventType,
                -3000000L - encerramento.getId(),
                processoId,
                processoNumero,
                title,
                at,
                color,
                false,
                detailsUrl,
                body,
                "NATIVE:OFICIAL:ENCERRAMENTO:" + encerramento.getId() + ':' + eventType,
                "OFICIAL_TELEMETRIA"
        );
    }

    private void appendOfficialAttemptWindows(Map<String, CalendarEventDto> out,
                                              List<DiligenciaOperadorCheckpointEvento> checkpoints,
                                              Map<Long, String> numeroById) {
        Map<String, List<DiligenciaOperadorCheckpointEvento>> grouped = new LinkedHashMap<>();
        for (DiligenciaOperadorCheckpointEvento checkpoint : checkpoints) {
            if (checkpoint.getCheckpointTipo() != DiligenciaCheckpointTipo.TENTATIVA) {
                continue;
            }
            String groupingKey = officialGroupingKey(checkpoint.getWorkItemId(), checkpoint.getDiligenceReference(), checkpoint.getProcessoId());
            grouped.computeIfAbsent(groupingKey, ignored -> new ArrayList<>()).add(checkpoint);
        }
        for (Map.Entry<String, List<DiligenciaOperadorCheckpointEvento>> entry : grouped.entrySet()) {
            List<DiligenciaOperadorCheckpointEvento> attempts = entry.getValue();
            int maxTentativas = attempts.stream()
                    .map(DiligenciaOperadorCheckpointEvento::getTentativaSequencia)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(attempts.size());
            if (maxTentativas < 2 && attempts.size() < 2) {
                continue;
            }
            DiligenciaOperadorCheckpointEvento latest = attempts.stream()
                    .max(Comparator.comparing(DiligenciaOperadorCheckpointEvento::getOccurredAt))
                    .orElse(null);
            if (latest == null || latest.getOccurredAt() == null) {
                continue;
            }
            Long processoId = latest.getProcessoId();
            String processoNumero = firstNonBlank(latest.getProcessoNumero(), processoId == null ? null : numeroById.get(processoId));
            String body = String.join("\n",
                    "categoria=MANDADO",
                    "operacao=MANDADO_MULTI_TENTATIVA",
                    "referencia=" + safe(latest.getDiligenceReference()),
                    "processoId=" + safe(processoId),
                    "workItemId=" + safe(latest.getWorkItemId()),
                    "totalTentativas=" + attempts.size(),
                    "maiorSequencia=" + maxTentativas,
                    "ultimaClassificacao=" + safe(latest.getClassification()),
                    "ultimaDistanciaMetros=" + latest.getDistanceMeters(),
                    "ultimaOcorrencia=" + latest.getOccurredAt()
            );
            out.putIfAbsent("NATIVE:OFICIAL:TENTATIVAS:" + entry.getKey(), new CalendarEventDto(
                    "MANDADO_MULTI_TENTATIVA",
                    generatedNegativeId(entry.getKey(), 61_000_000L),
                    processoId,
                    processoNumero,
                    "Janela de múltiplas tentativas",
                    LocalDateTime.ofInstant(latest.getOccurredAt(), DEFAULT_ZONE),
                    latest.isInsideGeofence() ? "AMBER" : "RED",
                    false,
                    officialDetailsUrl(processoId),
                    body,
                    "NATIVE:OFICIAL:TENTATIVAS:" + entry.getKey(),
                    "OFICIAL_TELEMETRIA"
            ));
        }
    }

    private void appendOfficialReturnWindows(Map<String, CalendarEventDto> out,
                                             List<DiligenciaOperadorEncerramento> encerramentos,
                                             Map<Long, String> numeroById) {
        if (encerramentos.isEmpty()) {
            return;
        }
        List<Long> followupIds = encerramentos.stream()
                .map(DiligenciaOperadorEncerramento::getFollowupWorkItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, WorkItem> followupMap = new LinkedHashMap<>();
        if (!followupIds.isEmpty()) {
            workItemRepository.findAllById(followupIds).forEach(item -> followupMap.put(item.getId(), item));
        }
        for (DiligenciaOperadorEncerramento encerramento : encerramentos) {
            if (encerramento.getOutcome() == DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO && encerramento.getFollowupWorkItemId() == null) {
                continue;
            }
            WorkItem followup = encerramento.getFollowupWorkItemId() == null ? null : followupMap.get(encerramento.getFollowupWorkItemId());
            Long processoId = encerramento.getProcessoId();
            String processoNumero = firstNonBlank(encerramento.getProcessoNumero(), processoId == null ? null : numeroById.get(processoId));
            Instant eventInstant = followup == null ? encerramento.getCreatedAt() : firstNonNull(followup.getDueAt(), followup.getUpdatedAt(), encerramento.getCreatedAt());
            String followupQueue = followup == null ? null : followup.getQueueCode();
            String followupTemplate = followup == null ? null : followup.getTemplateCode();
            String title = encerramento.getOutcome() == DiligenciaEncerramentoTipo.CUMPRIMENTO_FRUSTRADO
                    ? "Janela de retorno e reexpedição"
                    : encerramento.getOutcome() == DiligenciaEncerramentoTipo.DILIGENCIA_PARCIAL
                    ? "Retorno parcial com próxima providência"
                    : "Janela de certidão e ato seguinte";
            String body = String.join("\n",
                    "categoria=MANDADO",
                    "operacao=MANDADO_JANELA_RETORNO",
                    "outcome=" + encerramento.getOutcome(),
                    "referencia=" + safe(encerramento.getDiligenceReference()),
                    "certidaoId=" + safe(encerramento.getCertidaoId()),
                    "processoId=" + safe(processoId),
                    "workItemId=" + safe(encerramento.getWorkItemId()),
                    "followupWorkItemId=" + safe(encerramento.getFollowupWorkItemId()),
                    "followupQueue=" + safe(followupQueue),
                    "followupTemplate=" + safe(followupTemplate),
                    "documentos=" + (encerramento.getDocumentosVinculados() == null ? 0 : encerramento.getDocumentosVinculados()),
                    "janelaAt=" + eventInstant
            );
            String domainKey = "NATIVE:OFICIAL:RETORNO_WINDOW:" + officialGroupingKey(encerramento.getWorkItemId(), encerramento.getDiligenceReference(), processoId);
            out.putIfAbsent(domainKey, new CalendarEventDto(
                    "MANDADO_JANELA_RETORNO",
                    generatedNegativeId(domainKey, 62_000_000L),
                    processoId,
                    processoNumero,
                    title,
                    LocalDateTime.ofInstant(eventInstant, DEFAULT_ZONE),
                    encerramento.getOutcome() == DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO ? "AMBER" : "RED",
                    false,
                    officialDetailsUrl(processoId),
                    body,
                    domainKey,
                    "OFICIAL_TELEMETRIA"
            ));
        }
    }

    private String officialGroupingKey(Long workItemId, String diligenceReference, Long processoId) {
        if (workItemId != null) {
            return "WI:" + workItemId;
        }
        if (diligenceReference != null && !diligenceReference.isBlank()) {
            return "REF:" + diligenceReference.trim();
        }
        return "PROC:" + safe(processoId);
    }

    private String officialDetailsUrl(Long processoId) {
        return processoId == null
                ? "/api/v1/oficial-justica/diligencias/fila-viva"
                : "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench";
    }

    private CalendarEventDto toSecretariatQueueEvent(SecretariatQueueItem item,
                                                     Map<Long, String> numeroById) {
        if (item == null || item.getWorkItemId() == null) {
            return null;
        }
        Instant reference = firstNonNull(item.getDueAt(), item.getUpdatedAt(), item.getCreatedAt());
        if (reference == null) {
            return null;
        }
        LocalDateTime at = LocalDateTime.ofInstant(reference, DEFAULT_ZONE);
        String eventType = resolveSecretariatQueueEventType(item);
        Long processoId = item.getProcessoId();
        String processoNumero = processoId == null ? null : numeroById.get(processoId);
        String title = switch (eventType) {
            case "SECRETARIA_FILA_AUDIENCIA" -> "Fila de audiência da secretaria";
            case "SECRETARIA_PAUTA_INTERNA" -> "Pauta interna da secretaria";
            case "GABINETE_VOTO" -> "Fila de voto de gabinete";
            case "GABINETE_MINUTA" -> "Fila de minuta de gabinete";
            case "GABINETE_CONCLUSAO" -> "Conclusão e gabinete";
            case "GABINETE_PAUTA" -> "Pauta de gabinete e colegiado";
            case "SECRETARIA_SLA" -> "SLA e expediente da unidade";
            default -> "Fila operacional institucional";
        };
        String body = String.join("\n",
                "categoria=" + (eventType.startsWith("GABINETE") ? "GABINETE" : "SECRETARIA"),
                "operacao=" + eventType,
                "inbox=" + safe(item.getInboxKey()),
                "queue=" + safe(item.getQueueCode()),
                "lane=" + safe(item.getLaneCode()),
                "desk=" + safe(item.getDeskAxis()),
                "status=" + safe(item.getStatus()),
                "prioridade=" + (item.getPrioridade() == null ? 0 : item.getPrioridade()),
                "score=" + (item.getScore() == null ? 0 : item.getScore()),
                "blocking=" + item.isBlocking(),
                "escalationRequired=" + item.isEscalationRequired(),
                "hearingSensitive=" + item.isHearingSensitive(),
                "metadata=" + sanitizeBody(item.getMetadataJson()),
                "tags=" + sanitizeBody(item.getTagsJson())
        );
        String color = switch (eventType) {
            case "GABINETE_VOTO", "GABINETE_MINUTA", "GABINETE_CONCLUSAO", "GABINETE_PAUTA" -> item.isBlocking() ? "RED" : "PURPLE";
            case "SECRETARIA_FILA_AUDIENCIA", "SECRETARIA_PAUTA_INTERNA" -> item.isBlocking() ? "RED" : "BLUE";
            case "SECRETARIA_SLA" -> overdue(reference) || item.isEscalationRequired() ? "RED" : "AMBER";
            default -> item.isBlocking() ? "AMBER" : "BLUE";
        };
        String detailsUrl = processoId == null ? "/api/v1/secretariat/queue" : "/api/v1/processos/" + processoId;
        return new CalendarEventDto(
                eventType,
                -4000000L - item.getWorkItemId(),
                processoId,
                processoNumero,
                title + " • " + clip(firstNonBlank(item.getTitulo(), item.getQueueCode(), item.getInboxKey(), "Evento institucional"), 120),
                at,
                color,
                false,
                detailsUrl,
                body,
                "NATIVE:SECRETARIA_QUEUE:" + item.getWorkItemId() + ':' + eventType,
                "SECRETARIA_QUEUE"
        );
    }

    private String resolveSecretariatQueueEventType(SecretariatQueueItem item) {
        String raw = normalize(item.getInboxKey(), item.getQueueCode(), item.getLaneCode(), item.getDeskAxis(), item.getTitulo(), item.getTagsJson(), item.getMetadataJson(), item.getStatus());
        if (containsAny(raw, "PAUTA_SUSTENTACAO", "PAUTA DE GABINETE", "PAUTA_COLEGIADA", "SESSAO", "SESSÃO") && containsAny(raw, "GAB", "GABINETE", "CAMARA", "CÂMARA", "TURMA", "COLEGIADO")) {
            return "GABINETE_PAUTA";
        }
        if (containsAny(raw, "VOTO", "GABINETE_VOTO", "PAUTA_COLEGIADA")) {
            return "GABINETE_VOTO";
        }
        if (containsAny(raw, "MINUTA", "PULMAO_MINUTAS", "PULMÃO_MINUTAS")) {
            return "GABINETE_MINUTA";
        }
        if (containsAny(raw, "CONCLUSAO", "CONCLUSÃO", "CONCLUSO", "GABINETE")) {
            return "GABINETE_CONCLUSAO";
        }
        if (item.isHearingSensitive() || containsAny(raw, "AUDIENCIA", "AUDIÊNCIA", "PAUTA", "SALA")) {
            if (containsAny(raw, "PAUTA", "SALA", "RESERVA", "ESCALONAMENTO")) {
                return "SECRETARIA_PAUTA_INTERNA";
            }
            return "SECRETARIA_FILA_AUDIENCIA";
        }
        if (item.isBlocking() || item.isEscalationRequired() || item.isSecrecyReviewRequired() || containsAny(raw, "SLA", "TRIAGEM", "JUNTADA", "EXPEDIENTE", "REMESSA", "PENDENCIA", "PENDÊNCIA")) {
            return "SECRETARIA_SLA";
        }
        return "SECRETARIA_OPERACIONAL";
    }

    private boolean looksLikeInstitutionalInbox(String inboxKey) {
        String normalized = normalize(inboxKey);
        return containsAny(normalized, "SEC", "SECRETARIA", "GAB", "GABINETE", "ASSESSORIA", "PAUTA", "CARTORIO", "CARTÓRIO");
    }

    private boolean supportsInstitutionalQueue(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return false;
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        return tipo.isServidorJudiciario()
                || tipo.isAssessor()
                || tipo.isMagistratura()
                || tipo.isMinisterioPublico()
                || tipo.isDefensoriaPublica()
                || tipo.isProcuradoria();
    }

    private boolean isOfficialUser(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return false;
        }
        return usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA
                || usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
    }

    private boolean overdue(Instant reference) {
        return reference != null && reference.isBefore(Instant.now());
    }

    private EventProfile classifyWorkItem(Usuario usuario, WorkItem workItem) {
        String raw = normalize(
                workItem.getTemplateCode(),
                workItem.getQueueCode(),
                workItem.getInboxKey(),
                workItem.getTitulo(),
                workItem.getDescricao(),
                workItem.getBaseLegal(),
                workItem.getAssignedRole() == null ? null : workItem.getAssignedRole().name(),
                workItem.getType() == null ? null : workItem.getType().name()
        );
        String strongRaw = normalize(
                workItem.getTemplateCode(),
                workItem.getQueueCode(),
                workItem.getInboxKey(),
                workItem.getTitulo(),
                workItem.getAssignedRole() == null ? null : workItem.getAssignedRole().name(),
                workItem.getType() == null ? null : workItem.getType().name()
        );
        TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
        if (containsAny(raw, "PRECATORIO", "PRECATÓRIO", "RPV", "REQUISICAO_DE_PAGAMENTO", "REQUISIÇÃO DE PAGAMENTO")) {
            if (containsAny(raw, "ALVARA", "ALVARÁ", "LIBERACAO", "LIBERAÇÃO")) {
                return new EventProfile("PRECATORIO_LIBERACAO_OPERACIONAL", "Liberação de precatório", "PRECATORIO");
            }
            if (containsAny(raw, "RPV", "PEQUENO VALOR")) {
                return new EventProfile("PRECATORIO_RPV_OPERACIONAL", "RPV operacional", "PRECATORIO");
            }
            return new EventProfile("PRECATORIO_OPERACIONAL", "Precatório e RPV", "PRECATORIO");
        }
        if (containsAny(raw, "MANDADO", "DILIGEN", "CUMPRIMENTO", "CITACAO", "CITAÇÃO", "INTIMACAO_PESSOAL", "INTIMAÇÃO PESSOAL", "CERTIDAO_OFICIAL", "CERTIDÃO OFICIAL")
                || workItem.getType() == WorkItemType.DILIGENCIA) {
            if (containsAny(raw, "MULTIPLA TENTATIVA", "MÚLTIPLA TENTATIVA", "REITERACAO", "REITERAÇÃO", "SEGUNDA TENTATIVA", "TERCEIRA TENTATIVA")) {
                return new EventProfile("MANDADO_MULTI_TENTATIVA", "Janela de múltiplas tentativas", "MANDADO");
            }
            if (containsAny(raw, "RETORNO", "RETORN", "DEVOLU", "REEXPED")) {
                if (containsAny(raw, "JANELA", "ATO SEGUINTE", "FOLLOWUP", "PROXIMA PROVIDENCIA", "PRÓXIMA PROVIDÊNCIA")) {
                    return new EventProfile("MANDADO_JANELA_RETORNO", "Janela de retorno do mandado", "MANDADO");
                }
                return new EventProfile("MANDADO_RETORNO", "Retorno de mandado", "MANDADO");
            }
            if (containsAny(strongRaw, "CERTIDAO", "CERTIDÃO", "CERTIFICAR")) {
                return new EventProfile("MANDADO_CERTIDAO", "Certidão do oficial", "MANDADO");
            }
            if (containsAny(raw, "ROTA", "ROTEIRO", "ENDERECO", "ENDEREÇO", "JANELA EXTERNA")) {
                return new EventProfile("MANDADO_ROTA", "Rota de diligência", "MANDADO");
            }
            return new EventProfile("MANDADO_DILIGENCIA", "Mandado e diligência", "MANDADO");
        }
        if (containsAny(raw, "PERICIA", "PERÍCIA", "LAUDO", "QUESITO", "VISTORIA")
                || workItem.getType() == WorkItemType.PERICIA
                || workItem.getType() == WorkItemType.LAUDO) {
            if (containsAny(raw, "ENTREGA TECNICA", "ENTREGA TÉCNICA", "PROTOCOLO DE LAUDO", "ENTREGA DO LAUDO")) {
                return new EventProfile("PERICIA_ENTREGA_TECNICA", "Entrega técnica pericial", "PERICIA");
            }
            if (containsAny(raw, "LAUDO", "QUESITO")) {
                if (workItem.getStatus() == com.tcc.pjb.backend.model.entity.enums.WorkItemStatus.PENDENTE || containsAny(raw, "PENDENTE", "AGUARDANDO LAUDO", "PRAZO DO LAUDO")) {
                    return new EventProfile("PERICIA_LAUDO_PENDENTE", "Laudo pendente", "PERICIA");
                }
                return new EventProfile("PERICIA_LAUDO", "Laudo pericial", "PERICIA");
            }
            if (containsAny(raw, "HONORARIO", "HONORÁRIO", "PAGAMENTO")) {
                return new EventProfile("PERICIA_HONORARIOS", "Honorários periciais", "PERICIA");
            }
            if (containsAny(raw, "ACEITE", "NOMEACAO", "NOMEAÇÃO")) {
                return new EventProfile("PERICIA_ACEITE", "Aceite pericial", "PERICIA");
            }
            return new EventProfile("PERICIA_OPERACIONAL", "Perícia e laudo", "PERICIA");
        }
        if ((tipoUsuario != null && tipoUsuario.isServidorJudiciario())
                || containsAny(raw, "SECRETARIA", "CARTORIO", "CARTÓRIO", "EXPEDIENTE", "REMESSA", "TRIAGEM", "JUNTADA", "AUDIENCIA", "AUDIÊNCIA", "INTIMAR PARTES", "SALA DE ESPERA")) {
            if (containsAny(raw, "PAUTA INTERNA", "RESERVA DE SALA", "SALA DE AUDIENCIA", "SALA DE AUDIÊNCIA")) {
                return new EventProfile("SECRETARIA_PAUTA_INTERNA", "Pauta interna da secretaria", "SECRETARIA");
            }
            if (containsAny(raw, "AUDIENCIA", "AUDIÊNCIA", "PAUTA", "INTIMAR PARTES", "SALA DE ESPERA")) {
                return new EventProfile("SECRETARIA_AUDIENCIA", "Secretaria de audiência", "SECRETARIA");
            }
            if (containsAny(raw, "SLA", "DECURSO", "EXPEDIENTE", "REMESSA", "TRIAGEM", "JUNTADA", "PENDENCIA INTERNA", "PENDÊNCIA INTERNA")) {
                return new EventProfile("SECRETARIA_SLA", "SLA e expediente cartorário", "SECRETARIA");
            }
            return new EventProfile("SECRETARIA_OPERACIONAL", "Secretaria e cartório", "SECRETARIA");
        }
        if (containsAny(raw, "GABINETE", "MINUTA", "VOTO", "CONCLUSAO", "CONCLUSÃO", "CONCLUSO", "DESPACHO", "DECISAO", "DECISÃO", "PAUTA COLEGIADA", "SESSAO COLEGIADA", "SESSÃO COLEGIADA")) {
            if (containsAny(raw, "VOTO")) {
                return new EventProfile("GABINETE_VOTO", "Voto e sessão", "GABINETE");
            }
            if (containsAny(raw, "PAUTA", "SESSAO", "SESSÃO", "SUSTENTACAO ORAL", "SUSTENTAÇÃO ORAL")) {
                return new EventProfile("GABINETE_PAUTA", "Pauta de gabinete", "GABINETE");
            }
            if (containsAny(raw, "MINUTA", "MINUTAR", "RASCUNHO")) {
                return new EventProfile("GABINETE_MINUTA", "Minuta decisória", "GABINETE");
            }
            if (containsAny(raw, "CONCLUSAO", "CONCLUSÃO", "CONCLUSO")) {
                return new EventProfile("GABINETE_CONCLUSAO", "Conclusão para decisão", "GABINETE");
            }
            return new EventProfile("GABINETE_DECISORIO", "Gabinete decisório", "GABINETE");
        }
        if (containsAny(raw, "SESSAO", "SESSÃO", "PAUTA", "COLEGIADO", "JULGAMENTO", "SUSTENTACAO ORAL", "SUSTENTAÇÃO ORAL")) {
            if (containsAny(raw, "SUSTENTACAO ORAL", "SUSTENTAÇÃO ORAL")) {
                return new EventProfile("PAUTA_SUSTENTACAO", "Pauta de sustentação", "PAUTA");
            }
            return new EventProfile("PAUTA_COLEGIADA", "Sessão e pauta", "PAUTA");
        }
        if (containsAny(raw, "EMBARG", "AGRAVO", "APELA", "RESP", "RECURSO", "CONTRARRAZO", "CONTRARRAZÃO", "PRAZO", "CONTESTACAO", "CONTESTAÇÃO", "DEFESA", "PARECER", "MANIFESTACAO", "MANIFESTAÇÃO")
                || workItem.getType() == WorkItemType.RECURSO
                || workItem.getType() == WorkItemType.MANIFESTACAO) {
            if (containsAny(raw, "EMBARG")) {
                return new EventProfile("PRAZO_EMBARGOS_OPERACIONAL", "Embargos e ajuste", "PRAZO");
            }
            if (containsAny(raw, "AGRAVO", "APELA", "RESP", "RECURSO")) {
                return new EventProfile("PRAZO_RECURSAL_OPERACIONAL", "Prazo recursal", "PRAZO");
            }
            return new EventProfile("PRAZO_INSTITUCIONAL", "Prazo institucional", "PRAZO");
        }
        return new EventProfile("AGENDA_OPERACIONAL", "Agenda institucional", "OPERACIONAL");
    }

    private static String resolvePericiaEventType(String status, String observacaoNormalizada) {
        if (containsAny(observacaoNormalizada, "ENTREGA TECNICA", "ENTREGA TÉCNICA", "PROTOCOLO DE LAUDO")) {
            return "PERICIA_ENTREGA_TECNICA";
        }
        if (containsAny(observacaoNormalizada, "LAUDO")) {
            return "PERICIA_LAUDO";
        }
        if ("ACEITO".equals(status) || "RECUSADO".equals(status) || "REVOGADO".equals(status)
                || containsAny(observacaoNormalizada, "ACEITE", "HONORARIO", "HONORÁRIO", "QUESITO", "QUESITOS", "PROPOSTA HONORARIA", "PROPOSTA HONORÁRIA")) {
            return "PERICIA_ACEITE";
        }
        if ("NOMEADO".equals(status)) {
            return "PERICIA_ACEITE";
        }
        return "PERICIA_NOMEACAO";
    }

    private static LocalDateTime resolvePericiaDate(PeritoNomeacao nomeacao, String status) {
        if (nomeacao == null) {
            return null;
        }
        if ("REVOGADO".equals(status) && nomeacao.getRevogadoEm() != null) {
            return nomeacao.getRevogadoEm();
        }
        if (("ACEITO".equals(status) || "RECUSADO".equals(status)) && nomeacao.getRespondidoEm() != null) {
            return nomeacao.getRespondidoEm();
        }
        return nomeacao.getNomeadoEm();
    }

    private static Instant firstNonNull(Instant first, Instant second, Instant third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
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

    private static String safe(Object value) {
        return value == null ? "" : sanitizeBody(String.valueOf(value));
    }

    private static String clip(String value, int max) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    private static String normalize(String... values) {
        StringBuilder builder = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(value.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return builder.toString();
    }

    private static boolean containsAny(String raw, String... tokens) {
        if (raw == null || raw.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && raw.contains(token.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static long generatedNegativeId(String seed, long floor) {
        long hash = Math.abs(seed == null ? 0 : seed.hashCode());
        return -(floor + hash);
    }

    private static String sanitizeBody(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > 320 ? normalized.substring(0, 320) : normalized;
    }

    private record EventProfile(
            String eventType,
            String prefix,
            String category
    ) {
        String title(String baseTitle) {
            String normalized = baseTitle == null || baseTitle.isBlank() ? prefix : baseTitle.trim();
            if (normalized.toUpperCase(Locale.ROOT).startsWith(prefix.toUpperCase(Locale.ROOT))) {
                return clip(normalized, 180);
            }
            return clip(prefix + " • " + normalized, 180);
        }

        String body(WorkItem workItem) {
            List<String> lines = new ArrayList<>();
            lines.add("categoria=" + category);
            lines.add("operacao=" + eventType);
            lines.add("template=" + safe(workItem.getTemplateCode()));
            lines.add("queue=" + safe(workItem.getQueueCode()));
            lines.add("inbox=" + safe(workItem.getInboxKey()));
            lines.add("status=" + (workItem.getStatus() == null ? "" : workItem.getStatus().name()));
            lines.add("prioridade=" + (workItem.getPrioridade() == null ? "" : workItem.getPrioridade()));
            lines.add("blocking=" + workItem.isBlocking());
            lines.add("fase=" + (workItem.getFaseOrigem() == null ? "" : workItem.getFaseOrigem().name()));
            lines.add("papel=" + (workItem.getAssignedRole() == null ? "" : workItem.getAssignedRole().name()));
            lines.add("territorio=" + safe(firstNonBlank(workItem.getUf(), workItem.getComarca())));
            lines.add("baseLegal=" + sanitizeBody(workItem.getBaseLegal()));
            lines.add("descricao=" + sanitizeBody(workItem.getDescricao()));
            return String.join("\n", lines);
        }

        String color(LocalDateTime at, boolean blocking) {
            if ("PRECATORIO".equals(category)) {
                return "GREEN";
            }
            if ("GABINETE".equals(category) || "PAUTA".equals(category)) {
                return blocking ? "RED" : "PURPLE";
            }
            if ("MANDADO".equals(category)) {
                return blocking ? "RED" : "BLUE";
            }
            if ("PERICIA".equals(category) || "SECRETARIA".equals(category)) {
                return blocking ? "AMBER" : "BLUE";
            }
            LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
            if (!at.isAfter(now.plusHours(24))) {
                return "RED";
            }
            if (!at.isAfter(now.plusDays(3))) {
                return "AMBER";
            }
            return "AMBER";
        }

        private static String safe(String value) {
            return value == null ? "" : clip(value.trim(), 140);
        }


    }
}
