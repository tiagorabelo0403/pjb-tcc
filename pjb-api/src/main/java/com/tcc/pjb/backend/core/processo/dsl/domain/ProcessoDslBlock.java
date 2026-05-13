package com.tcc.pjb.backend.core.processo.dsl.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoDslBlock(
        String code,
        String title,
        String version,
        List<ProcessoDslRule> rules,
        List<String> sources
) {
    public ProcessoDslBlock {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        version = version == null ? "2026.1" : version;
        rules = rules == null ? List.of() : List.copyOf(rules);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
