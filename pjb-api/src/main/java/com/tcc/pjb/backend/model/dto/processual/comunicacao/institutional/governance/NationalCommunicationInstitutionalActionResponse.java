package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalActionResponse(
        String expedicaoUuid,
        String unidadeCodigo,
        String caixaCodigo,
        String status,
        String gateStatus,
        boolean gateBloqueado,
        List<String> justificativas,
        String hashIntegridade
) {
    public NationalCommunicationInstitutionalActionResponse(String expedicaoUuid,
                                                            String status,
                                                            String unidadeCodigo,
                                                            String caixaCodigo,
                                                            java.time.Instant updatedAt) {
        this(expedicaoUuid, unidadeCodigo, caixaCodigo, status, null, false, java.util.List.of(), null);
    }
}
