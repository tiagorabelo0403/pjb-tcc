package com.tcc.pjb.backend.model.dto.profile;

public record ChainOfCustodySyncExportRequest(
        String parceiroInstitucional,
        String noOrigem,
        String nonce,
        String justificativa
) {
}
