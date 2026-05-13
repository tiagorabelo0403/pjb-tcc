package com.tcc.pjb.backend.core.processo.vertical.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoVerticalLane(
        String profileCode,
        String displayName,
        String panel,
        String accentColor,
        String trustFloor,
        List<String> authorityBands,
        List<String> actionHints,
        List<String> separators,
        List<String> guards
) {
    public ProcessoVerticalLane {
        Objects.requireNonNull(profileCode);
        Objects.requireNonNull(displayName);
        panel = panel == null ? "NAO_INFORMADO" : panel;
        accentColor = accentColor == null ? "slate" : accentColor;
        trustFloor = trustFloor == null ? "NAO_INFORMADO" : trustFloor;
        authorityBands = authorityBands == null ? List.of() : List.copyOf(authorityBands);
        actionHints = actionHints == null ? List.of() : List.copyOf(actionHints);
        separators = separators == null ? List.of() : List.copyOf(separators);
        guards = guards == null ? List.of() : List.copyOf(guards);
    }
}
