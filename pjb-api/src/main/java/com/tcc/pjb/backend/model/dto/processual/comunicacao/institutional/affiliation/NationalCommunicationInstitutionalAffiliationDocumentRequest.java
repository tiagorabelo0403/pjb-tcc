package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

public record NationalCommunicationInstitutionalAffiliationDocumentRequest(
        String codigo,
        String nome,
        String tipo,
        String referenciaExterna,
        String hashDocumento,
        Boolean obrigatorio,
        Boolean validado
) {
}
