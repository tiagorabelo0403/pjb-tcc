package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueActionContractDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueItemDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelGroupDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelSnapshotDto;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SecretariatQueuePanelProjectionSupport {

    SecretariatQueuePanelSnapshotDto buildSnapshot(SecretariatQueueInboxContext context,
                                                   List<SecretariatQueuePanelRow> rows) {
        List<SecretariatQueuePanelGroupDto> byProcesso = groupRows("PROCESSO", rows, SecretariatQueuePanelRow::processoKey, SecretariatQueuePanelRow::processoLabel);
        List<SecretariatQueuePanelGroupDto> byRito = groupRows("RITO", rows, SecretariatQueuePanelRow::ritoKey, SecretariatQueuePanelRow::ritoLabel);
        List<SecretariatQueuePanelGroupDto> byVara = groupRows("VARA", rows, SecretariatQueuePanelRow::varaKey, SecretariatQueuePanelRow::varaKey);
        List<SecretariatQueuePanelGroupDto> byData = groupRows("DATA", rows, SecretariatQueuePanelRow::dataKey, SecretariatQueuePanelRow::dataLabel);

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("loadBand", context.loadProfile().loadBand());
        metadata.put("responseMode", context.loadProfile().responseMode());
        metadata.put("dashboardBucket", context.dashboardBucket());
        metadata.put("deskDescriptor", context.portfolio().operationalDescriptor());
        metadata.put("institutionalVisibility", context.inboxProfile().toMap());
        metadata.put("totalItems", rows.size());
        metadata.put("processGroupCount", byProcesso.size());
        metadata.put("ritoGroupCount", byRito.size());
        metadata.put("varaGroupCount", byVara.size());
        metadata.put("dateGroupCount", byData.size());
        metadata.put("overdueItems", rows.stream().filter(SecretariatQueuePanelRow::overdue).count());
        metadata.put("urgentItems", rows.stream().filter(SecretariatQueuePanelRow::urgent).count());
        metadata.put("hearingItems", rows.stream().filter(SecretariatQueuePanelRow::hearingSensitive).count());
        metadata.put("secrecyItems", rows.stream().filter(SecretariatQueuePanelRow::secrecyReviewRequired).count());
        metadata.put("maxUrgencyScore", rows.stream().mapToInt(SecretariatQueuePanelRow::urgencyScore).max().orElse(0));
        metadata.put("avgUrgencyScore", rows.isEmpty() ? 0.0 : rows.stream().mapToInt(SecretariatQueuePanelRow::urgencyScore).average().orElse(0.0));
        metadata.put("availableAxes", List.of("PROCESSO", "RITO", "VARA", "DATA"));
        metadata.put("availableRitos", buildRitoCatalog());
        metadata.put("ritoCoverage", buildRitoCoverage(rows));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);

        return new SecretariatQueuePanelSnapshotDto(
            context.inboxKey(),
            context.inboxDescriptor(),
            context.portfolio().operationalDescriptor(),
            context.dashboardBucket(),
            byProcesso,
            byRito,
            byVara,
            byData,
            Collections.unmodifiableMap(metadata)
        );
    }

    List<SecretariatQueueActionContractDto> buildActionContracts(SecretariatQueuePanelRow row) {
        String targetPanelRoute = resolveTargetPanelRoute(row);
        List<String> refreshRoutes = List.of(
            OperationalApiRoutes.secretariatQueuePanel(row.inboxKey()),
            OperationalApiRoutes.secretariatQueueAgenda(row.inboxKey())
        );
        List<SecretariatQueueActionContractDto> actions = new ArrayList<>();
        actions.add(actionContract(
            "CONFIRMAR_LOCAL_AGENDA",
            "Confirmar local/link",
            OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_VENUE_CONFIRMATION.replace("{workItemId}", String.valueOf(row.workItemId())),
            "SecretariatQueueVenueConfirmationRequest",
            appliesToVenue(row),
            appliesToVenue(row) ? null : "Ação aplicável a audiência ou sessão",
            targetPanelRoute,
            refreshRoutes
        ));
        actions.add(actionContract(
            "CONFIRMAR_INTIMACAO_PARTICIPANTES",
            "Confirmar intimação",
            OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION.replace("{workItemId}", String.valueOf(row.workItemId())),
            "SecretariatQueueParticipantNotificationRequest",
            appliesToParticipantNotification(row),
            appliesToParticipantNotification(row) ? null : "Ação aplicável a comunicação processual, audiência ou sessão",
            targetPanelRoute,
            refreshRoutes
        ));
        actions.add(actionContract(
            "REGISTRAR_COMPARECIMENTO",
            "Registrar comparecimento",
            OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_ATTENDANCE.replace("{workItemId}", String.valueOf(row.workItemId())),
            "SecretariatQueueAttendanceRequest",
            appliesToAttendance(row),
            appliesToAttendance(row) ? null : "Ação aplicável a audiência ou sessão",
            targetPanelRoute,
            refreshRoutes
        ));
        actions.add(actionContract(
            "REGISTRAR_EVENTO_REAL",
            "Registrar evento real",
            OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_COMPLETION_EVENT.replace("{workItemId}", String.valueOf(row.workItemId())),
            "SecretariatQueueCompletionEventRequest",
            appliesToCompletionEvent(row),
            appliesToCompletionEvent(row) ? null : "Ação indisponível para item concluído",
            targetPanelRoute,
            refreshRoutes
        ));
        boolean processReturnEnabled = row.autoReturnReady() || "PRONTO_PARA_RETORNO".equals(row.processReturnStatus()) || "EVENTO_REAL_REGISTRADO".equals(row.completionEventStatus());
        actions.add(actionContract(
            "EXECUTAR_RETORNO_PROCESSO",
            "Executar retorno ao processo",
            OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PROCESS_RETURN.replace("{workItemId}", String.valueOf(row.workItemId())),
            "SecretariatQueueProcessReturnRequest",
            processReturnEnabled,
            processReturnEnabled ? null : "Evento real ainda não habilitou o retorno automático",
            targetPanelRoute,
            refreshRoutes
        ));
        return List.copyOf(actions);
    }

    String resolveTargetPanelRoute(SecretariatQueuePanelRow row) {
        return firstNonBlank(
            stringValue(row.metadata().get("reentryTargetPanelRoute")),
            stringValue(row.metadata().get("targetPanelRoute")),
            stringValue(row.metadata().get("authorityInstitutionalLandingPath")),
            stringValue(row.metadata().get("authorityPanelRoute")),
            row.processReturnRoute(),
            judgePanelRoute(row),
            isBlank(row.inboxKey()) ? null : OperationalApiRoutes.secretariatQueuePanel(row.inboxKey())
        );
    }

    private String judgePanelRoute(SecretariatQueuePanelRow row) {
        if (row == null || row.processoId() == null) {
            return null;
        }
        String track = row.eventTrack() == null ? "" : row.eventTrack().trim().toUpperCase(Locale.ROOT);
        if (!List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA", "COMUNICACAO_PROCESSUAL").contains(track)) {
            return null;
        }
        return OperationalApiRoutes.JUDGE_GABINETE_DECISOES_BASE + "/processos/" + row.processoId();
    }

    List<Map<String, Object>> buildRitoCatalog() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var item : RitoProcessual.listRitosWithGroup()) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("code", item.rito());
            row.put("group", item.grupo());
            out.add(Map.copyOf(row));
        }
        return List.copyOf(out);
    }

    Map<String, Long> buildRitoCoverage(List<SecretariatQueuePanelRow> rows) {
        LinkedHashMap<String, Long> coverage = new LinkedHashMap<>();
        for (var item : RitoProcessual.listRitosWithGroup()) {
            coverage.put(item.rito(), 0L);
        }
        rows.forEach(row -> coverage.compute(normalizeGroupKey(row.ritoProcessual()), (key, value) -> value == null ? 1L : value + 1L));
        return Map.copyOf(coverage);
    }

    private List<SecretariatQueuePanelGroupDto> groupRows(String axis,
                                                          List<SecretariatQueuePanelRow> rows,
                                                          java.util.function.Function<SecretariatQueuePanelRow, String> keyExtractor,
                                                          java.util.function.Function<SecretariatQueuePanelRow, String> labelExtractor) {
        LinkedHashMap<String, List<SecretariatQueuePanelRow>> grouped = new LinkedHashMap<>();
        rows.stream()
            .sorted(PANEL_ROW_ORDER)
            .forEach(row -> grouped.computeIfAbsent(normalizeGroupKey(keyExtractor.apply(row)), ignored -> new ArrayList<>()).add(row));

        List<SecretariatQueuePanelGroupDto> out = new ArrayList<>();
        for (Map.Entry<String, List<SecretariatQueuePanelRow>> entry : grouped.entrySet()) {
            List<SecretariatQueuePanelRow> groupRows = entry.getValue();
            if (groupRows.isEmpty()) {
                continue;
            }
            LinkedHashSet<Long> processIds = new LinkedHashSet<>();
            long urgentCount = 0L;
            List<SecretariatQueuePanelItemDto> items = new ArrayList<>(groupRows.size());
            for (SecretariatQueuePanelRow row : groupRows) {
                if (row.processoId() != null) {
                    processIds.add(row.processoId());
                }
                if (row.urgent()) {
                    urgentCount++;
                }
                items.add(new SecretariatQueuePanelItemDto(
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
                    resolveTargetPanelRoute(row),
                    row.tags(),
                    buildActionContracts(row),
                    row.metadata()
                ));
            }
            SecretariatQueuePanelRow first = groupRows.get(0);
            out.add(new SecretariatQueuePanelGroupDto(
                axis,
                entry.getKey(),
                firstNonBlank(labelExtractor.apply(first), entry.getKey()),
                first.referenceAt(),
                groupRows.size(),
                processIds.size(),
                urgentCount,
                List.copyOf(items)
            ));
        }

        out.sort(Comparator
            .comparing(SecretariatQueuePanelGroupDto::referenceAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SecretariatQueuePanelGroupDto::groupLabel, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    private SecretariatQueueActionContractDto actionContract(String actionCode,
                                                             String label,
                                                             String route,
                                                             String payloadType,
                                                             boolean enabled,
                                                             String blockingReason,
                                                             String targetPanelRoute,
                                                             List<String> refreshRoutes) {
        return new SecretariatQueueActionContractDto(
            actionCode,
            label,
            "POST",
            route,
            payloadType,
            enabled,
            enabled ? null : blockingReason,
            targetPanelRoute,
            refreshRoutes
        );
    }

    private boolean appliesToVenue(SecretariatQueuePanelRow row) {
        return "AUDIENCIA_PROCESSUAL".equals(row.eventTrack()) || "SESSAO_COLEGIADA".equals(row.eventTrack());
    }

    private boolean appliesToParticipantNotification(SecretariatQueuePanelRow row) {
        return "COMUNICACAO_PROCESSUAL".equals(row.eventTrack())
            || "AUDIENCIA_PROCESSUAL".equals(row.eventTrack())
            || "SESSAO_COLEGIADA".equals(row.eventTrack());
    }

    private boolean appliesToAttendance(SecretariatQueuePanelRow row) {
        return "AUDIENCIA_PROCESSUAL".equals(row.eventTrack()) || "SESSAO_COLEGIADA".equals(row.eventTrack());
    }

    private boolean appliesToCompletionEvent(SecretariatQueuePanelRow row) {
        return !"CONCLUIDO".equalsIgnoreCase(row.status());
    }

    private static final Comparator<SecretariatQueuePanelRow> PANEL_ROW_ORDER = Comparator
        .comparing(SecretariatQueuePanelRow::referenceAt, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(SecretariatQueuePanelRow::prioridade, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(SecretariatQueuePanelRow::urgencyScore, Comparator.reverseOrder())
        .thenComparing(SecretariatQueuePanelRow::processoNumero, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(SecretariatQueuePanelRow::workItemId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static String normalizeGroupKey(String value) {
        return firstNonBlank(value, "SEM_GRUPO");
    }

    private static String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isBlank() ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
}
