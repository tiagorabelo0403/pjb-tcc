package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.util.List;

public record NationalCommunicationInstitutionalAccessCheckResponse(
        boolean autorizado,
        String unidadeCodigo,
        String caixaCodigo,
        String capacidadeSolicitada,
        List<String> justificativas,
        List<NationalCommunicationInstitutionalMembershipResponse> vinculosElegiveis) {
}
