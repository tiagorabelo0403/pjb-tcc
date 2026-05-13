package com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application;

import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateStatus;

public record InstitutionalInboxActionResult(
        String expedicaoUuid,
        String unidadeCodigo,
        String caixaCodigo,
        StatusComunicacaoInstitucional status,
        InstitutionalGateStatus gateStatus,
        boolean gateBloqueado,
        List<String> justificativas,
        String hashIntegridade
) {
    public InstitutionalInboxActionResult {
        justificativas = List.copyOf(justificativas == null ? List.of() : justificativas);
    }
}
