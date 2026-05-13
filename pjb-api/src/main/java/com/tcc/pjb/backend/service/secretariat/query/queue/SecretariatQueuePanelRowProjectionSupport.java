package com.tcc.pjb.backend.service.secretariat.query.queue;

import java.util.Collections;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SecretariatQueuePanelRowProjectionSupport {

    SecretariatQueuePanelRow toPanelRow(SecretariatQueueItem item,
                                        Map<String, Object> metadata,
                                        List<String> tags) {
        Instant referenceAt = firstInstant(
            instantValue(metadata.get("panelReferenceAt")),
            instantValue(metadata.get("hearingAt")),
            item.getDueAt(),
            item.getUpdatedAt(),
            item.getCreatedAt()
        );
        String processoNumero = firstNonBlank(stringValue(metadata.get("processoNumero")), item.getProcessoId() == null ? null : "Processo " + item.getProcessoId());
        String ritoProcessual = firstNonBlank(stringValue(metadata.get("ritoProcessual")), stringValue(metadata.get("rito")), "SEM_RITO");
        String classeProcessual = firstNonBlank(stringValue(metadata.get("classeProcessual")), stringValue(metadata.get("classeTpuCodigo")), item.getTitulo());
        String varaLabel = firstNonBlank(
            stringValue(metadata.get("routingVaraLabel")),
            stringValue(metadata.get("vara")),
            stringValue(metadata.get("routingOrgaoLabel")),
            stringValue(metadata.get("authorityUnitLabel")),
            stringValue(metadata.get("authorityOrgao")),
            stringValue(metadata.get("unidadeJudiciariaCodigo")),
            "UNIDADE_NAO_IDENTIFICADA"
        );
        String unidadeCodigo = firstNonBlank(stringValue(metadata.get("routingUnitCode")), stringValue(metadata.get("unidadeJudiciariaCodigo")));
        String comarca = firstNonBlank(stringValue(metadata.get("routingComarcaLabel")), stringValue(metadata.get("comarca")));
        String tribunalCodigo = firstNonBlank(stringValue(metadata.get("tribunalCodigo")), stringValue(metadata.get("authorityTribunalAxis")));
        String dataLabel = firstNonBlank(stringValue(metadata.get("panelDateBucket")), dateBucket(referenceAt), "SEM_DATA");
        String stage = firstNonBlank(stringValue(metadata.get("stage")), stringValue(metadata.get("stageFamily")), item.getQueueCode(), "BASE");
        String cellCode = firstNonBlank(stringValue(metadata.get("routingCellCode")), stringValue(metadata.get("stageCellCode")), stringValue(metadata.get("cellCode")), item.getLaneCode(), item.getDeskAxis(), "CELULA_BASE");
        String secretariatLabel = firstNonBlank(stringValue(metadata.get("routingSecretariatLabel")), stringValue(metadata.get("secretariatLabel")), stringValue(metadata.get("routingSecretariatCode")), item.getInboxKey());
        String foroLabel = firstNonBlank(stringValue(metadata.get("routingComarcaLabel")), stringValue(metadata.get("foroLabel")), comarca);
        String organLabel = firstNonBlank(stringValue(metadata.get("routingOrgaoLabel")), stringValue(metadata.get("authorityOrgao")), stringValue(metadata.get("authorityUnitLabel")), varaLabel);
        String responsibleName = firstNonBlank(
            stringValue(metadata.get("assignedUserName")),
            nestedString(metadata, "assignedUser", "nome"),
            nestedString(metadata, "assignment", "primaryUserName"),
            nestedString(metadata, "responsavelOperacional", "nome"),
            "NAO_ATRIBUIDO"
        );
        Long responsibleUserId = longValue(firstNonBlank(
            stringValue(metadata.get("assignedUserId")),
            nestedString(metadata, "assignedUser", "id"),
            nestedString(metadata, "assignment", "primaryUserId"),
            nestedString(metadata, "responsavelOperacional", "usuarioId")
        ));
        String category = resolveOperationalCategory(stage, item.getQueueCode(), tags, metadata, item.isHearingSensitive());
        String eventTrack = resolveEventTrack(category, metadata);
        String confirmationStatus = resolveConfirmationStatus(eventTrack, item.getStatus(), item.getDueAt(), metadata);
        String attendanceStatus = resolveAttendanceStatus(eventTrack, metadata);
        String venueConfirmationStatus = resolveVenueConfirmationStatus(eventTrack, metadata);
        String participantNotificationStatus = resolveParticipantNotificationStatus(eventTrack, metadata);
        String completionEventStatus = resolveCompletionEventStatus(eventTrack, item.getStatus(), metadata);
        String processReturnRoute = resolveProcessReturnRoute(metadata);
        String processReturnStatus = resolveProcessReturnStatus(eventTrack, confirmationStatus, attendanceStatus, completionEventStatus, processReturnRoute, item.getStatus(), metadata);
        boolean autoReturnReady = resolveAutoReturnReady(processReturnStatus, processReturnRoute, completionEventStatus, metadata);
        int urgencyScore = intValue(metadata.get("urgencyScore"));
        String urgencyExplanation = firstNonBlank(stringValue(metadata.get("urgencyExplanation")), stringValue(metadata.get("triageRationale")));
        return new SecretariatQueuePanelRow(
            item.getInboxKey(),
            item.getWorkItemId(),
            item.getProcessoId(),
            processoNumero,
            item.getTitulo(),
            item.getQueueCode(),
            stage,
            item.getStatus(),
            item.getPrioridade(),
            referenceAt,
            item.getDueAt(),
            ritoProcessual,
            classeProcessual,
            varaLabel,
            unidadeCodigo,
            comarca,
            tribunalCodigo,
            tags,
            Collections.unmodifiableMap(metadata),
            item.getDueAt() != null && item.getDueAt().isBefore(Instant.now()),
            item.getPrioridade() != null && item.getPrioridade() <= 1,
            item.isHearingSensitive(),
            item.isSecrecyReviewRequired(),
            processoNumero,
            ritoProcessual,
            varaLabel,
            dataLabel,
            cellCode,
            secretariatLabel,
            foroLabel,
            organLabel,
            responsibleUserId,
            responsibleName,
            category,
            eventTrack,
            confirmationStatus,
            venueConfirmationStatus,
            participantNotificationStatus,
            completionEventStatus,
            attendanceStatus,
            processReturnStatus,
            processReturnRoute,
            autoReturnReady,
            urgencyScore,
            urgencyExplanation
        );
    }

    private String resolveEventTrack(String category, Map<String, Object> metadata) {
        String explicit = firstNonBlank(
            stringValue(metadata.get("eventTrack")),
            stringValue(metadata.get("operationalTrack")),
            stringValue(metadata.get("agendaTrack"))
        );
        if (explicit != null) {
            return explicit;
        }
        return switch (firstNonBlank(category, "OPERACIONAL_GERAL")) {
            case "AUDIENCIA" -> "AUDIENCIA_PROCESSUAL";
            case "SESSAO_COLEGIADA" -> "SESSAO_COLEGIADA";
            case "CITACAO_INTIMACAO" -> "COMUNICACAO_PROCESSUAL";
            case "PERICIA" -> "PERICIA_TECNICA";
            case "EXECUCAO_CUMPRIMENTO" -> "CUMPRIMENTO_EXECUCAO";
            case "SANEAMENTO" -> "SANEAMENTO_PROCESSUAL";
            default -> "OPERACIONAL_GERAL";
        };
    }

    private String resolveConfirmationStatus(String eventTrack,
                                             String status,
                                             Instant dueAt,
                                             Map<String, Object> metadata) {
        String explicit = firstNonBlank(
            stringValue(metadata.get("operationalConfirmationStatus")),
            stringValue(metadata.get("confirmationStatus")),
            stringValue(metadata.get("agendaConfirmationStatus")),
            stringValue(metadata.get("confirmacaoStatus"))
        );
        if (explicit != null) {
            return explicit;
        }
        if ("CONCLUIDO".equalsIgnoreCase(status) || "FINALIZADO".equalsIgnoreCase(status)) {
            return "CUMPRIDO";
        }
        boolean confirmable = List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA", "COMUNICACAO_PROCESSUAL").contains(firstNonBlank(eventTrack, "OPERACIONAL_GERAL"));
        if (!confirmable) {
            return "NAO_APLICAVEL";
        }
        if (Boolean.TRUE.equals(metadata.get("confirmationOverdue")) || Boolean.TRUE.equals(metadata.get("agendaConfirmationOverdue"))) {
            return "ATRASADO_SEM_CONFIRMACAO";
        }
        return "PENDENTE_CONFIRMACAO";
    }

    private String resolveAttendanceStatus(String eventTrack, Map<String, Object> metadata) {
        String explicit = firstNonBlank(
            stringValue(metadata.get("attendanceStatus")),
            stringValue(metadata.get("comparecimentoStatus")),
            stringValue(metadata.get("presencaStatus"))
        );
        if (explicit != null) {
            return explicit;
        }
        return List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA").contains(firstNonBlank(eventTrack, "OPERACIONAL_GERAL"))
            ? "AGUARDANDO_REALIZACAO"
            : "NAO_APLICAVEL";
    }

    private String resolveVenueConfirmationStatus(String eventTrack, Map<String, Object> metadata) {
        String explicit = firstNonBlank(
            nestedString(metadata, "venue", "confirmationStatus"),
            stringValue(metadata.get("venueConfirmationStatus")),
            stringValue(metadata.get("localConfirmacaoStatus"))
        );
        if (explicit != null) {
            return explicit;
        }
        return List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA").contains(firstNonBlank(eventTrack, "OPERACIONAL_GERAL"))
            ? "PENDENTE_LOCAL"
            : "NAO_APLICAVEL";
    }

    private String resolveParticipantNotificationStatus(String eventTrack, Map<String, Object> metadata) {
        String explicit = firstNonBlank(
            nestedString(metadata, "participantNotification", "status"),
            stringValue(metadata.get("participantNotificationStatus")),
            stringValue(metadata.get("participantIntimationStatus"))
        );
        if (explicit != null) {
            return explicit;
        }
        if (!List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA", "COMUNICACAO_PROCESSUAL").contains(firstNonBlank(eventTrack, "OPERACIONAL_GERAL"))) {
            return "NAO_APLICAVEL";
        }
        long missingCount = longFromObject(firstNonBlank(
            nestedString(metadata, "participantNotification", "missingCount"),
            stringValue(metadata.get("contactMissingCount"))
        ));
        long readyCount = longFromObject(firstNonBlank(
            nestedString(metadata, "participantNotification", "readyCount"),
            stringValue(metadata.get("contactReadyCount"))
        ));
        if (missingCount > 0) {
            return "CONTATOS_INCOMPLETOS";
        }
        if (readyCount > 0) {
            return "PENDENTE_INTIMACAO";
        }
        return "AGUARDANDO_DADOS_PARTICIPANTES";
    }

    private String resolveCompletionEventStatus(String eventTrack, String status, Map<String, Object> metadata) {
        String explicit = firstNonBlank(
            stringValue(metadata.get("completionEventStatus")),
            stringValue(metadata.get("eventoRealStatus")),
            stringValue(metadata.get("completionStatus"))
        );
        if (explicit != null) {
            return explicit;
        }
        boolean eventBased = List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA", "COMUNICACAO_PROCESSUAL").contains(firstNonBlank(eventTrack, "OPERACIONAL_GERAL"));
        if (!eventBased) {
            return "NAO_APLICAVEL";
        }
        if (firstInstant(
            instantValue(metadata.get("completionEventOccurredAt")),
            instantValue(metadata.get("completionOccurredAt")),
            instantValue(metadata.get("completionCertifiedAt"))
        ) != null) {
            return "EVENTO_REAL_REGISTRADO";
        }
        if ("CONCLUIDO".equalsIgnoreCase(status) || "FINALIZADO".equalsIgnoreCase(status)) {
            return "EVENTO_REAL_REGISTRADO";
        }
        return "PENDENTE_EVENTO_REAL";
    }

    private String resolveProcessReturnRoute(Map<String, Object> metadata) {
        return firstNonBlank(
            stringValue(metadata.get("processReturnRoute")),
            stringValue(metadata.get("authorityReturnRoute")),
            stringValue(metadata.get("originReturnRoute"))
        );
    }

    private String resolveProcessReturnStatus(String eventTrack,
                                              String confirmationStatus,
                                              String attendanceStatus,
                                              String completionEventStatus,
                                              String processReturnRoute,
                                              String status,
                                              Map<String, Object> metadata) {
        String explicit = firstNonBlank(
            stringValue(metadata.get("processReturnStatus")),
            stringValue(metadata.get("retornoProcessoStatus")),
            stringValue(metadata.get("returnStatus"))
        );
        if (explicit != null) {
            return explicit;
        }
        boolean eventBasedReturn = List.of("AUDIENCIA_PROCESSUAL", "SESSAO_COLEGIADA", "COMUNICACAO_PROCESSUAL").contains(firstNonBlank(eventTrack, "OPERACIONAL_GERAL"));
        if (processReturnRoute == null || processReturnRoute.isBlank()) {
            return eventBasedReturn ? "AGUARDANDO_EVENTO" : "NAO_APLICAVEL";
        }
        if (Boolean.TRUE.equals(metadata.get("autoReturnReady")) || "PRONTO_PARA_RETORNO".equalsIgnoreCase(stringValue(metadata.get("returnReadiness")))) {
            return "PRONTO_PARA_RETORNO";
        }
        if ("EVENTO_REAL_REGISTRADO".equalsIgnoreCase(completionEventStatus)) {
            return "PRONTO_PARA_RETORNO";
        }
        if ("CONCLUIDO".equalsIgnoreCase(status) || "CUMPRIDO".equalsIgnoreCase(confirmationStatus)) {
            return "PRONTO_PARA_RETORNO";
        }
        if (List.of("PRESENTE", "AUSENTE", "JUSTIFICADO", "REALIZADO").contains(firstNonBlank(attendanceStatus, "NAO_APLICAVEL"))) {
            return "PRONTO_PARA_RETORNO";
        }
        return eventBasedReturn ? "AGUARDANDO_EVENTO" : "RETORNO_MONITORADO";
    }

    private boolean resolveAutoReturnReady(String processReturnStatus,
                                           String processReturnRoute,
                                           String completionEventStatus,
                                           Map<String, Object> metadata) {
        if (metadata != null) {
            Object raw = metadata.get("autoReturnReady");
            if (raw instanceof Boolean bool) {
                return bool;
            }
            if (raw != null && "true".equalsIgnoreCase(String.valueOf(raw))) {
                return true;
            }
        }
        return processReturnRoute != null && !processReturnRoute.isBlank()
            && ("PRONTO_PARA_RETORNO".equalsIgnoreCase(processReturnStatus)
            || "EVENTO_REAL_REGISTRADO".equalsIgnoreCase(completionEventStatus));
    }

    private String resolveOperationalCategory(String stage,
                                              String queueCode,
                                              List<String> tags,
                                              Map<String, Object> metadata,
                                              boolean hearingSensitive) {
        String normalizedStage = normalize(firstNonBlank(stage, stringValue(metadata.get("stageFamily")), queueCode));
        String normalizedQueue = normalize(queueCode);
        String normalizedTags = normalize(String.join(" ", tags == null ? List.of() : tags));
        if (hearingSensitive || normalizedStage.contains("AUDI") || normalizedQueue.contains("AUDI") || normalizedTags.contains("AUDI")) {
            return "AUDIENCIA";
        }
        if (normalizedStage.contains("CIT") || normalizedStage.contains("INTIM") || normalizedStage.contains("VISTA") || normalizedQueue.contains("CIT") || normalizedQueue.contains("INTIM")) {
            return "CITACAO_INTIMACAO";
        }
        if (normalizedStage.contains("COLEG") || normalizedStage.contains("PAUTA") || normalizedStage.contains("ACOR") || normalizedStage.contains("EMBARG") || normalizedQueue.contains("SESS") || normalizedQueue.contains("ACOR") || normalizedQueue.contains("PAUTA")) {
            return "SESSAO_COLEGIADA";
        }
        if (normalizedStage.contains("PERIC") || normalizedQueue.contains("PERIC")) {
            return "PERICIA";
        }
        if (normalizedStage.contains("EXEC") || normalizedQueue.contains("EXEC") || normalizedQueue.contains("CUMPRIMENTO")) {
            return "EXECUCAO_CUMPRIMENTO";
        }
        if (normalizedStage.contains("SANE") || normalizedQueue.contains("SANE")) {
            return "SANEAMENTO";
        }
        return "OPERACIONAL_GERAL";
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

    private static long longFromObject(String raw) {
        Long value = longValue(raw);
        return value == null ? 0L : value;
    }

    private static int intValue(Object raw) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw != null) {
            try {
                return Integer.parseInt(String.valueOf(raw).trim());
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
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

    private static String dateBucket(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
    }

    private static String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isBlank() ? null : value;
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
}
