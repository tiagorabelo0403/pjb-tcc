package com.tcc.pjb.backend.core.security.edge;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EdgeAttestationValidator {

    private final ObjectMapper mapper;

    public EdgeAttestationValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public EdgeCryptoAttestation parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readValue(json, EdgeCryptoAttestation.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("edge attestation inválida", e);
        }
    }

    public void validateOrThrow(EdgeCryptoAttestation a, String expectedSha384, long expectedSize) {
        if (a == null) return;

        if (a.schema() == null || a.schema().isBlank()) {
            throw new IllegalArgumentException("edge attestation sem schema");
        }
        if (a.algoritmoHash() == null || !a.algoritmoHash().equalsIgnoreCase("SHA-384")) {
            throw new IllegalArgumentException("algoritmoHash não suportado");
        }
        if (a.hashSha384() == null || a.hashSha384().length() != 96) {
            throw new IllegalArgumentException("hashSha384 inválido");
        }
        if (expectedSha384 != null && !a.hashSha384().equalsIgnoreCase(expectedSha384)) {
            throw new IllegalArgumentException("hash divergente entre reserva e attestation");
        }
        if (expectedSize > 0 && a.tamanhoBytes() != expectedSize) {
            throw new IllegalArgumentException("tamanho divergente entre reserva e attestation");
        }
        if (a.signatureBase64() == null || a.signatureBase64().isBlank()) {
            throw new IllegalArgumentException("attestation sem assinatura");
        }
        if (a.signerSubject() == null || a.signerSubject().isBlank()) {
            throw new IllegalArgumentException("attestation sem signerSubject");
        }
        if (a.certChainPem() == null || a.certChainPem().isEmpty()) {
            throw new IllegalArgumentException("attestation sem cadeia de certificados");
        }
    }
}
