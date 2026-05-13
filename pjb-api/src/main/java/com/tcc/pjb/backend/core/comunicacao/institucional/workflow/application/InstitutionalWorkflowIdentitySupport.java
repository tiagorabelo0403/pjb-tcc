package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.model.entity.enums.TipoFluxoDelegacaoInstitucional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

final class InstitutionalWorkflowIdentitySupport {

    private InstitutionalWorkflowIdentitySupport() {
    }

    static String assignmentId(String expedicaoUuid,
                               TipoFluxoDelegacaoInstitucional tipoFluxo,
                               Long targetUserId,
                               Instant createdAt) {
        Objects.requireNonNull(tipoFluxo, "tipoFluxo");
        Objects.requireNonNull(targetUserId, "targetUserId");
        Instant instant = createdAt == null ? Instant.now() : createdAt;
        return UUID.nameUUIDFromBytes((normalize(expedicaoUuid) + "|" + tipoFluxo.name() + "|" + targetUserId + "|" + instant.toEpochMilli())
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    static String nextDraftId(String expedicaoUuid, Collection<InstitutionalDraftManifestation> history) {
        int nextVersion = history == null ? 1 : history.size() + 1;
        return UUID.nameUUIDFromBytes((normalize(expedicaoUuid) + "|DRAFT|" + nextVersion).getBytes(StandardCharsets.UTF_8)).toString();
    }

    static String outboxDedupKey(String prefix, String eventType, String expedicaoUuid, Map<String, Object> payload) {
        TreeMap<String, String> ordered = new TreeMap<>();
        if (payload != null) {
            payload.forEach((key, value) -> ordered.put(String.valueOf(key), String.valueOf(value)));
        }
        StringBuilder builder = new StringBuilder();
        builder.append(normalize(prefix)).append(':')
                .append(normalize(eventType)).append(':')
                .append(normalize(expedicaoUuid));
        ordered.forEach((key, value) -> builder.append(':').append(key).append('=').append(value));
        return builder.toString();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value_obrigatorio");
        }
        return value.trim();
    }
}
