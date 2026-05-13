package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalAffiliationValidationReportResponse(
        String validationId,
        String requestId,
        String organizationScope,
        String orgaoSigla,
        String unidadeCodigo,
        boolean aptaParaHomologacao,
        boolean documentosObrigatoriosPresentes,
        boolean representanteValidado,
        boolean dominioInstitucionalValidado,
        boolean certificadoMaterialValidado,
        boolean cadeiaConfiancaValidada,
        List<NationalCommunicationInstitutionalAffiliationValidationFindingResponse> findings,
        List<String> fundamentos,
        Instant validatedAt
) {
}
