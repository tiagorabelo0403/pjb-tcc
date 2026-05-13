package com.tcc.pjb.backend.core.dje.domain;

public record DjeEdicaoResult(
        boolean available,
        String summary,
        Long total
) {
}
