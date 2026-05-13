package com.tcc.pjb.backend.service.processual.recursal.pdf;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfValidationResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.springframework.stereotype.Service;

@Service
public class RecursalPdfArtifactValidationService {

    private static final COSName DSS = COSName.getPDFName("DSS");
    private static final COSName VRI = COSName.getPDFName("VRI");
    private static final COSName CERTS = COSName.getPDFName("Certs");
    private static final COSName OCSPS = COSName.getPDFName("OCSPs");
    private static final COSName CRLS = COSName.getPDFName("CRLs");

    private final AuditLedgerService auditLedgerService;

    public RecursalPdfArtifactValidationService(AuditLedgerService auditLedgerService) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService, "auditLedgerService");
    }

    @SuppressWarnings("unchecked")
    public RecursalPdfValidationResult validate(RecursalPdfArtifact artifact,
                                                boolean certificateRequired) {
        ArrayList<String> errors = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        if (artifact == null || !artifact.available()) {
            errors.add("PDF_PROTOCOLAVEL_INDISPONIVEL");
            if (certificateRequired) {
                errors.add("REAL_CERTIFICATE_SIGNATURE_REQUIRED");
                details.put("certificateRequired", true);
            }
            RecursalPdfValidationResult result = RecursalPdfValidationResult.invalid("INVALID_PROTOCOL_ARTIFACT", errors, warnings, details);
            auditLedgerService.appendSafely("RECURSAL_PDF_VALIDATION_FAILED", "RECURSAL_PDF", "UNAVAILABLE", null, result.status());
            return result;
        }
        String actualHash = Hashes.sha256Hex(artifact.bytes());
        details.put("documentSha256", actualHash);
        if (!actualHash.equalsIgnoreCase(artifact.sha256())) {
            errors.add("DOCUMENT_HASH_MISMATCH");
        }
        inspectPdfEvidence(artifact, errors, warnings, details);
        boolean nativeEmbedded = Boolean.TRUE.equals(artifact.metadata().get("nativePdfSignatureEmbedded"));
        boolean nativeMocked = Boolean.TRUE.equals(artifact.metadata().get("nativePdfSignatureMocked"));
        String nativeStatus = text(artifact.metadata().get("nativePdfSignatureStatus"));
        if (nativeStatus != null) {
            details.put("nativePdfSignatureStatus", nativeStatus);
        }
        if (nativeEmbedded) {
            details.put("nativePdfSignatureProfile", artifact.metadata().get("nativePdfSignatureProfile"));
            details.put("nativePdfSignatureMocked", nativeMocked);
            details.put("nativePdfSignatureSigner", artifact.metadata().get("nativePdfSignatureSigner"));
        } else if (nativeStatus != null && !nativeStatus.startsWith("DISABLED")) {
            warnings.add("PDF_NATIVE_SIGNATURE_NOT_EMBEDDED");
        }
        Object proofObject = artifact.metadata().get("proofEnvelope");
        Map<String, Object> proof = proofObject instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        if (proof.size() == 0) {
            errors.add("PROOF_ENVELOPE_ABSENT");
        } else {
            details.put("proofEnvelopeId", proof.get("envelopeId"));
            details.put("proofMode", proof.get("proofMode"));
            if (!actualHash.equals(String.valueOf(proof.get("documentSha256")))) {
                errors.add("PROOF_DOCUMENT_HASH_MISMATCH");
            }
            if (blank(proof.get("signatureDigestSha256")) || blank(proof.get("signatureBase64"))) {
                errors.add("DETACHED_SIGNATURE_ABSENT");
            }
            if (blank(proof.get("timestampTokenSha256")) || blank(proof.get("timestampTokenBase64"))) {
                errors.add("TIMESTAMP_TOKEN_ABSENT");
            }
            boolean signatureMocked = Boolean.TRUE.equals(proof.get("signatureMocked"));
            boolean timestampExternalAuthority = Boolean.TRUE.equals(proof.get("timestampExternalAuthority"));
            details.put("signatureMocked", signatureMocked);
            details.put("timestampAuthority", proof.get("timestampAuthority"));
            details.put("timestampExternalAuthority", timestampExternalAuthority);
            boolean nativeRealCertificate = nativeEmbedded && !nativeMocked;
            if (certificateRequired && signatureMocked && !nativeRealCertificate) {
                errors.add("REAL_CERTIFICATE_SIGNATURE_REQUIRED");
            } else if (signatureMocked && !nativeRealCertificate) {
                warnings.add("MOCK_HSM_SIGNATURE_ACTIVE");
            }
            if (!timestampExternalAuthority) {
                warnings.add("TIMESTAMP_WITHOUT_EXTERNAL_ACT");
            }
        }
        Object longTermObject = artifact.metadata().get("longTermValidationBundle");
        Map<String, Object> longTerm = longTermObject instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        if (longTerm.size() != 0) {
            details.put("padesProfileCandidate", longTerm.get("profileAchieved"));
            details.put("longTermProfileRequested", longTerm.get("profileRequested"));
            boolean certificateValidationAvailable = Boolean.TRUE.equals(longTerm.get("certificateValidationAvailable"));
            boolean certificateValidationPassed = Boolean.TRUE.equals(longTerm.get("certificateValidationPassed"));
            details.put("certificateValidationAvailable", certificateValidationAvailable);
            details.put("certificateValidationPassed", certificateValidationPassed);
            details.put("dssMaterialized", longTerm.get("dssMaterialized"));
            details.put("vriMaterialized", longTerm.get("vriMaterialized"));
            details.put("revocationMaterialized", longTerm.get("revocationMaterialized"));
            String archiveTimestampStatus = text(longTerm.get("archiveTimestampStatus"));
            if (archiveTimestampStatus != null) {
                details.put("archiveTimestampStatus", archiveTimestampStatus);
            }
            if (certificateRequired && certificateValidationAvailable && !certificateValidationPassed) {
                errors.add("LONG_TERM_CERTIFICATE_VALIDATION_FAILED");
            }
            if (blank(longTerm.get("documentTimestampStatus")) || !Boolean.TRUE.equals(longTerm.get("documentTimestampEmbedded"))) {
                warnings.add("LONG_TERM_DOCUMENT_TIMESTAMP_NOT_CONFIRMED");
            }
            if (blank(longTerm.get("archiveTimestampTokenSha256"))) {
                warnings.add("LONG_TERM_ARCHIVE_TIMESTAMP_UNAVAILABLE");
            }
            String profileCandidate = text(longTerm.get("profileAchieved"));
            if (("PADES_LT_EVIDENCE_CANDIDATE".equals(profileCandidate) || "PADES_LTA_EVIDENCE_CANDIDATE".equals(profileCandidate))
                    && !Boolean.TRUE.equals(longTerm.get("dssMaterialized"))) {
                errors.add("LONG_TERM_DSS_NOT_MATERIALIZED");
            }
            if (("PADES_LT_EVIDENCE_CANDIDATE".equals(profileCandidate) || "PADES_LTA_EVIDENCE_CANDIDATE".equals(profileCandidate))
                    && !Boolean.TRUE.equals(longTerm.get("vriMaterialized"))) {
                errors.add("LONG_TERM_VRI_NOT_MATERIALIZED");
            }
            if ("PADES_LTA_EVIDENCE_CANDIDATE".equals(profileCandidate) && !Boolean.TRUE.equals(longTerm.get("revocationMaterialized"))) {
                warnings.add("LONG_TERM_REVOCATION_NOT_MATERIALIZED");
            }
        } else {
            warnings.add("LONG_TERM_VALIDATION_BUNDLE_ABSENT");
        }
        RecursalPdfValidationResult result = errors.size() == 0
                ? RecursalPdfValidationResult.valid(warnings.size() == 0 ? "VALID_PROTOCOL_ARTIFACT" : "VALID_PROTOCOL_ARTIFACT_WITH_WARNINGS", warnings, details)
                : RecursalPdfValidationResult.invalid("INVALID_PROTOCOL_ARTIFACT", errors, warnings, details);
        auditLedgerService.appendSafely(
                result.valid() ? "RECURSAL_PDF_VALIDATED" : "RECURSAL_PDF_VALIDATION_FAILED",
                "RECURSAL_PDF",
                artifact.sha256(),
                actualHash,
                result.status()
        );
        return result;
    }

    private void inspectPdfEvidence(RecursalPdfArtifact artifact,
                                    List<String> errors,
                                    List<String> warnings,
                                    Map<String, Object> details) {
        try (PDDocument document = Loader.loadPDF(artifact.bytes())) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            details.put("pdfSignatureCount", signatures.size());
            boolean nativeSignaturePresent = signatures.stream()
                    .map(PDSignature::getSubFilter)
                    .anyMatch(value -> "adbe.pkcs7.detached".equalsIgnoreCase(value) || "ETSI.CAdES.detached".equalsIgnoreCase(value));
            boolean documentTimestampPresent = signatures.stream()
                    .map(PDSignature::getSubFilter)
                    .anyMatch(value -> "ETSI.RFC3161".equalsIgnoreCase(value));
            details.put("pdfNativeSignaturePresent", nativeSignaturePresent);
            details.put("pdfDocumentTimestampPresent", documentTimestampPresent);
            COSDictionary catalog = document.getDocumentCatalog().getCOSObject();
            COSDictionary dss = catalog.getDictionaryObject(DSS) instanceof COSDictionary dictionary ? dictionary : null;
            COSDictionary vri = dss != null && dss.getDictionaryObject(VRI) instanceof COSDictionary dictionary ? dictionary : null;
            COSArray certs = dss != null && dss.getDictionaryObject(CERTS) instanceof COSArray array ? array : null;
            COSArray ocsps = dss != null && dss.getDictionaryObject(OCSPS) instanceof COSArray array ? array : null;
            COSArray crls = dss != null && dss.getDictionaryObject(CRLS) instanceof COSArray array ? array : null;
            boolean dssPresent = dss != null;
            boolean vriPresent = vri != null && vri.keySet().size() != 0;
            details.put("pdfDssPresent", dssPresent);
            details.put("pdfVriPresent", vriPresent);
            details.put("pdfDssCertCount", certs == null ? 0 : certs.size());
            details.put("pdfDssOcspCount", ocsps == null ? 0 : ocsps.size());
            details.put("pdfDssCrlCount", crls == null ? 0 : crls.size());
            if (Boolean.TRUE.equals(artifact.metadata().get("nativePdfSignatureEmbedded")) && !nativeSignaturePresent) {
                errors.add("PDF_NATIVE_SIGNATURE_DICTIONARY_MISSING");
            }
            if (Boolean.TRUE.equals(artifact.metadata().get("documentTimestampEmbedded")) && !documentTimestampPresent && !documentTimestampAcceptedByEvidenceBundle(artifact)) {
                errors.add("PDF_DOCUMENT_TIMESTAMP_DICTIONARY_MISSING");
            }
            if (Boolean.TRUE.equals(artifact.metadata().get("dssMaterialized")) && !dssPresent) {
                errors.add("PDF_DSS_DICTIONARY_MISSING");
            }
            if (Boolean.TRUE.equals(artifact.metadata().get("vriMaterialized")) && !vriPresent) {
                errors.add("PDF_VRI_DICTIONARY_MISSING");
            }
            if (dssPresent && (certs == null || certs.size() == 0)) {
                warnings.add("PDF_DSS_CERTIFICATE_CHAIN_EMPTY");
            }
            if (nativeSignaturePresent && !documentTimestampPresent && !documentTimestampAcceptedByEvidenceBundle(artifact)) {
                warnings.add("PDF_DOCUMENT_TIMESTAMP_NOT_EMBEDDED");
            }
        } catch (Exception ex) {
            warnings.add("PDF_SIGNATURE_INSPECTION_FAILED");
            details.put("pdfSignatureInspectionFailure", ex.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean documentTimestampAcceptedByEvidenceBundle(RecursalPdfArtifact artifact) {
        Object raw = artifact.metadata().get("longTermValidationBundle");
        Map<String, Object> longTerm = raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        return Boolean.TRUE.equals(longTerm.get("documentTimestampEmbedded"))
                && Boolean.TRUE.equals(longTerm.get("documentTimestampMocked"))
                && !blank(longTerm.get("documentTimestampTokenSha256"));
    }

    private boolean blank(Object value) {
        return value == null || String.valueOf(value).trim().isBlank();
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isBlank() ? null : out;
    }
}
