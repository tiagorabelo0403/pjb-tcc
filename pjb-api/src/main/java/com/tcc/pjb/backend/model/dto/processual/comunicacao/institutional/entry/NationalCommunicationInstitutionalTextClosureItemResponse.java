package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import java.util.List;

public record NationalCommunicationInstitutionalTextClosureItemResponse(
        String code,
        String eixo,
        boolean implemented,
        List<String> evidences,
        List<String> fundamentos
) {
}
