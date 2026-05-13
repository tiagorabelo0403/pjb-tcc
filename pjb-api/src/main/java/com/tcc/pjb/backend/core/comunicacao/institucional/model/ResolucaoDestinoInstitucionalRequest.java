package com.tcc.pjb.backend.core.comunicacao.institucional.model;

import java.text.Normalizer;
import java.util.Locale;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record ResolucaoDestinoInstitucionalRequest(
        Long processoId,
        String processoNumero,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        String uf,
        String comarca,
        String foro,
        String unidadeSugerida,
        String nucleoSugerido,
        String fundamentoLegal,
        boolean exigeCienciaPessoal
) {
    public ResolucaoDestinoInstitucionalRequest {
        if (destinatarioKind == null) {
            throw new IllegalArgumentException("destinatarioKind é obrigatório");
        }
        if (papelProcessual == null) {
            throw new IllegalArgumentException("papelProcessual é obrigatório");
        }
        processoNumero = normalizeOptional(processoNumero);
        uf = normalizeOptionalUpper(uf);
        comarca = normalizeOptionalKey(comarca);
        foro = normalizeOptionalKey(foro);
        unidadeSugerida = normalizeOptional(unidadeSugerida);
        nucleoSugerido = normalizeOptional(nucleoSugerido);
        fundamentoLegal = normalizeOptional(fundamentoLegal);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeOptionalUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptionalKey(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        return Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
    }
}
