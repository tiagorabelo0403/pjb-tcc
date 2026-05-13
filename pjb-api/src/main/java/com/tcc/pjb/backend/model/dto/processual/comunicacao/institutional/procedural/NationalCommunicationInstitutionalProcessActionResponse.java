package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProcessActionResponse(
        String code,
        String title,
        String description,
        String accentColor,
        boolean requiresCertificate,
        boolean requiresTitularApproval,
        boolean modifiesFlow,
        List<String> fasesPreferenciais,
        List<String> ritosPreferenciais,
        List<String> fundamentos
) {
}
