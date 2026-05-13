package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusDelegacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoFluxoDelegacaoInstitucional;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record InstitutionalDelegationAssignment(
        String assignmentId,
        String expedicaoUuid,
        Long processoId,
        String unidadeCodigo,
        String caixaCodigo,
        Long deleganteUsuarioId,
        Long delegadoUsuarioId,
        TipoFluxoDelegacaoInstitucional tipoFluxo,
        Set<CapacidadeCaixaInstitucional> capacidades,
        StatusDelegacaoInstitucional status,
        String motivo,
        Instant inicioVigencia,
        Instant fimVigencia,
        Instant createdAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalDelegationAssignment {
        assignmentId = require(assignmentId, "assignmentId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        caixaCodigo = require(caixaCodigo, "caixaCodigo");
        Objects.requireNonNull(deleganteUsuarioId, "deleganteUsuarioId");
        Objects.requireNonNull(delegadoUsuarioId, "delegadoUsuarioId");
        Objects.requireNonNull(tipoFluxo, "tipoFluxo");
        capacidades = capacidades == null || capacidades.isEmpty() ? EnumSet.noneOf(CapacidadeCaixaInstitucional.class) : EnumSet.copyOf(capacidades);
        Objects.requireNonNull(status, "status");
        motivo = normalize(motivo);
        Objects.requireNonNull(inicioVigencia, "inicioVigencia");
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

    public InstitutionalDelegationAssignment withRevogacao(Instant at, String hash) {
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalDelegationAssignment(
                assignmentId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, deleganteUsuarioId, delegadoUsuarioId,
                tipoFluxo, capacidades, StatusDelegacaoInstitucional.REVOGADA, motivo, inicioVigencia, fimVigencia, createdAt, now, hash
        );
    }

    public InstitutionalDelegationAssignment withExpiracao(Instant at, String hash) {
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalDelegationAssignment(
                assignmentId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, deleganteUsuarioId, delegadoUsuarioId,
                tipoFluxo, capacidades, StatusDelegacaoInstitucional.EXPIRADA, motivo, inicioVigencia, fimVigencia, createdAt, now, hash
        );
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
