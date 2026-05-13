package com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalIntegrationContractDescriptor(
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
    public InstitutionalIntegrationContractDescriptor {
        provider = require(provider, "provider");
        canal = require(canal, "canal");
        version = require(version, "version");
        signatureAlgorithm = require(signatureAlgorithm, "signatureAlgorithm");
        requiredFields = PayloadMaps.copyTrimmedStrings(requiredFields);
        optionalFields = PayloadMaps.copyTrimmedStrings(optionalFields);
        transportGuarantees = PayloadMaps.copyTrimmedStrings(transportGuarantees);
        idempotencyKeyField = require(idempotencyKeyField, "idempotencyKeyField");
        correlationField = require(correlationField, "correlationField");
        notes = notes == null ? "" : notes.trim();
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
