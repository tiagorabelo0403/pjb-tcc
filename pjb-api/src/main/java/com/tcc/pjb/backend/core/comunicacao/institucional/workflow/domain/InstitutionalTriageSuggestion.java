package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalTriageSuggestion(
        String suggestionId,
        String expedicaoUuid,
        String unidadeCodigo,
        String caixaCodigoOrigem,
        String caixaCodigoSugerida,
        Long usuarioIdSugerido,
        String tipoSugestao,
        int score,
        List<String> fundamentos
) {
    public InstitutionalTriageSuggestion {
        suggestionId = require(suggestionId, "suggestionId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        caixaCodigoOrigem = require(caixaCodigoOrigem, "caixaCodigoOrigem");
        caixaCodigoSugerida = require(caixaCodigoSugerida, "caixaCodigoSugerida");
        tipoSugestao = require(tipoSugestao, "tipoSugestao");
        fundamentos = PayloadMaps.copyTrimmedStrings(fundamentos);
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
