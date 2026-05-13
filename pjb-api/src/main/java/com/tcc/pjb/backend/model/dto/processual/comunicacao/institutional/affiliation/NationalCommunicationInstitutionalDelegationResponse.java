package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalDelegationResponse(
        String assignmentId,
        String expedicaoUuid,
        Long processoId,
        String unidadeCodigo,
        String caixaCodigo,
        Long deleganteUsuarioId,
        Long delegadoUsuarioId,
        String tipoFluxo,
        List<String> capacidades,
        String status,
        String motivo,
        Instant inicioVigencia,
        Instant fimVigencia,
        Instant updatedAt,
        String hashIntegridade
) {
    public NationalCommunicationInstitutionalDelegationResponse(
            String assignmentId,
            String expedicaoUuid,
            Long processoId,
            String unidadeCodigo,
            String caixaCodigo,
            Long deleganteUsuarioId,
            Long delegadoUsuarioId,
            String tipoFluxo,
            java.util.List<String> capacidades,
            String status,
            String motivo,
            java.time.Instant inicioVigencia,
            java.time.Instant fimVigencia,
            java.time.Instant updatedAt) {
        this(assignmentId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, deleganteUsuarioId, delegadoUsuarioId, tipoFluxo, capacidades, status, motivo, inicioVigencia, fimVigencia, updatedAt, null);
    }
}
