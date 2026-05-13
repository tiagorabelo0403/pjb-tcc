package com.tcc.pjb.backend.core.quality.codebase.application;

import java.util.List;
import java.util.Objects;

public record PjbCodebaseCriticalFlowDefinition(
        String nome,
        List<String> tokens
) {
    public PjbCodebaseCriticalFlowDefinition {
        nome = Objects.toString(nome, "").trim();
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
    }
}
