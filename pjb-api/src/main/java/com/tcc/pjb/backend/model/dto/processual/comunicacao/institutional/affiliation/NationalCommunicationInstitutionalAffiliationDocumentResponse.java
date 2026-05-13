package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

public record NationalCommunicationInstitutionalAffiliationDocumentResponse(
        String codigo,
        String nome,
        String tipo,
        String referenciaExterna,
        String hashDocumento,
        boolean obrigatorio,
        boolean validado
) {
}
