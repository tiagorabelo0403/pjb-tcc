package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalRevocationRequest(
        Long nominatedUserId,
        String unidadeCodigo,
        Boolean revogarAfiliacao,
        List<String> fundamentos
) {
}
