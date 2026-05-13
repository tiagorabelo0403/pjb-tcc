package com.tcc.pjb.backend.core.comunicacao.institucional.model;

import java.util.ArrayList;
import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;

public record AlvoInstitucional(
        Long processoId,
        String processoNumero,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        UnidadeInstitucional unidade,
        CaixaInstitucional caixa,
        CanalEntregaInstitucional canalPrincipal,
        List<CanalEntregaInstitucional> canaisElegiveis,
        String fundamentoLegal,
        String hashResolucao
) {
    public AlvoInstitucional {
        if (destinatarioKind == null) {
            throw new IllegalArgumentException("destinatarioKind é obrigatório");
        }
        if (papelProcessual == null) {
            throw new IllegalArgumentException("papelProcessual é obrigatório");
        }
        if (unidade == null) {
            throw new IllegalArgumentException("unidade é obrigatória");
        }
        if (caixa == null) {
            throw new IllegalArgumentException("caixa é obrigatória");
        }
        if (canalPrincipal == null) {
            throw new IllegalArgumentException("canalPrincipal é obrigatório");
        }
        ArrayList<CanalEntregaInstitucional> canaisNormalizados = new ArrayList<>(PayloadMaps.copyListDistinct(canaisElegiveis));
        if (canaisNormalizados.stream().noneMatch(canalPrincipal::equals)) {
            canaisNormalizados.add(0, canalPrincipal);
        }
        canaisElegiveis = List.copyOf(canaisNormalizados);
        fundamentoLegal = normalizeOptional(fundamentoLegal);
        hashResolucao = normalizeRequired(hashResolucao, "hashResolucao");
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
