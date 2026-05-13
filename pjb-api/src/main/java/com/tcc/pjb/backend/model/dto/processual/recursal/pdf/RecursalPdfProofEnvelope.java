package com.tcc.pjb.backend.model.dto.processual.recursal.pdf;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public record RecursalPdfProofEnvelope(
        String envelopeId,
        String documentSha256,
        Instant signedAt,
        String signatureAlgorithm,
        String signatureProvider,
        boolean signatureMocked,
        byte[] signatureBytes,
        String signatureDigestSha256,
        Instant timestampedAt,
        String timestampAuthority,
        boolean timestampExternalAuthority,
        byte[] timestampToken,
        String timestampTokenSha256,
        String proofMode,
        String envelopeSha256,
        Map<String, Object> metadata) {

    public RecursalPdfProofEnvelope {
        envelopeId = normalize(envelopeId);
        documentSha256 = normalize(documentSha256);
        signatureAlgorithm = normalize(signatureAlgorithm);
        signatureProvider = normalize(signatureProvider);
        signatureBytes = signatureBytes == null ? new byte[0] : signatureBytes.clone();
        signatureDigestSha256 = normalize(signatureDigestSha256);
        timestampAuthority = normalize(timestampAuthority);
        timestampToken = timestampToken == null ? new byte[0] : timestampToken.clone();
        timestampTokenSha256 = normalize(timestampTokenSha256);
        proofMode = normalize(proofMode);
        envelopeSha256 = normalize(envelopeSha256);
        metadata = metadata == null || metadata.isEmpty() ? Map.of() : Map.copyOf(metadata);
    }

    public boolean available() {
        return documentSha256 != null
                && signatureAlgorithm != null
                && signatureProvider != null
                && signatureDigestSha256 != null
                && timestampAuthority != null
                && timestampTokenSha256 != null
                && proofMode != null
                && envelopeSha256 != null
                && signatureBytes.length > 0
                && timestampToken.length > 0;
    }

    public Map<String, Object> toMap() {
        if (!available()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("envelopeId", envelopeId);
        out.put("documentSha256", documentSha256);
        out.put("signedAt", signedAt);
        out.put("signatureAlgorithm", signatureAlgorithm);
        out.put("signatureProvider", signatureProvider);
        out.put("signatureMocked", signatureMocked);
        out.put("signatureDigestSha256", signatureDigestSha256);
        out.put("signatureBase64", Base64.getEncoder().encodeToString(signatureBytes));
        out.put("timestampedAt", timestampedAt);
        out.put("timestampAuthority", timestampAuthority);
        out.put("timestampExternalAuthority", timestampExternalAuthority);
        out.put("timestampTokenSha256", timestampTokenSha256);
        out.put("timestampTokenBase64", Base64.getEncoder().encodeToString(timestampToken));
        out.put("proofMode", proofMode);
        out.put("envelopeSha256", envelopeSha256);
        if (!metadata.isEmpty()) {
            out.put("metadata", metadata);
        }
        return Map.copyOf(out);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
