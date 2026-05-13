package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalIntegrationContractDescriptorResponse(
        String provider,
        String canal,
        String version,
        String signatureAlgorithm,
        List<String> requiredFields,
        List<String> optionalFields,
        List<String> transportGuarantees,
        String idempotencyKeyField,
        String correlationField,
        String notes
) {
}
