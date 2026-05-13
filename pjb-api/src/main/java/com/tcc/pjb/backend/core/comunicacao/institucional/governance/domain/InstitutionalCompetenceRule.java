package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record InstitutionalCompetenceRule(
        String ruleId,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        String uf,
        String comarca,
        String foro,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        String unidadeCodigo,
        int prioridade,
        Instant vigenciaInicio,
        Instant vigenciaFim,
        boolean ativa,
        String origem,
        String fundamentoAdministrativo,
        Instant createdAt,
        Instant updatedAt
) {

    public InstitutionalCompetenceRule {
        ruleId = require(ruleId, "ruleId");
        Objects.requireNonNull(destinatarioKind, "destinatarioKind");
        Objects.requireNonNull(papelProcessual, "papelProcessual");
        uf = normalizeUpperOptional(uf);
        comarca = normalizeOptional(comarca);
        foro = normalizeOptional(foro);
        unidadeCodigo = normalizeUpper(unidadeCodigo);
        Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        origem = require(origem, "origem");
        fundamentoAdministrativo = normalizeOptional(fundamentoAdministrativo);
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (vigenciaFim != null && !vigenciaFim.isAfter(vigenciaInicio)) {
            throw new IllegalArgumentException("vigenciaFim deve ser posterior a vigenciaInicio");
        }
    }

    public boolean isEffectiveAt(Instant reference) {
        Instant safeReference = reference == null ? Instant.now() : reference;
        return ativa && !safeReference.isBefore(vigenciaInicio) && (vigenciaFim == null || safeReference.isBefore(vigenciaFim));
    }

    public boolean matches(DestinatarioInstitucionalKind candidateKind,
                           PapelProcessualInstitucional candidatePapel,
                           String candidateUf,
                           String candidateComarca,
                           String candidateForo,
                           RamoDireito candidateRamo,
                           GrauJurisdicao candidateGrau,
                           Instant reference) {
        if (candidateKind != destinatarioKind || candidatePapel != papelProcessual || !isEffectiveAt(reference)) {
            return false;
        }
        if (uf != null && !uf.equalsIgnoreCase(candidateUf)) {
            return false;
        }
        if (comarca != null && !key(comarca).equals(key(candidateComarca))) {
            return false;
        }
        if (foro != null && !key(foro).equals(key(candidateForo))) {
            return false;
        }
        if (ramoDireito != null && candidateRamo != null && ramoDireito != candidateRamo) {
            return false;
        }
        if (grauJurisdicao != null && candidateGrau != null && grauJurisdicao != candidateGrau) {
            return false;
        }
        return true;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String normalizeUpper(String value) {
        return require(value, "value").toUpperCase(Locale.ROOT);
    }

    private static String normalizeUpperOptional(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String key(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT).replace(" ", "");
    }
}
