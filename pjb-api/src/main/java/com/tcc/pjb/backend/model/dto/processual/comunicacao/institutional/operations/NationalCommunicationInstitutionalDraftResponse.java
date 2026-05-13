package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;

public record NationalCommunicationInstitutionalDraftResponse(
        String draftId,
        String expedicaoUuid,
        Long processoId,
        String unidadeCodigo,
        String caixaCodigo,
        Long autorUsuarioId,
        Long aprovadorUsuarioId,
        String status,
        String titulo,
        String conteudo,
        String observacoes,
        Instant createdAt,
        Instant submittedAt,
        Instant reviewedAt,
        Instant updatedAt,
        String hashIntegridade
) {
}
