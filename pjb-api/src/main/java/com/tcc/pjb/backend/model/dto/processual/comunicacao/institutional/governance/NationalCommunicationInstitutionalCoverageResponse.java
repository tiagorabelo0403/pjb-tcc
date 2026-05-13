package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalCoverageResponse(
        String ruleId,
        String unidadeCodigo,
        String caixaCodigo,
        Long titularUsuarioId,
        Long coberturaUsuarioId,
        String tipoCobertura,
        List<String> capacidades,
        String status,
        Instant inicioVigencia,
        Instant fimVigencia,
        String motivo,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {
}
