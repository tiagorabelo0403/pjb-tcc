package com.tcc.pjb.backend.core.comunicacao.institucional.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.Locale;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record UnidadeInstitucional(
        String codigo,
        DestinatarioInstitucionalKind destinatarioKind,
        String sigla,
        String nomeOficial,
        String uf,
        String comarca,
        String foro,
        String unidade,
        String nucleo,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        PapelProcessualInstitucional papelPrincipal,
        CaixaInstitucional caixaPrincipal,
        List<CanalEntregaInstitucional> canais,
        String tribunalCodigo,
        boolean ativa,
        String observacao
) {
    public UnidadeInstitucional {
        codigo = normalizeRequired(codigo, "codigo");
        if (destinatarioKind == null) {
            throw new IllegalArgumentException("destinatarioKind é obrigatório");
        }
        sigla = normalizeRequired(sigla, "sigla");
        nomeOficial = normalizeRequired(nomeOficial, "nomeOficial");
        uf = normalizeOptionalUpper(uf);
        comarca = normalizeOptional(comarca);
        foro = normalizeOptional(foro);
        unidade = normalizeOptional(unidade);
        nucleo = normalizeOptional(nucleo);
        if (papelPrincipal == null) {
            throw new IllegalArgumentException("papelPrincipal é obrigatório");
        }
        if (caixaPrincipal == null) {
            throw new IllegalArgumentException("caixaPrincipal é obrigatória");
        }
        ArrayList<CanalEntregaInstitucional> canaisNormalizados = new ArrayList<>(PayloadMaps.copyListDistinct(canais));
        if (canaisNormalizados.isEmpty()) {
            canaisNormalizados.add(new CanalEntregaInstitucional(com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional.PJB_INBOX, true, false, 48, 120, null, null));
        }
        canais = List.copyOf(canaisNormalizados);
        tribunalCodigo = normalizeOptionalUpper(tribunalCodigo);
        observacao = normalizeOptional(observacao);
    }

    public boolean matchesUf(String candidateUf) {
        return matchesToken(uf, candidateUf, true);
    }

    public boolean matchesComarca(String candidateComarca) {
        return matchesToken(comarca, candidateComarca, false);
    }

    public boolean matchesForo(String candidateForo) {
        return matchesToken(foro, candidateForo, false);
    }

    public boolean matchesRamo(RamoDireito candidateRamo) {
        return ramoDireito == null || candidateRamo == null || ramoDireito == candidateRamo;
    }

    public boolean matchesGrau(GrauJurisdicao candidateGrau) {
        return grauJurisdicao == null || candidateGrau == null || grauJurisdicao == candidateGrau;
    }

    public CanalEntregaInstitucional canalPrincipal() {
        return canais.stream()
                .filter(CanalEntregaInstitucional::isCanalPrincipalJuridico)
                .findFirst()
                .orElseGet(() -> canais.isEmpty()
                        ? new CanalEntregaInstitucional(com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional.PJB_INBOX, true, false, 48, 120, null, null)
                        : canais.getFirst());
    }

    private static boolean matchesToken(String stored, String candidate, boolean upperOnly) {
        if (stored == null || candidate == null || candidate.isBlank()) {
            return stored == null || candidate == null || candidate.isBlank();
        }
        String normalizedCandidate = upperOnly ? candidate.trim().toUpperCase(Locale.ROOT) : normalizeKey(candidate);
        String normalizedStored = upperOnly ? stored.trim().toUpperCase(Locale.ROOT) : normalizeKey(stored);
        return normalizedStored.equals(normalizedCandidate);
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
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


    private static String normalizeKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
    }
}
