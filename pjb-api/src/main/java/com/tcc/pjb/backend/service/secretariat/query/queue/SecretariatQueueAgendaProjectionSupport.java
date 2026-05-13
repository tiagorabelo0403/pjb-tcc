package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueActionContractDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaChecklistItemDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaCompletionDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaContactDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaGroupDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaItemDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaNotificationSummaryDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaVenueDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueDeadlineBucketDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueFilterGroupDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class SecretariatQueueAgendaProjectionSupport {

    private final SecretariatQueuePanelProjectionSupport panelProjectionSupport;

    SecretariatQueueAgendaProjectionSupport(SecretariatQueuePanelProjectionSupport panelProjectionSupport) {
        this.panelProjectionSupport = Objects.requireNonNull(panelProjectionSupport);
    }

    SecretariatQueueAgendaSnapshotDto buildSnapshot(SecretariatQueueInboxContext context,
                                                    SecretariatQueueAgendaFilter effectiveFilter,
                                                    Instant from,
                                                    Instant to,
                                                    Instant now,
                                                    List<SecretariatQueuePanelRow> rows) {
        List<SecretariatQueueAgendaItemDto> items = rows.stream()
            .sorted(PANEL_ROW_ORDER)
            .map(this::toAgendaItem)
            .toList();
        List<SecretariatQueueAgendaGroupDto> byData = groupAgendaRows("DATA", rows, SecretariatQueuePanelRow::dataKey, SecretariatQueuePanelRow::dataLabel);
        List<SecretariatQueueAgendaGroupDto> byVara = groupAgendaRows("VARA", rows, SecretariatQueuePanelRow::varaKey, SecretariatQueuePanelRow::varaLabel);
        List<SecretariatQueueAgendaGroupDto> byRito = groupAgendaRows("RITO", rows, SecretariatQueuePanelRow::ritoKey, SecretariatQueuePanelRow::ritoLabel);
        List<SecretariatQueueAgendaGroupDto> byCell = groupAgendaRows("CELULA", rows, SecretariatQueuePanelRow::cellKey, SecretariatQueuePanelRow::cellLabel);
        List<SecretariatQueueAgendaGroupDto> byResponsible = groupAgendaRows("RESPONSAVEL", rows, SecretariatQueuePanelRow::responsibleKey, SecretariatQueuePanelRow::responsibleLabel);
        List<SecretariatQueueAgendaGroupDto> byCategory = groupAgendaRows("CATEGORIA", rows, SecretariatQueuePanelRow::categoryKey, SecretariatQueuePanelRow::categoryLabel);
        List<SecretariatQueueAgendaGroupDto> byTrack = groupAgendaRows("TRILHA", rows, SecretariatQueuePanelRow::trackKey, SecretariatQueuePanelRow::trackLabel);
        List<SecretariatQueueAgendaGroupDto> byConfirmation = groupAgendaRows("CONFIRMACAO", rows, SecretariatQueuePanelRow::confirmationKey, SecretariatQueuePanelRow::confirmationLabel);
        List<SecretariatQueueAgendaGroupDto> byVenueConfirmation = groupAgendaRows("LOCAL", rows, SecretariatQueuePanelRow::venueConfirmationKey, SecretariatQueuePanelRow::venueConfirmationLabel);
        List<SecretariatQueueAgendaGroupDto> byParticipantNotification = groupAgendaRows("INTIMACAO", rows, SecretariatQueuePanelRow::participantNotificationKey, SecretariatQueuePanelRow::participantNotificationLabel);
        List<SecretariatQueueAgendaGroupDto> byAttendance = groupAgendaRows("COMPARECIMENTO", rows, SecretariatQueuePanelRow::attendanceKey, SecretariatQueuePanelRow::attendanceLabel);
        List<SecretariatQueueAgendaGroupDto> byReturnStatus = groupAgendaRows("RETORNO", rows, SecretariatQueuePanelRow::returnKey, SecretariatQueuePanelRow::returnLabel);
        List<SecretariatQueueDeadlineBucketDto> deadlineBuckets = buildDeadlineBuckets(rows, now);
        List<SecretariatQueueFilterGroupDto> filters = buildAgendaFilters(rows);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("rangeStart", from);
        metadata.put("rangeEnd", to);
        metadata.put("totalItems", items.size());
        metadata.put("contactReadyCount", items.stream().flatMap(item -> item.contacts().stream()).filter(SecretariatQueueAgendaContactDto::contactReady).count());
        metadata.put("contactMissingCount", items.stream().flatMap(item -> item.contacts().stream()).filter(contact -> !contact.contactReady()).count());
        metadata.put("hearingItems", rows.stream().filter(SecretariatQueuePanelRow::hearingSensitive).count());
        metadata.put("sessionItems", rows.stream().filter(row -> "SESSAO_COLEGIADA".equals(row.eventTrack())).count());
        metadata.put("pendingConfirmationCount", rows.stream().filter(row -> "PENDENTE_CONFIRMACAO".equals(row.confirmationStatus())).count());
        metadata.put("delayedConfirmationCount", rows.stream().filter(row -> "ATRASADO_SEM_CONFIRMACAO".equals(row.confirmationStatus())).count());
        metadata.put("attendancePendingCount", rows.stream().filter(row -> "AGUARDANDO_REALIZACAO".equals(row.attendanceStatus())).count());
        metadata.put("attendanceRecordedCount", rows.stream().filter(row -> !List.of("NAO_APLICAVEL", "AGUARDANDO_REALIZACAO").contains(row.attendanceStatus())).count());
        metadata.put("processReturnExecutedCount", rows.stream().filter(row -> "RETORNO_EXECUTADO".equals(row.processReturnStatus())).count());
        metadata.put("actionableItemsCount", items.stream().filter(item -> item.actionContracts() != null && item.actionContracts().stream().anyMatch(SecretariatQueueActionContractDto::enabled)).count());
        metadata.put("reentryMaterializedCount", items.stream().filter(item -> longValue(stringValue(item.metadata().get("reentryWorkItemId"))) != null).count());
        metadata.put("processReturnReadyCount", rows.stream().filter(SecretariatQueuePanelRow::autoReturnReady).count());
        metadata.put("byCellCount", byCell.size());
        metadata.put("byResponsibleCount", byResponsible.size());
        metadata.put("byCategoryCount", byCategory.size());
        metadata.put("byTrackCount", byTrack.size());
        metadata.put("byConfirmationCount", byConfirmation.size());
        metadata.put("byVenueConfirmationCount", byVenueConfirmation.size());
        metadata.put("byParticipantNotificationCount", byParticipantNotification.size());
        metadata.put("byAttendanceCount", byAttendance.size());
        metadata.put("byReturnCount", byReturnStatus.size());
        metadata.put("venueConfirmedCount", rows.stream().filter(row -> "LOCAL_CONFIRMADO".equals(row.venueConfirmationStatus())).count());
        metadata.put("venuePendingCount", rows.stream().filter(row -> "PENDENTE_LOCAL".equals(row.venueConfirmationStatus())).count());
        metadata.put("participantNotificationConfirmedCount", rows.stream().filter(row -> "PARTICIPANTES_INTIMADOS".equals(row.participantNotificationStatus())).count());
        metadata.put("participantNotificationPendingCount", rows.stream().filter(row -> "PENDENTE_INTIMACAO".equals(row.participantNotificationStatus())).count());
        metadata.put("participantNotificationIncompleteCount", rows.stream().filter(row -> "CONTATOS_INCOMPLETOS".equals(row.participantNotificationStatus())).count());
        metadata.put("attendancePresentCount", rows.stream().filter(row -> "PRESENTE".equals(row.attendanceStatus())).count());
        metadata.put("attendanceAbsentCount", rows.stream().filter(row -> "AUSENTE".equals(row.attendanceStatus())).count());
        metadata.put("completionEventReadyCount", rows.stream().filter(row -> "EVENTO_REAL_REGISTRADO".equals(row.completionEventStatus())).count());
        metadata.put("availableRitos", panelProjectionSupport.buildRitoCatalog());
        metadata.put("ritoCoverage", panelProjectionSupport.buildRitoCoverage(rows));
        metadata.put("activeFilter", effectiveFilter.toMap());
        metadata.put("availableAxes", List.of("DATA", "VARA", "RITO", "CELULA", "RESPONSAVEL", "CATEGORIA", "TRILHA", "CONFIRMACAO", "LOCAL", "INTIMACAO", "COMPARECIMENTO", "RETORNO"));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new SecretariatQueueAgendaSnapshotDto(
            context.inboxKey(),
            context.inboxDescriptor(),
            context.portfolio().operationalDescriptor(),
            byData,
            byVara,
            byRito,
            byCell,
            byResponsible,
            byCategory,
            byTrack,
            byConfirmation,
            byVenueConfirmation,
            byParticipantNotification,
            byAttendance,
            byReturnStatus,
            deadlineBuckets,
            filters,
            items,
            Collections.unmodifiableMap(metadata)
        );
    }

    boolean matchesAgendaFilter(SecretariatQueuePanelRow row, SecretariatQueueAgendaFilter filter) {
        if (filter == null) {
            return true;
        }
        return matchesToken(filter.tribunal(), row.tribunalCodigo())
            && matchesToken(filter.foro(), row.foroLabel())
            && matchesToken(filter.vara(), row.varaLabel())
            && matchesToken(filter.orgao(), row.organLabel())
            && matchesToken(filter.secretaria(), row.secretariatLabel())
            && matchesToken(filter.rito(), row.ritoProcessual())
            && matchesToken(filter.cellCode(), row.cellCode())
            && matchesToken(filter.responsavel(), row.responsibleName())
            && matchesToken(filter.categoria(), row.categoryLabel());
    }

    private SecretariatQueueAgendaItemDto toAgendaItem(SecretariatQueuePanelRow row) {
        return new SecretariatQueueAgendaItemDto(
            row.workItemId(),
            row.processoId(),
            row.processoNumero(),
            row.titulo(),
            row.queueCode(),
            row.stage(),
            row.status(),
            row.prioridade(),
            row.referenceAt(),
            row.dueAt(),
            row.ritoProcessual(),
            row.classeProcessual(),
            row.varaLabel(),
            row.unidadeCodigo(),
            row.comarca(),
            row.tribunalCodigo(),
            row.foroLabel(),
            row.secretariatLabel(),
            row.cellCode(),
            row.responsibleUserId(),
            row.responsibleName(),
            row.categoryLabel(),
            row.eventTrack(),
            row.confirmationStatus(),
            row.attendanceStatus(),
            row.processReturnStatus(),
            row.processReturnRoute(),
            row.autoReturnReady(),
            row.venueConfirmationStatus(),
            row.participantNotificationStatus(),
            row.completionEventStatus(),
            panelProjectionSupport.resolveTargetPanelRoute(row),
            extractVenue(row),
            extractNotificationSummary(row),
            extractCompletion(row),
            extractChecklist(row),
            extractAgendaContacts(row.metadata()),
            panelProjectionSupport.buildActionContracts(row),
            row.metadata()
        );
    }

    private SecretariatQueueAgendaVenueDto extractVenue(SecretariatQueuePanelRow row) {
        Map<String, Object> venue = nestedMap(row.metadata(), "venue");
        Instant confirmedAt = firstInstant(
            instantValue(venue.get("confirmedAt")),
            instantValue(venue.get("confirmationAt")),
            instantValue(row.metadata().get("venueConfirmedAt"))
        );
        String confirmationStatus = firstNonBlank(
            stringValue(venue.get("confirmationStatus")),
            row.venueConfirmationStatus(),
            "NAO_APLICAVEL"
        );
        boolean confirmed = "LOCAL_CONFIRMADO".equals(confirmationStatus) || confirmedAt != null;
        return new SecretariatQueueAgendaVenueDto(
            firstNonBlank(stringValue(venue.get("modality")), stringValue(venue.get("modalidade"))),
            firstNonBlank(stringValue(venue.get("locationLabel")), stringValue(venue.get("location")), stringValue(venue.get("local")), row.secretariatLabel(), row.foroLabel()),
            firstNonBlank(stringValue(venue.get("roomLabel")), stringValue(venue.get("room")), stringValue(venue.get("sala"))),
            firstNonBlank(stringValue(venue.get("virtualLink")), stringValue(venue.get("link")), stringValue(venue.get("meetingLink"))),
            confirmationStatus,
            confirmed,
            confirmedAt
        );
    }

    private SecretariatQueueAgendaNotificationSummaryDto extractNotificationSummary(SecretariatQueuePanelRow row) {
        Map<String, Object> notification = nestedMap(row.metadata(), "participantNotification");
        long readyCount = longFromObject(firstNonBlank(stringValue(notification.get("readyCount")), stringValue(row.metadata().get("contactReadyCount"))));
        long missingCount = longFromObject(firstNonBlank(stringValue(notification.get("missingCount")), stringValue(row.metadata().get("contactMissingCount"))));
        long pendingCount = longFromObject(stringValue(notification.get("pendingCount")));
        if (pendingCount <= 0) {
            long total = extractAgendaContacts(row.metadata()).size();
            pendingCount = Math.max(0L, total - readyCount);
        }
        return new SecretariatQueueAgendaNotificationSummaryDto(
            firstNonBlank(stringValue(notification.get("status")), row.participantNotificationStatus(), "NAO_APLICAVEL"),
            readyCount,
            pendingCount,
            missingCount
        );
    }

    private SecretariatQueueAgendaCompletionDto extractCompletion(SecretariatQueuePanelRow row) {
        Instant occurredAt = firstInstant(
            instantValue(row.metadata().get("completionEventOccurredAt")),
            instantValue(row.metadata().get("completionOccurredAt")),
            instantValue(row.metadata().get("completionCertifiedAt"))
        );
        boolean ready = row.autoReturnReady() || "EVENTO_REAL_REGISTRADO".equals(row.completionEventStatus());
        return new SecretariatQueueAgendaCompletionDto(
            firstNonBlank(stringValue(row.metadata().get("completionEvent")), stringValue(row.metadata().get("eventoConclusao"))),
            firstNonBlank(row.completionEventStatus(), "NAO_APLICAVEL"),
            occurredAt,
            ready,
            row.processReturnRoute()
        );
    }

    private List<SecretariatQueueAgendaChecklistItemDto> extractChecklist(SecretariatQueuePanelRow row) {
        Object rawChecklist = row.metadata().get("operationalChecklist");
        List<SecretariatQueueAgendaChecklistItemDto> out = new ArrayList<>();
        if (rawChecklist instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String code = stringValue(map.get("code"));
                    String label = firstNonBlank(stringValue(map.get("label")), code);
                    String status = firstNonBlank(stringValue(map.get("status")), "PENDENTE");
                    boolean blocking = Boolean.parseBoolean(String.valueOf(map.containsKey("blocking") ? map.get("blocking") : Boolean.FALSE));
                    Instant confirmedAt = firstInstant(
                        instantValue(map.get("confirmedAt")),
                        instantValue(map.get("doneAt")),
                        instantValue(map.get("completedAt"))
                    );
                    out.add(new SecretariatQueueAgendaChecklistItemDto(code, label, status, blocking, confirmedAt));
                }
            }
        }
        if (!out.isEmpty()) {
            return List.copyOf(out);
        }
        return defaultChecklist(row);
    }

    private List<SecretariatQueueAgendaChecklistItemDto> defaultChecklist(SecretariatQueuePanelRow row) {
        List<SecretariatQueueAgendaChecklistItemDto> items = new ArrayList<>();
        String track = firstNonBlank(row.eventTrack(), "OPERACIONAL_GERAL");
        items.add(new SecretariatQueueAgendaChecklistItemDto("ROTEAMENTO_UNIDADE", "Roteamento para unidade e secretaria corretas", "OK", true, row.referenceAt()));
        if (List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA").contains(track)) {
            items.add(new SecretariatQueueAgendaChecklistItemDto("LOCAL_LINK", "Confirmação de sala, local ou link", row.venueConfirmationStatus(), true, extractVenue(row).confirmedAt()));
            items.add(new SecretariatQueueAgendaChecklistItemDto("PARTICIPANTES_INTIMADOS", "Confirmação de intimação dos participantes", row.participantNotificationStatus(), true, null));
            items.add(new SecretariatQueueAgendaChecklistItemDto("COMPARECIMENTO", "Registro de presença ou ausência", row.attendanceStatus(), false, null));
            items.add(new SecretariatQueueAgendaChecklistItemDto("EVENTO_REAL", "Registro do evento real para retorno automático", row.completionEventStatus(), true, extractCompletion(row).occurredAt()));
        } else if ("COMUNICACAO_PROCESSUAL".equals(track)) {
            items.add(new SecretariatQueueAgendaChecklistItemDto("DESTINATARIOS", "Destinatários e canal confirmados", row.confirmationStatus(), true, null));
            items.add(new SecretariatQueueAgendaChecklistItemDto("PARTICIPANTES_INTIMADOS", "Confirmação de intimação/citação", row.participantNotificationStatus(), true, null));
            items.add(new SecretariatQueueAgendaChecklistItemDto("EVENTO_REAL", "Certificação do cumprimento real", row.completionEventStatus(), true, extractCompletion(row).occurredAt()));
        } else {
            items.add(new SecretariatQueueAgendaChecklistItemDto("EXECUCAO_ITEM", "Execução operacional do item", row.confirmationStatus(), false, null));
        }
        return List.copyOf(items);
    }

    private List<SecretariatQueueAgendaGroupDto> groupAgendaRows(String axis,
                                                                 List<SecretariatQueuePanelRow> rows,
                                                                 java.util.function.Function<SecretariatQueuePanelRow, String> keyExtractor,
                                                                 java.util.function.Function<SecretariatQueuePanelRow, String> labelExtractor) {
        LinkedHashMap<String, List<SecretariatQueuePanelRow>> grouped = new LinkedHashMap<>();
        rows.stream()
            .sorted(PANEL_ROW_ORDER)
            .forEach(row -> grouped.computeIfAbsent(normalizeGroupKey(keyExtractor.apply(row)), ignored -> new ArrayList<>()).add(row));

        List<SecretariatQueueAgendaGroupDto> out = new ArrayList<>();
        for (Map.Entry<String, List<SecretariatQueuePanelRow>> entry : grouped.entrySet()) {
            List<SecretariatQueuePanelRow> groupRows = entry.getValue();
            if (groupRows.isEmpty()) {
                continue;
            }
            LinkedHashSet<Long> processIds = new LinkedHashSet<>();
            long contactReadyCount = 0L;
            List<SecretariatQueueAgendaItemDto> items = new ArrayList<>(groupRows.size());
            for (SecretariatQueuePanelRow row : groupRows) {
                if (row.processoId() != null) {
                    processIds.add(row.processoId());
                }
                SecretariatQueueAgendaItemDto item = toAgendaItem(row);
                contactReadyCount += item.contacts().stream().filter(SecretariatQueueAgendaContactDto::contactReady).count();
                items.add(item);
            }
            SecretariatQueuePanelRow first = groupRows.get(0);
            out.add(new SecretariatQueueAgendaGroupDto(
                axis,
                entry.getKey(),
                firstNonBlank(labelExtractor.apply(first), entry.getKey()),
                first.referenceAt(),
                groupRows.size(),
                processIds.size(),
                contactReadyCount,
                List.copyOf(items)
            ));
        }
        out.sort(Comparator
            .comparing(SecretariatQueueAgendaGroupDto::referenceAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SecretariatQueueAgendaGroupDto::groupLabel, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    private List<SecretariatQueueAgendaContactDto> extractAgendaContacts(Map<String, Object> metadata) {
        LinkedHashMap<String, SecretariatQueueAgendaContactDto> contacts = new LinkedHashMap<>();
        Map<String, Object> envelope = nestedMap(metadata, "contactEnvelope");
        addAgendaContact(contacts, envelope.get("autor"), "AUTOR", "AUTOR");
        addAgendaContact(contacts, envelope.get("reu"), "REU", "REU");
        Object rawAdvogados = envelope.get("advogados");
        if (rawAdvogados instanceof List<?> list) {
            for (Object item : list) {
                addAgendaContact(contacts, item, "ADVOGADO", "INDETERMINADO");
            }
        }
        if (contacts.isEmpty()) {
            Object rawParticipants = metadata.get("participants");
            if (rawParticipants instanceof List<?> list) {
                for (Object item : list) {
                    addAgendaContact(contacts, item, "PARTE", null);
                }
            }
        }
        return List.copyOf(contacts.values());
    }

    @SuppressWarnings("unchecked")
    private void addAgendaContact(LinkedHashMap<String, SecretariatQueueAgendaContactDto> contacts,
                                  Object raw,
                                  String defaultRole,
                                  String defaultSide) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        String role = firstNonBlank(stringValue(map.get("role")), defaultRole);
        String side = firstNonBlank(stringValue(map.get("side")), defaultSide);
        String nome = firstNonBlank(stringValue(map.get("nome")), stringValue(map.get("name")));
        String documento = firstNonBlank(stringValue(map.get("documento")), stringValue(map.get("cpf")));
        String email = stringValue(map.get("email"));
        String telefone = stringValue(map.get("telefone"));
        String numeroOab = firstNonBlank(stringValue(map.get("numeroOab")), stringValue(map.get("oab")));
        boolean contactReady = Boolean.TRUE.equals(map.get("contactReady")) || "true".equalsIgnoreCase(stringValue(map.get("contactReady")));
        String source = firstNonBlank(stringValue(map.get("source")), "METADATA");
        if (nome == null && documento == null && email == null && telefone == null && numeroOab == null) {
            return;
        }
        String key = firstNonBlank(stringValue(map.get("usuarioId")), email, documento, role + ':' + nome);
        contacts.putIfAbsent(key, new SecretariatQueueAgendaContactDto(role, side, nome, documento, email, telefone, numeroOab, contactReady, source));
    }

    private List<SecretariatQueueDeadlineBucketDto> buildDeadlineBuckets(List<SecretariatQueuePanelRow> rows, Instant now) {
        List<BucketSpec> specs = List.of(
            new BucketSpec("OVERDUE", "Vencidos"),
            new BucketSpec("DUE_24H", "Vencendo em 24h"),
            new BucketSpec("DUE_72H", "Vencendo em 72h"),
            new BucketSpec("DUE_7D", "Vencendo em 7 dias"),
            new BucketSpec("SCHEDULED", "Agendados depois de 7 dias"),
            new BucketSpec("NO_DUE", "Sem data operacional")
        );
        List<SecretariatQueueDeadlineBucketDto> out = new ArrayList<>();
        for (BucketSpec spec : specs) {
            List<SecretariatQueuePanelRow> bucketRows = rows.stream().filter(row -> sameDeadlineBucket(spec.code(), row, now)).toList();
            LinkedHashSet<Long> processIds = new LinkedHashSet<>();
            List<Long> workItemIds = new ArrayList<>();
            for (SecretariatQueuePanelRow row : bucketRows) {
                if (row.processoId() != null) {
                    processIds.add(row.processoId());
                }
                if (row.workItemId() != null) {
                    workItemIds.add(row.workItemId());
                }
            }
            out.add(new SecretariatQueueDeadlineBucketDto(spec.code(), spec.label(), bucketRows.size(), processIds.size(), List.copyOf(workItemIds)));
        }
        return List.copyOf(out);
    }

    private List<SecretariatQueueFilterGroupDto> buildAgendaFilters(List<SecretariatQueuePanelRow> rows) {
        return List.of(
            filterGroup("tribunal", "Tribunal", rows.stream().map(SecretariatQueuePanelRow::tribunalCodigo).toList()),
            filterGroup("foro", "Foro / Comarca", rows.stream().map(SecretariatQueuePanelRow::foroLabel).toList()),
            filterGroup("vara", "Vara / Unidade", rows.stream().map(SecretariatQueuePanelRow::varaLabel).toList()),
            filterGroup("orgao", "Órgão julgador", rows.stream().map(SecretariatQueuePanelRow::organLabel).toList()),
            filterGroup("secretaria", "Secretaria", rows.stream().map(SecretariatQueuePanelRow::secretariatLabel).toList()),
            filterGroup("rito", "Rito processual", rows.stream().map(SecretariatQueuePanelRow::ritoProcessual).toList()),
            filterGroup("cellCode", "Célula operacional", rows.stream().map(SecretariatQueuePanelRow::cellCode).toList()),
            filterGroup("responsavel", "Responsável", rows.stream().map(SecretariatQueuePanelRow::responsibleName).toList()),
            filterGroup("categoria", "Categoria operacional", rows.stream().map(SecretariatQueuePanelRow::categoryLabel).toList()),
            filterGroup("trilha", "Trilha operacional", rows.stream().map(SecretariatQueuePanelRow::eventTrack).toList()),
            filterGroup("confirmacao", "Confirmação operacional", rows.stream().map(SecretariatQueuePanelRow::confirmationStatus).toList()),
            filterGroup("local", "Confirmação de local", rows.stream().map(SecretariatQueuePanelRow::venueConfirmationStatus).toList()),
            filterGroup("intimacao", "Intimação de participantes", rows.stream().map(SecretariatQueuePanelRow::participantNotificationStatus).toList()),
            filterGroup("comparecimento", "Comparecimento", rows.stream().map(SecretariatQueuePanelRow::attendanceStatus).toList()),
            filterGroup("retorno", "Retorno ao processo", rows.stream().map(SecretariatQueuePanelRow::processReturnStatus).toList())
        );
    }

    private SecretariatQueueFilterGroupDto filterGroup(String code, String label, List<String> rawValues) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (rawValues != null) {
            for (String value : rawValues) {
                String normalized = firstNonBlank(value);
                if (normalized != null) {
                    values.add(normalized);
                }
            }
        }
        return new SecretariatQueueFilterGroupDto(code, label, List.copyOf(values));
    }

    private boolean matchesToken(String filterValue, String actualValue) {
        if (isBlank(filterValue)) {
            return true;
        }
        if (isBlank(actualValue)) {
            return false;
        }
        return normalize(actualValue).contains(normalize(filterValue));
    }

    private boolean sameDeadlineBucket(String bucketCode, SecretariatQueuePanelRow row, Instant now) {
        return Objects.equals(bucketCode, deadlineBucket(row.dueAt(), now));
    }

    private String deadlineBucket(Instant dueAt, Instant now) {
        if (dueAt == null) {
            return "NO_DUE";
        }
        if (dueAt.isBefore(now)) {
            return "OVERDUE";
        }
        if (!dueAt.isAfter(now.plusSeconds(24 * 60 * 60L))) {
            return "DUE_24H";
        }
        if (!dueAt.isAfter(now.plusSeconds(72 * 60 * 60L))) {
            return "DUE_72H";
        }
        if (!dueAt.isAfter(now.plusSeconds(7 * 24 * 60 * 60L))) {
            return "DUE_7D";
        }
        return "SCHEDULED";
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

    private static long longFromObject(String raw) {
        Long value = longValue(raw);
        return value == null ? 0L : value;
    }

    private static Long longValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Instant instantValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Instant instant) {
            return instant;
        }
        try {
            return Instant.parse(String.valueOf(raw));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Instant firstInstant(Instant... values) {
        if (values == null) {
            return null;
        }
        for (Instant value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isBlank() ? null : value;
    }

    private static String normalizeGroupKey(String value) {
        return firstNonBlank(value, "SEM_GRUPO");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT)
            .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
            .replace('É', 'E').replace('Ê', 'E')
            .replace('Í', 'I')
            .replace('Ó', 'O').replace('Õ', 'O').replace('Ô', 'O')
            .replace('Ú', 'U')
            .replace('Ç', 'C');
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

    private static final Comparator<SecretariatQueuePanelRow> PANEL_ROW_ORDER = Comparator
        .comparing(SecretariatQueuePanelRow::referenceAt, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(SecretariatQueuePanelRow::prioridade, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(SecretariatQueuePanelRow::processoNumero, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(SecretariatQueuePanelRow::workItemId, Comparator.nullsLast(Comparator.naturalOrder()));
}
