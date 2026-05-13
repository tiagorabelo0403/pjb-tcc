package com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public record InstitutionalTimelineEvent(
        String eventId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        InstitutionalTimelineEventType eventType,
        StatusComunicacaoInstitucional statusComunicacao,
        String unidadeCodigo,
        String caixaCodigo,
        Long actorUserId,
        TipoUsuario actorTipoUsuario,
        String resumo,
        Map<String, Object> detalhes,
        Instant occurredAt,
        String hashIntegridade
) {
    public InstitutionalTimelineEvent {
        eventId = require(eventId, "eventId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(statusComunicacao, "statusComunicacao");
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        caixaCodigo = require(caixaCodigo, "caixaCodigo");
        resumo = require(resumo, "resumo");
        detalhes = immutable(detalhes);
        Objects.requireNonNull(occurredAt, "occurredAt");
        hashIntegridade = require(hashIntegridade, "hashIntegridade");
    }

    private static Map<String, Object> immutable(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        value.forEach((key, v) -> {
            if (key != null && !key.isBlank() && v != null) {
                copy.put(key, v);
            }
        });
        return copy.isEmpty() ? Map.of() : Map.copyOf(copy);
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
