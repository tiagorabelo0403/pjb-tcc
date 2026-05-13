package com.tcc.pjb.backend.service.processual.recursal.protocolo;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfValidationResult;
import com.tcc.pjb.backend.model.dto.processual.recursal.protocolo.RecursalProtocolArtifactReadiness;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecursalProtocolArtifactReadinessService {

    public RecursalProtocolArtifactReadiness assess(RecursalPdfArtifact artifact,
                                                    RecursalPdfValidationResult validation,
                                                    JudicialConnectorOperationalProfileReport operationalProfile,
                                                    boolean certificateRequired) {
        boolean protocolArtifactValid = validation != null && validation.valid();
        boolean connectorSubmissionReady = protocolArtifactValid
                && operationalProfile != null
                && operationalProfile.readyForTribunalSubmission();
        String profile = artifact == null ? null : text(artifact.metadata().get("padesProfileCandidate"));
        boolean nativeEmbedded = artifact != null && Boolean.TRUE.equals(artifact.metadata().get("nativePdfSignatureEmbedded"));
        boolean documentTimestampEmbedded = artifact != null && Boolean.TRUE.equals(artifact.metadata().get("documentTimestampEmbedded"));
        boolean dssMaterialized = artifact != null && Boolean.TRUE.equals(artifact.metadata().get("dssMaterialized"));
        boolean vriMaterialized = artifact != null && Boolean.TRUE.equals(artifact.metadata().get("vriMaterialized"));
        boolean revocationMaterialized = artifact != null && Boolean.TRUE.equals(artifact.metadata().get("revocationMaterialized"));

        ArrayList<String> reasons = new ArrayList<>();
        if (!protocolArtifactValid) {
            reasons.add("PROTOCOL_ARTIFACT_INVALID");
        }
        if (operationalProfile == null || !operationalProfile.readyForTribunalSubmission()) {
            reasons.add("CONNECTOR_SUBMISSION_REVIEW_REQUIRED");
        }
        if (profile == null) {
            reasons.add("PADES_PROFILE_UNAVAILABLE");
        }

        Map<String, Object> longTerm = longTermBundle(artifact);
        boolean certificateValidationAvailable = Boolean.TRUE.equals(longTerm.get("certificateValidationAvailable"));
        boolean certificateValidationPassed = Boolean.TRUE.equals(longTerm.get("certificateValidationPassed"));

        boolean productionEvidenceReady;
        if (!protocolArtifactValid) {
            productionEvidenceReady = false;
        } else if (certificateRequired) {
            productionEvidenceReady = nativeEmbedded
                    && documentTimestampEmbedded
                    && isLtOrBetter(profile)
                    && dssMaterialized
                    && vriMaterialized
                    && certificateValidationAvailable
                    && certificateValidationPassed
                    && (!isLta(profile) || revocationMaterialized);
            if (!nativeEmbedded) {
                reasons.add("NATIVE_PDF_SIGNATURE_REQUIRED");
            }
            if (!documentTimestampEmbedded) {
                reasons.add("DOCUMENT_TIMESTAMP_REQUIRED");
            }
            if (!isLtOrBetter(profile)) {
                reasons.add("LONG_TERM_PADES_PROFILE_REQUIRED");
            }
            if (!dssMaterialized) {
                reasons.add("DSS_MATERIALIZATION_REQUIRED");
            }
            if (!vriMaterialized) {
                reasons.add("VRI_MATERIALIZATION_REQUIRED");
            }
            if (!certificateValidationAvailable) {
                reasons.add("CERTIFICATE_VALIDATION_REQUIRED_FOR_PRODUCTION");
            } else if (!certificateValidationPassed) {
                reasons.add("CERTIFICATE_VALIDATION_NOT_PASSED");
            }
            if (isLta(profile) && !revocationMaterialized) {
                reasons.add("REVOCATION_EVIDENCE_REQUIRED_FOR_LTA");
            }
        } else {
            productionEvidenceReady = nativeEmbedded
                    && documentTimestampEmbedded
                    && isLtOrBetter(profile)
                    && dssMaterialized
                    && vriMaterialized;
            if (!nativeEmbedded) {
                reasons.add("NATIVE_PDF_SIGNATURE_RECOMMENDED");
            }
            if (!documentTimestampEmbedded) {
                reasons.add("DOCUMENT_TIMESTAMP_RECOMMENDED");
            }
            if (!isLtOrBetter(profile)) {
                reasons.add("LONG_TERM_PADES_PROFILE_RECOMMENDED");
            }
            if (!dssMaterialized) {
                reasons.add("DSS_MATERIALIZATION_RECOMMENDED");
            }
            if (!vriMaterialized) {
                reasons.add("VRI_MATERIALIZATION_RECOMMENDED");
            }
        }

        boolean readyForProduction = connectorSubmissionReady
                && productionEvidenceReady
                && operationalProfile != null
                && operationalProfile.readyForProduction();
        boolean reviewableLongTermEvidence = !protocolArtifactValid && nativeEmbedded && documentTimestampEmbedded;
        String status = reviewableLongTermEvidence
                ? "REVIEW_LONG_TERM_EVIDENCE_BEFORE_PRODUCTION"
                : !protocolArtifactValid
                ? "BLOCKED_BY_PROTOCOL_ARTIFACT_VALIDATION"
                : !connectorSubmissionReady
                ? "REVIEW_BEFORE_CONNECTOR_SUBMISSION"
                : !productionEvidenceReady
                ? "REVIEW_LONG_TERM_EVIDENCE_BEFORE_PRODUCTION"
                : readyForProduction
                ? "READY_FOR_REAL_CONNECTOR_SUBMISSION"
                : "REVIEW_BEFORE_CONNECTOR_SUBMISSION";
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("certificateRequired", certificateRequired);
        details.put("padesProfileCandidate", profile);
        details.put("nativePdfSignatureEmbedded", nativeEmbedded);
        details.put("documentTimestampEmbedded", documentTimestampEmbedded);
        details.put("dssMaterialized", dssMaterialized);
        details.put("vriMaterialized", vriMaterialized);
        details.put("revocationMaterialized", revocationMaterialized);
        details.put("connectorReadyForTribunalSubmission", operationalProfile != null && operationalProfile.readyForTribunalSubmission());
        details.put("connectorReadyForProduction", operationalProfile != null && operationalProfile.readyForProduction());
        details.put("certificateValidationAvailable", certificateValidationAvailable);
        details.put("certificateValidationPassed", certificateValidationPassed);
        details.entrySet().removeIf(entry -> entry.getValue() == null);
        return new RecursalProtocolArtifactReadiness(
                status,
                protocolArtifactValid,
                connectorSubmissionReady,
                productionEvidenceReady,
                readyForProduction,
                profile,
                List.copyOf(reasons),
                Map.copyOf(details)
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> longTermBundle(RecursalPdfArtifact artifact) {
        if (artifact == null) {
            return Map.of();
        }
        Object value = artifact.metadata().get("longTermValidationBundle");
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private boolean isLtOrBetter(String profile) {
        return "PADES_LT_EVIDENCE_CANDIDATE".equals(profile) || "PADES_LTA_EVIDENCE_CANDIDATE".equals(profile);
    }

    private boolean isLta(String profile) {
        return "PADES_LTA_EVIDENCE_CANDIDATE".equals(profile);
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isBlank() ? null : out;
    }
}
