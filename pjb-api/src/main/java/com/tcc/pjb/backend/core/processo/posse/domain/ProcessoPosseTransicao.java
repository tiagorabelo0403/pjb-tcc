package com.tcc.pjb.backend.core.processo.posse.domain;

import java.time.Instant;
import java.util.Objects;

public record ProcessoPosseTransicao(
        Long workItemId,
        long sequence,
        String eventCode,
        String fromState,
        String toState,
        Instant occurredAt,
        String actor,
        String lane,
        boolean blocking
) {
    public ProcessoPosseTransicao {
        Objects.requireNonNull(eventCode);
        fromState = fromState == null ? "NAO_INFORMADO" : fromState;
        toState = toState == null ? "NAO_INFORMADO" : toState;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        actor = actor == null ? "SISTEMA" : actor;
        lane = lane == null ? "NAO_INFORMADA" : lane;
    }
}
