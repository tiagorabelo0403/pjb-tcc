package com.tcc.pjb.backend.core.comunicacao.institucional.model;

import java.util.Locale;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;

public record CaixaInstitucional(
        String codigo,
        String nomeExibicao,
        TipoCaixaInstitucional tipo,
        String unidadeCodigo,
        DestinatarioInstitucionalKind destinatarioKind,
        boolean recebimentoEmLote,
        boolean permiteTriagem
) {
    public CaixaInstitucional {
        codigo = normalizeRequired(codigo, "codigo");
        nomeExibicao = normalizeRequired(nomeExibicao, "nomeExibicao");
        if (tipo == null) {
            throw new IllegalArgumentException("tipo é obrigatório");
        }
        if (destinatarioKind == null) {
            throw new IllegalArgumentException("destinatarioKind é obrigatório");
        }
        unidadeCodigo = normalizeRequired(unidadeCodigo, "unidadeCodigo");
    }

    public TipoCaixaInstitucional tipoCaixa() {
        return tipo();
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
