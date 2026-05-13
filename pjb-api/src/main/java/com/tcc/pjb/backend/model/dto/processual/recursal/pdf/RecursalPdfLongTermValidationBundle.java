package com.tcc.pjb.backend.model.dto.processual.recursal.pdf;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecursalPdfLongTermValidationBundle(
        String profileRequested,
        String profileAchieved,
        Instant preparedAt,
        boolean documentTimestampEmbedded,
        String documentTimestampStatus,
        String documentTimestampAuthority,
        boolean documentTimestampMocked,
        String documentTimestampTokenSha256,
        byte[] documentTimestampToken,
        Instant documentTimestampedAt,
        String archiveTimestampStatus,
        String archiveTimestampAuthority,
        boolean archiveTimestampMocked,
        String archiveTimestampTokenSha256,
        byte[] archiveTimestampToken,
        Instant archiveTimestampedAt,
        String evidenceDigestSha256,
        boolean certificateValidationAvailable,
        boolean certificateValidationPassed,
        boolean dssMaterialized,
        boolean vriMaterialized,
        boolean revocationMaterialized,
        String dssStatus,
        int dssCertCount,
        int dssVriEntryCount,
        Map<String, Object> certificateValidation,
        List<String> warnings,
        Map<String, Object> metadata) {

    public RecursalPdfLongTermValidationBundle {
        profileRequested = normalize(profileRequested);
        profileAchieved = normalize(profileAchieved);
        documentTimestampStatus = normalize(documentTimestampStatus);
        documentTimestampAuthority = normalize(documentTimestampAuthority);
        documentTimestampTokenSha256 = normalize(documentTimestampTokenSha256);
        documentTimestampToken = documentTimestampToken == null ? new byte[0] : documentTimestampToken.clone();
        archiveTimestampStatus = normalize(archiveTimestampStatus);
        archiveTimestampAuthority = normalize(archiveTimestampAuthority);
        archiveTimestampTokenSha256 = normalize(archiveTimestampTokenSha256);
        archiveTimestampToken = archiveTimestampToken == null ? new byte[0] : archiveTimestampToken.clone();
        evidenceDigestSha256 = normalize(evidenceDigestSha256);
        dssStatus = normalize(dssStatus);
        certificateValidation = certificateValidation == null || certificateValidation.isEmpty() ? Map.of() : Map.copyOf(certificateValidation);
        warnings = warnings == null || warnings.isEmpty() ? List.of() : List.copyOf(warnings);
        metadata = metadata == null || metadata.isEmpty() ? Map.of() : Map.copyOf(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("profileRequested", profileRequested);
        out.put("profileAchieved", profileAchieved);
        out.put("preparedAt", preparedAt == null ? null : preparedAt.toString());
        out.put("documentTimestampEmbedded", documentTimestampEmbedded);
        out.put("documentTimestampStatus", documentTimestampStatus);
        out.put("documentTimestampAuthority", documentTimestampAuthority);
        out.put("documentTimestampMocked", documentTimestampMocked);
        out.put("documentTimestampTokenSha256", documentTimestampTokenSha256);
        out.put("documentTimestampedAt", documentTimestampedAt == null ? null : documentTimestampedAt.toString());
        if (documentTimestampToken.length > 0) {
            out.put("documentTimestampTokenBase64", Base64.getEncoder().encodeToString(documentTimestampToken));
        }
        out.put("archiveTimestampStatus", archiveTimestampStatus);
        out.put("archiveTimestampAuthority", archiveTimestampAuthority);
        out.put("archiveTimestampMocked", archiveTimestampMocked);
        out.put("archiveTimestampTokenSha256", archiveTimestampTokenSha256);
        out.put("archiveTimestampedAt", archiveTimestampedAt == null ? null : archiveTimestampedAt.toString());
        if (archiveTimestampToken.length > 0) {
            out.put("archiveTimestampTokenBase64", Base64.getEncoder().encodeToString(archiveTimestampToken));
        }
        out.put("evidenceDigestSha256", evidenceDigestSha256);
        out.put("certificateValidationAvailable", certificateValidationAvailable);
        out.put("certificateValidationPassed", certificateValidationPassed);
        out.put("dssMaterialized", dssMaterialized);
        out.put("vriMaterialized", vriMaterialized);
        out.put("revocationMaterialized", revocationMaterialized);
        out.put("dssStatus", dssStatus);
        out.put("dssCertCount", dssCertCount);
        out.put("dssVriEntryCount", dssVriEntryCount);
        if (!certificateValidation.isEmpty()) {
            out.put("certificateValidation", certificateValidation);
        }
        if (!warnings.isEmpty()) {
            out.put("warnings", warnings);
        }
        if (!metadata.isEmpty()) {
            out.put("metadata", metadata);
        }
        out.entrySet().removeIf(entry -> entry.getValue() == null);
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
