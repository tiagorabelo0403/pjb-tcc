package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalTriageSuggestionDashboard(
        String expedicaoUuid,
        String unidadeCodigo,
        String caixaAtual,
        List<InstitutionalTriageSuggestion> suggestions,
        List<String> notes,
        Instant generatedAt
) {
    public InstitutionalTriageSuggestionDashboard {
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        caixaAtual = require(caixaAtual, "caixaAtual");
        suggestions = PayloadMaps.copyListWithoutNulls(suggestions);
        notes = PayloadMaps.copyTrimmedStrings(notes);
        Objects.requireNonNull(generatedAt, "generatedAt");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
