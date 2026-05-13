package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProceduralContextVectorResponse(
        String profileCode,
        String displayName,
        String panel,
        String processProfile,
        String trustFloor,
        String ritoProcessual,
        String faseProcessual,
        String statusProcessual,
        String ramoDireito,
        boolean recursal,
        boolean embargos,
        boolean execucao,
        boolean urgente,
        boolean custodial,
        boolean technical,
        boolean governance,
        List<String> fundamentos
) {
}
