package com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain;

import java.time.Instant;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public record InstitutionalDeliveryProof(
        String proofId,
        String expedicaoUuid,
        Long processoId,
        String etapa,
        String canal,
        Long actorUserId,
        TipoUsuario actorTipoUsuario,
        String evidenciaTipo,
        String evidencia,
        Instant createdAt,
        String hashIntegridade
) {
    public InstitutionalDeliveryProof {
        proofId = require(proofId, "proofId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        etapa = require(etapa, "etapa");
        canal = require(canal, "canal");
        evidenciaTipo = require(evidenciaTipo, "evidenciaTipo");
        evidencia = require(evidencia, "evidencia");
        Objects.requireNonNull(createdAt, "createdAt");
        hashIntegridade = require(hashIntegridade, "hashIntegridade");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
