package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaGovernancaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record InstitutionalCatalogGovernanceEntry(
        String governanceId,
        String unidadeCodigo,
        DestinatarioInstitucionalKind destinatarioKind,
        String uf,
        String comarca,
        String foro,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        AbrangenciaGovernancaInstitucional abrangencia,
        Instant vigenciaInicio,
        Instant vigenciaFim,
        boolean ativa,
        boolean suspendeEntregaExterna,
        boolean exigeHomologacaoAdministrativa,
        Set<CanalComunicacaoInstitucional> canaisPreferenciais,
        String unidadeSubstitutaCodigo,
        String fundamentoAdministrativo,
        String origem,
        Instant createdAt,
        Instant updatedAt
) {

    public InstitutionalCatalogGovernanceEntry {
        governanceId = require(governanceId, "governanceId");
        unidadeCodigo = normalizeUpper(unidadeCodigo);
        Objects.requireNonNull(destinatarioKind, "destinatarioKind");
        uf = normalizeUpperOptional(uf);
        comarca = normalizeOptional(comarca);
        foro = normalizeOptional(foro);
        Objects.requireNonNull(abrangencia, "abrangencia");
        Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        canaisPreferenciais = channels(canaisPreferenciais);
        unidadeSubstitutaCodigo = normalizeUpperOptional(unidadeSubstitutaCodigo);
        fundamentoAdministrativo = normalizeOptional(fundamentoAdministrativo);
        origem = require(origem, "origem");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (vigenciaFim != null && !vigenciaFim.isAfter(vigenciaInicio)) {
            throw new IllegalArgumentException("vigenciaFim deve ser posterior a vigenciaInicio");
        }
    }

    public boolean isEffectiveAt(Instant reference) {
        return ativa && isWithinVigency(reference);
    }

    public boolean isWithinVigency(Instant reference) {
        Instant safeReference = reference == null ? Instant.now() : reference;
        return !safeReference.isBefore(vigenciaInicio)
                && (vigenciaFim == null || safeReference.isBefore(vigenciaFim));
    }

    public boolean matches(String candidateUnitCode,
                           DestinatarioInstitucionalKind candidateKind,
                           String candidateUf,
                           String candidateComarca,
                           String candidateForo,
                           RamoDireito candidateRamo,
                           GrauJurisdicao candidateGrau,
                           Instant reference) {
        if (candidateKind != destinatarioKind || !isWithinVigency(reference)) {
            return false;
        }
        if (unidadeCodigo != null && candidateUnitCode != null && !unidadeCodigo.equalsIgnoreCase(candidateUnitCode)) {
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

    private static Set<CanalComunicacaoInstitucional> channels(Set<CanalComunicacaoInstitucional> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        EnumSet<CanalComunicacaoInstitucional> normalized = EnumSet.noneOf(CanalComunicacaoInstitucional.class);
        for (CanalComunicacaoInstitucional value : values) {
            if (value != null) {
                normalized.add(value);
            }
        }
        return normalized.isEmpty() ? Set.of() : Set.copyOf(normalized);
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
