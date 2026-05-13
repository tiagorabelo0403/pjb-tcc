package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusCoberturaOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCoberturaOperacionalInstitucional;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record InstitutionalOperationalCoverageRule(
        String ruleId,
        String unidadeCodigo,
        String caixaCodigo,
        Long titularUsuarioId,
        Long coberturaUsuarioId,
        TipoCoberturaOperacionalInstitucional tipoCobertura,
        Set<CapacidadeCaixaInstitucional> capacidades,
        StatusCoberturaOperacionalInstitucional status,
        Instant inicioVigencia,
        Instant fimVigencia,
        String motivo,
        String observacoes,
        Instant createdAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalOperationalCoverageRule {
        ruleId = require(ruleId, "ruleId");
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        caixaCodigo = require(caixaCodigo, "caixaCodigo");
        Objects.requireNonNull(titularUsuarioId, "titularUsuarioId");
        Objects.requireNonNull(coberturaUsuarioId, "coberturaUsuarioId");
        Objects.requireNonNull(tipoCobertura, "tipoCobertura");
        capacidades = capacidades == null || capacidades.isEmpty() ? EnumSet.noneOf(CapacidadeCaixaInstitucional.class) : EnumSet.copyOf(capacidades);
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(inicioVigencia, "inicioVigencia");
        motivo = normalize(motivo);
        observacoes = normalize(observacoes);
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        hashIntegridade = require(hashIntegridade, "hashIntegridade");
    }

    public boolean ativaEm(Instant instant) {
        Instant base = instant == null ? Instant.now() : instant;
        if (!status.isAtiva()) {
            return false;
        }
        if (base.isBefore(inicioVigencia)) {
            return false;
        }
        return fimVigencia == null || !base.isAfter(fimVigencia);
    }

    public InstitutionalOperationalCoverageRule withStatus(StatusCoberturaOperacionalInstitucional status, Instant at, String hash) {
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalOperationalCoverageRule(ruleId, unidadeCodigo, caixaCodigo, titularUsuarioId, coberturaUsuarioId, tipoCobertura,
                capacidades, status, inicioVigencia, fimVigencia, motivo, observacoes, createdAt, now, hash);
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
