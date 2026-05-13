package com.tcc.pjb.backend.core.processo.posse.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoPosseItem(
        Long workItemId,
        String title,
        String currentOwnership,
        String immutableTrailHash,
        boolean claimable,
        List<ProcessoPosseTransicao> transitions,
        List<String> guards
) {
    public ProcessoPosseItem {
        Objects.requireNonNull(title);
        currentOwnership = currentOwnership == null ? "SEM_POSSE" : currentOwnership;
        immutableTrailHash = immutableTrailHash == null ? "" : immutableTrailHash;
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
        guards = guards == null ? List.of() : List.copyOf(guards);
    }
}
