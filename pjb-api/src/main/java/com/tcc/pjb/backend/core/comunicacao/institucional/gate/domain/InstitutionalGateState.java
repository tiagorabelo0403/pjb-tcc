package com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalGateState(
        String gateStateId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String gateCode,
        InstitutionalGateStatus status,
        boolean bloqueado,
        String motivo,
        String ultimaProvaTipo,
        Instant createdAt,
        Instant updatedAt,
        Instant releasedAt,
        List<String> justificativas,
        String hashIntegridade
) {
    public InstitutionalGateState {
        gateStateId = require(gateStateId, "gateStateId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        gateCode = require(gateCode, "gateCode");
        Objects.requireNonNull(status, "status");
        motivo = require(motivo, "motivo");
        ultimaProvaTipo = normalizeOptional(ultimaProvaTipo);
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        justificativas = List.copyOf(justificativas == null ? List.of() : justificativas);
        hashIntegridade = require(hashIntegridade, "hashIntegridade");
    }

    public InstitutionalGateState withStatus(InstitutionalGateStatus novoStatus,
                                             String novaProva,
                                             Instant at,
                                             List<String> novasJustificativas,
                                             String novoHash) {
        return new InstitutionalGateState(
                gateStateId,
                expedicaoUuid,
                processoId,
                processoNumero,
                gateCode,
                Objects.requireNonNull(novoStatus, "novoStatus"),
                novoStatus.isBloqueado(),
                motivo,
                normalizeOptional(novaProva),
                createdAt,
                at,
                novoStatus == InstitutionalGateStatus.LIBERADO ? at : releasedAt,
                novasJustificativas,
                novoHash
        );
    }

    private static String require(String value, String field) {
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


    public boolean bloqueando() {
        return bloqueado;
    }
}
