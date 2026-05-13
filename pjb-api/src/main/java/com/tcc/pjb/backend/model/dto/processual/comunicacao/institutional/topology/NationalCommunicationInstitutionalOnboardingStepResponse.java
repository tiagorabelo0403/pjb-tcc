package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.util.List;

public record NationalCommunicationInstitutionalOnboardingStepResponse(
        String stepCode,
        String title,
        String owner,
        boolean blocking,
        List<String> requiredArtifacts,
        List<String> fundamentos
) {
}
