package com.tcc.pjb.backend.service.processual.recursal.pdf;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.security.JudicialCertificateValidationReport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCertificateValidationService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorTlsMode;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreMaterial;
import com.tcc.pjb.backend.integration.judicial.security.JudicialResolvedSecurityBinding;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfLongTermValidationBundle;
import com.tcc.pjb.backend.model.entity.Processo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class RecursalPdfLongTermValidationService {

    private static final COSName SUBFILTER_ETSI_RFC3161 = COSName.getPDFName("ETSI.RFC3161");
    private static final COSName DOC_TIME_STAMP = COSName.getPDFName("DocTimeStamp");
    private static final COSName DSS = COSName.getPDFName("DSS");
    private static final COSName VRI = COSName.getPDFName("VRI");
    private static final COSName CERTS = COSName.getPDFName("Certs");
    private static final COSName OCSPS = COSName.getPDFName("OCSPs");
    private static final COSName CRLS = COSName.getPDFName("CRLs");
    private static final COSName PBJ_EVIDENCE = COSName.getPDFName("PBJEvidence");
    private static final COSName PBJ_PROFILE = COSName.getPDFName("PBJProfile");
    private static final COSName PBJ_REVOCATION = COSName.getPDFName("PBJRevocationEvidence");
    private static final COSName PBJ_LAST_MATERIALIZED_AT = COSName.getPDFName("PBJLastMaterializedAt");

    private final AuditLedgerService auditLedgerService;
    private final RecursalPdfLongTermValidationProperties properties;
    private final RecursalTimestampAuthorityService timestampAuthorityService;
    private final JudicialConnectorCertificateValidationService certificateValidationService;
    private final JudicialKeyStoreLoader judicialKeyStoreLoader;

    public RecursalPdfLongTermValidationService(AuditLedgerService auditLedgerService,
                                                RecursalPdfLongTermValidationProperties properties,
                                                RecursalTimestampAuthorityService timestampAuthorityService,
                                                JudicialConnectorCertificateValidationService certificateValidationService,
                                                JudicialKeyStoreLoader judicialKeyStoreLoader) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService, "auditLedgerService");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.timestampAuthorityService = Objects.requireNonNull(timestampAuthorityService, "timestampAuthorityService");
        this.certificateValidationService = Objects.requireNonNull(certificateValidationService, "certificateValidationService");
        this.judicialKeyStoreLoader = Objects.requireNonNull(judicialKeyStoreLoader, "judicialKeyStoreLoader");
    }

    public RecursalPdfArtifact prepare(Processo processo,
                                       LegalAppealType appealType,
                                       RecursalPdfArtifact artifact,
                                       Map<String, Object> assinaturaVinculada,
                                       Map<String, Object> sigiloRecursal) {
        if (artifact == null || !artifact.available() || !properties.enabled()) {
            return artifact == null ? RecursalPdfArtifact.unavailable() : artifact;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(artifact.metadata());
        metadata.put("longTermProfileRequested", properties.profileRequested());
        metadata.put("longTermPreparedAt", Instant.now().toString());
        boolean nativeSigned = Boolean.TRUE.equals(metadata.get("nativePdfSignatureEmbedded"));
        if (!properties.embedDocumentTimestamp() || !nativeSigned) {
            metadata.put("documentTimestampStatus", !nativeSigned ? "SKIPPED_NATIVE_SIGNATURE_REQUIRED" : "DISABLED_BY_CONFIGURATION");
            return artifact.withMergedMetadata(cleanMap(metadata));
        }
        RecursalPdfArtifact prepared = embedDocumentTimestamp(processo, appealType, artifact, sigiloRecursal, metadata);
        if (prepared == null || !prepared.available()) {
            return artifact.withMergedMetadata(cleanMap(metadata));
        }
        if (Boolean.TRUE.equals(properties.materializeDss()) || Boolean.TRUE.equals(properties.materializeVri())) {
            return materializeDssAndVri(processo, appealType, prepared, assinaturaVinculada, sigiloRecursal);
        }
        return prepared;
    }

    public RecursalPdfArtifact finalizeEvidence(Processo processo,
                                                LegalAppealType appealType,
                                                RecursalPdfArtifact artifact,
                                                Map<String, Object> assinaturaVinculada,
                                                Map<String, Object> sigiloRecursal) {
        if (artifact == null || !artifact.available() || !properties.enabled()) {
            return artifact == null ? RecursalPdfArtifact.unavailable() : artifact;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(artifact.metadata());
        ArrayList<String> warnings = new ArrayList<>();
        JudicialCertificateValidationReport certificateValidation = validateCertificate(processo, assinaturaVinculada, metadata, warnings);
        String proofEnvelopeId = stringValue(metadata, "proofEnvelopeId");
        String proofEnvelopeHash = proofEnvelopeHash(metadata.get("proofEnvelope"));
        String evidenceDigest = Hashes.sha256Hex(String.join("|",
                artifact.sha256(),
                safe(proofEnvelopeId),
                safe(proofEnvelopeHash),
                safe(stringValue(metadata, "documentTimestampTokenSha256")),
                certificateValidation == null ? "NO_CERT_REPORT" : Hashes.sha256Hex(certificateValidation.toMap().toString()),
                safe(stringValue(metadata, "dssEvidenceDigestSha256"))
        ));
        RecursalTimestampAuthorityService.RecursalTimeStampToken archiveToken = null;
        if (properties.issueArchiveTimestamp()) {
            archiveToken = timestampAuthorityService.issueSha256Token(
                    hexToBytes(evidenceDigest),
                    "ARCHIVE_EVIDENCE_TIMESTAMP",
                    stringValue(sigiloRecursal, "archiveTimestampAuthority"),
                    bool(sigiloRecursal, "timestampExternalAuthority"),
                    cleanMap(
                            "processoId", processo == null ? null : processo.getId(),
                            "appealType", appealType == null ? null : appealType.name(),
                            "artifactSha256", artifact.sha256(),
                            "evidenceDigestSha256", evidenceDigest
                    )
            );
            if (archiveToken == null) {
                warnings.add("ARCHIVE_TIMESTAMP_UNAVAILABLE");
            }
        }
        boolean certificatePassed = certificateValidation != null
                && certificateValidation.validNow()
                && certificateValidation.pathValidationSucceeded()
                && !certificateValidation.revocationHardFailed();
        boolean dssMaterialized = bool(metadata, "dssMaterialized");
        boolean vriMaterialized = bool(metadata, "vriMaterialized");
        boolean revocationMaterialized = bool(metadata, "revocationMaterialized");
        if (Boolean.TRUE.equals(properties.requireRevocationMaterialization()) && certificatePassed && !revocationMaterialized) {
            warnings.add("REVOCATION_EVIDENCE_NOT_MATERIALIZED");
        }
        if (Boolean.TRUE.equals(properties.requireExternalActForLta()) && archiveToken != null && archiveToken.mocked()) {
            warnings.add("ARCHIVE_TIMESTAMP_WITHOUT_EXTERNAL_ACT");
        }
        String profileAchieved = resolveProfile(metadata, certificateValidation, archiveToken, warnings);
        RecursalPdfLongTermValidationBundle bundle = new RecursalPdfLongTermValidationBundle(
                properties.profileRequested(),
                profileAchieved,
                Instant.now(),
                bool(metadata, "documentTimestampEmbedded"),
                stringValue(metadata, "documentTimestampStatus"),
                stringValue(metadata, "documentTimestampAuthority"),
                bool(metadata, "documentTimestampMocked"),
                stringValue(metadata, "documentTimestampTokenSha256"),
                new byte[0],
                parseInstant(stringValue(metadata, "documentTimestampedAt")),
                archiveToken == null ? null : archiveToken.mocked() ? "ARCHIVE_RFC3161_TOKEN_MOCK" : "ARCHIVE_RFC3161_TOKEN",
                archiveToken == null ? null : archiveToken.authorityName(),
                archiveToken != null && archiveToken.mocked(),
                archiveToken == null ? null : archiveToken.tokenSha256(),
                archiveToken == null ? new byte[0] : archiveToken.tokenBytes(),
                archiveToken == null ? null : archiveToken.generatedAt(),
                evidenceDigest,
                certificateValidation != null,
                certificatePassed,
                dssMaterialized,
                vriMaterialized,
                revocationMaterialized,
                stringValue(metadata, "dssMaterializationStatus"),
                intValue(metadata.get("dssCertCount")),
                intValue(metadata.get("dssVriEntryCount")),
                certificateValidation == null ? Map.of() : certificateValidation.toMap(),
                List.copyOf(warnings),
                cleanMap(
                        "proofEnvelopeId", proofEnvelopeId,
                        "proofEnvelopeSha256", proofEnvelopeHash,
                        "appealType", appealType == null ? null : appealType.name(),
                        "processoId", processo == null ? null : processo.getId(),
                        "requireRevocationMaterialization", properties.requireRevocationMaterialization(),
                        "requireExternalActForLta", properties.requireExternalActForLta()
                )
        );
        metadata.put("longTermValidationBundle", bundle.toMap());
        metadata.put("documentTimestampEmbedded", bundle.documentTimestampEmbedded());
        metadata.put("documentTimestampStatus", bundle.documentTimestampStatus());
        metadata.put("documentTimestampAuthority", bundle.documentTimestampAuthority());
        metadata.put("documentTimestampMocked", bundle.documentTimestampMocked());
        metadata.put("documentTimestampTokenSha256", bundle.documentTimestampTokenSha256());
        metadata.put("padesProfileCandidate", profileAchieved);
        String archiveTimestampTokenSha256 = archiveToken == null
                ? firstNonBlank(stringValue(metadata, "documentTimestampTokenSha256"), evidenceDigest)
                : archiveToken.tokenSha256();
        metadata.put("archiveTimestampStatus", archiveToken == null ? "UNAVAILABLE" : archiveToken.mocked() ? "ARCHIVE_RFC3161_TOKEN_MOCK" : "ARCHIVE_RFC3161_TOKEN");
        metadata.put("archiveTimestampAuthority", archiveToken == null ? firstNonBlank(stringValue(metadata, "documentTimestampAuthority"), "PJB TSA") : archiveToken.authorityName());
        metadata.put("archiveTimestampMocked", archiveToken == null || archiveToken.mocked());
        metadata.put("archiveTimestampTokenSha256", archiveTimestampTokenSha256);
        metadata.put("archiveTimestampedAt", archiveToken == null ? stringValue(metadata, "documentTimestampedAt") : archiveToken.generatedAt().toString());
        metadata.put("certificateValidationStatus", certificateValidation == null ? "UNAVAILABLE" : certificateValidation.status());
        metadata.put("certificateValidationPassed", certificatePassed);
        auditLedgerService.appendSafely("RECURSAL_PDF_LONG_TERM_EVIDENCE_FINALIZED", "RECURSAL_PDF", artifact.sha256(), evidenceDigest, profileAchieved);
        return artifact.withMergedMetadata(cleanMap(metadata));
    }

    private RecursalPdfArtifact embedDocumentTimestamp(Processo processo,
                                                       LegalAppealType appealType,
                                                       RecursalPdfArtifact artifact,
                                                       Map<String, Object> sigiloRecursal,
                                                       LinkedHashMap<String, Object> metadata) {
        try (PDDocument document = Loader.loadPDF(artifact.bytes()); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(SUBFILTER_ETSI_RFC3161);
            signature.setType(DOC_TIME_STAMP);
            signature.setName("PJB TSA");
            signature.setLocation(firstNonBlank(processo == null ? null : processo.getTribunal(), "PJB"));
            signature.setReason("Carimbo temporal RFC3161 para evidência recursal de longo prazo");
            java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"), Locale.ROOT);
            calendar.setTimeInMillis(System.currentTimeMillis());
            signature.setSignDate(calendar);
            document.addSignature(signature);
            ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(output);
            byte[] content;
            try (InputStream inputStream = externalSigning.getContent()) {
                content = inputStream.readAllBytes();
            }
            byte[] imprint = MessageDigest.getInstance("SHA-256").digest(content);
            RecursalTimestampAuthorityService.RecursalTimeStampToken token = timestampAuthorityService.issueSha256Token(
                    imprint,
                    "PDF_DOCUMENT_TIMESTAMP",
                    stringValue(sigiloRecursal, "timestampAuthority"),
                    bool(sigiloRecursal, "timestampExternalAuthority"),
                    cleanMap(
                            "processoId", processo == null ? null : processo.getId(),
                            "appealType", appealType == null ? null : appealType.name(),
                            "artifactSha256", artifact.sha256()
                    )
            );
            if (token == null || token.tokenBytes().length == 0) {
                return markEmbeddedMockDocumentTimestamp(artifact, metadata, imprint);
            }
            externalSigning.setSignature(token.tokenBytes());
            byte[] stampedPdf = output.toByteArray();
            try (PDDocument stamped = Loader.loadPDF(stampedPdf)) {
                metadata.put("documentTimestampStatus", token.mocked() ? "EMBEDDED_RFC3161_DOCUMENT_TIMESTAMP_MOCK" : "EMBEDDED_RFC3161_DOCUMENT_TIMESTAMP");
                metadata.put("documentTimestampEmbedded", true);
                metadata.put("documentTimestampAuthority", token.authorityName());
                metadata.put("documentTimestampMocked", token.mocked());
                metadata.put("documentTimestampExternalAuthority", token.externalAuthority());
                metadata.put("documentTimestampProfile", token.profile());
                metadata.put("documentTimestampTokenSha256", token.tokenSha256());
                metadata.put("documentTimestampedAt", token.generatedAt().toString());
                metadata.put("documentTimestampSignatureCount", stamped.getSignatureDictionaries().size());
                metadata.put("documentTimestampSubFilter", SUBFILTER_ETSI_RFC3161.getName());
                metadata.put("documentTimestampImprintSha256", Hashes.sha256Hex(imprint));
                auditLedgerService.appendSafely("RECURSAL_PDF_DOCUMENT_TIMESTAMP_EMBEDDED", "RECURSAL_PDF", artifact.sha256(), Hashes.sha256Hex(stampedPdf), token.tokenSha256());
                return new RecursalPdfArtifact(
                        stampedPdf,
                        artifact.filename(),
                        artifact.mediaType(),
                        Hashes.sha256Hex(stampedPdf),
                        stamped.getNumberOfPages(),
                        Map.copyOf(cleanMap(metadata))
                );
            }
        } catch (Exception ex) {
            metadata.put("documentTimestampStatus", "EMBEDDING_FAILED");
            metadata.put("documentTimestampFailure", ex.getClass().getSimpleName());
            auditLedgerService.appendSafely("RECURSAL_PDF_DOCUMENT_TIMESTAMP_FAILED", "RECURSAL_PDF", artifact.sha256(), null, ex.getClass().getSimpleName());
            return artifact.withMergedMetadata(cleanMap(metadata));
        }
    }

    private RecursalPdfArtifact markEmbeddedMockDocumentTimestamp(RecursalPdfArtifact artifact,
                                                                 LinkedHashMap<String, Object> metadata,
                                                                 byte[] imprint) {
        metadata.put("documentTimestampStatus", "EMBEDDED_RFC3161_DOCUMENT_TIMESTAMP_MOCK");
        metadata.put("documentTimestampEmbedded", true);
        metadata.put("documentTimestampAuthority", "PJB TSA MOCK");
        metadata.put("documentTimestampMocked", true);
        metadata.put("documentTimestampExternalAuthority", false);
        metadata.put("documentTimestampProfile", "RFC3161_INTERNAL_MOCK");
        metadata.put("documentTimestampTokenSha256", Hashes.sha256Hex(imprint));
        metadata.put("documentTimestampedAt", Instant.now().toString());
        metadata.put("documentTimestampSubFilter", SUBFILTER_ETSI_RFC3161.getName());
        metadata.put("documentTimestampImprintSha256", Hashes.sha256Hex(imprint));
        auditLedgerService.appendSafely("RECURSAL_PDF_DOCUMENT_TIMESTAMP_EMBEDDED", "RECURSAL_PDF", artifact.sha256(), Hashes.sha256Hex(imprint), "MOCK_TSA");
        return artifact.withMergedMetadata(cleanMap(metadata));
    }

    private RecursalPdfArtifact materializeDssAndVri(Processo processo,
                                                     LegalAppealType appealType,
                                                     RecursalPdfArtifact artifact,
                                                     Map<String, Object> assinaturaVinculada,
                                                     Map<String, Object> sigiloRecursal) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(artifact.metadata());
        ArrayList<String> warnings = new ArrayList<>();
        JudicialCertificateValidationReport certificateValidation = validateCertificate(processo, assinaturaVinculada, metadata, warnings);
        List<X509Certificate> chain = loadCertificateChain(assinaturaVinculada);
        if (chain.size() == 0) {
            metadata.put("dssMaterializationStatus", "SKIPPED_CERTIFICATE_CHAIN_UNAVAILABLE");
            metadata.put("dssMaterialized", false);
            metadata.put("vriMaterialized", false);
            metadata.put("revocationMaterialized", false);
            if (warnings.size() != 0) {
                metadata.put("dssWarnings", List.copyOf(warnings));
            }
            return artifact.withMergedMetadata(cleanMap(metadata));
        }
        try (PDDocument document = Loader.loadPDF(artifact.bytes()); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            if (signatures.size() == 0) {
                metadata.put("dssMaterializationStatus", "SKIPPED_SIGNATURES_UNAVAILABLE");
                metadata.put("dssMaterialized", false);
                metadata.put("vriMaterialized", false);
                metadata.put("revocationMaterialized", false);
                return artifact.withMergedMetadata(cleanMap(metadata));
            }
            COSDictionary catalog = document.getDocumentCatalog().getCOSObject();
            COSDictionary dss = dictionary(catalog.getDictionaryObject(DSS));
            COSArray certArray = ensureArray(dss, CERTS);
            LinkedHashSet<String> certFingerprints = new LinkedHashSet<>();
            List<COSStream> certStreams = new ArrayList<>();
            for (X509Certificate certificate : chain) {
                byte[] encoded = certificate.getEncoded();
                String fingerprint = Hashes.sha256Hex(encoded);
                if (!certFingerprints.add(fingerprint)) {
                    continue;
                }
                COSStream certStream = document.getDocument().createCOSStream();
                certStream.setName(COSName.TYPE, "EmbeddedFile");
                certStream.setName(COSName.SUBTYPE, "application/pkix-cert");
                certStream.setString(COSName.getPDFName("PBJSha256"), fingerprint);
                certStream.setString(COSName.getPDFName("PBJSubject"), certificate.getSubjectX500Principal().getName());
                certStream.setString(COSName.getPDFName("PBJIssuer"), certificate.getIssuerX500Principal().getName());
                writeBytes(certStream, encoded);
                certArray.add(certStream);
                certStreams.add(certStream);
            }
            COSDictionary vri = ensureDictionary(dss, VRI);
            COSStream revocationStream = createRevocationEvidenceStream(document, certificateValidation, processo, appealType, sigiloRecursal);
            if (revocationStream != null) {
                if (booleanValue(certificateValidation == null ? null : certificateValidation.metadata(), "ocspEnabled")) {
                    ensureArray(dss, OCSPS).add(revocationStream);
                }
                if (booleanValue(certificateValidation == null ? null : certificateValidation.metadata(), "crlEnabled")) {
                    ensureArray(dss, CRLS).add(revocationStream);
                }
                dss.setItem(PBJ_REVOCATION, revocationStream);
            }
            COSStream evidenceStream = document.getDocument().createCOSStream();
            LinkedHashMap<String, Object> dssEvidence = cleanMap(
                    "artifactSha256", artifact.sha256(),
                    "appealType", appealType == null ? null : appealType.name(),
                    "processoId", processo == null ? null : processo.getId(),
                    "certificateChainSize", certStreams.size(),
                    "certificateValidation", certificateValidation == null ? null : certificateValidation.toMap(),
                    "sigiloNivel", stringValue(sigiloRecursal, "nivelRecomendado")
            );
            writeBytes(evidenceStream, dssEvidence.toString().getBytes(StandardCharsets.UTF_8));
            dss.setItem(PBJ_EVIDENCE, evidenceStream);
            dss.setString(PBJ_PROFILE, properties.profileRequested());
            dss.setString(PBJ_LAST_MATERIALIZED_AT, Instant.now().toString());
            int vriEntries = 0;
            if (Boolean.TRUE.equals(properties.materializeVri())) {
                for (PDSignature signature : signatures) {
                    COSDictionary vriEntry = new COSDictionary();
                    COSArray vriCerts = new COSArray();
                    for (COSStream certStream : certStreams) {
                        vriCerts.add(certStream);
                    }
                    if (vriCerts.size() != 0) {
                        vriEntry.setItem(CERTS, vriCerts);
                    }
                    if (revocationStream != null) {
                        if (booleanValue(certificateValidation == null ? null : certificateValidation.metadata(), "ocspEnabled")) {
                            COSArray ocspRefs = new COSArray();
                            ocspRefs.add(revocationStream);
                            vriEntry.setItem(OCSPS, ocspRefs);
                        }
                        if (booleanValue(certificateValidation == null ? null : certificateValidation.metadata(), "crlEnabled")) {
                            COSArray crlRefs = new COSArray();
                            crlRefs.add(revocationStream);
                            vriEntry.setItem(CRLS, crlRefs);
                        }
                    }
                    vriEntry.setString(COSName.getPDFName("PBJSubFilter"), signature.getSubFilter());
                    vriEntry.setString(COSName.getPDFName("PBJFilter"), signature.getFilter());
                    vriEntry.setString(COSName.getPDFName("PBJSignDate"), signature.getSignDate() == null ? null : Instant.ofEpochMilli(signature.getSignDate().getTimeInMillis()).toString());
                    String signatureKey = signatureKey(signature, artifact.bytes());
                    vri.setItem(COSName.getPDFName(signatureKey), vriEntry);
                    vriEntries++;
                }
            }
            if (vriEntries == 0 && !Boolean.TRUE.equals(properties.materializeVri())) {
                dss.removeItem(VRI);
            }
            catalog.setItem(DSS, dss);
            document.saveIncremental(output);
            byte[] materializedPdf = output.toByteArray();
            metadata.put("dssMaterializationStatus", revocationStream == null ? "EMBEDDED_DSS_VRI_WITHOUT_REVOCATION_BYTES" : "EMBEDDED_DSS_VRI_WITH_REVOCATION_EVIDENCE");
            metadata.put("dssMaterialized", true);
            metadata.put("vriMaterialized", vriEntries > 0);
            metadata.put("revocationMaterialized", revocationStream != null && certificateValidation != null && certificateValidation.revocationAttempted() && !certificateValidation.revocationHardFailed());
            metadata.put("dssCertCount", certStreams.size());
            metadata.put("dssVriEntryCount", vriEntries);
            metadata.put("dssEvidenceDigestSha256", Hashes.sha256Hex(dssEvidence.toString()));
            metadata.put("dssRevocationStatus", certificateValidation == null ? "UNAVAILABLE" : certificateValidation.revocationHardFailed() ? "FAILED" : certificateValidation.revocationAttempted() ? "MATERIALIZED" : "NOT_ATTEMPTED");
            if (warnings.size() != 0) {
                metadata.put("dssWarnings", List.copyOf(warnings));
            }
            auditLedgerService.appendSafely("RECURSAL_PDF_DSS_VRI_MATERIALIZED", "RECURSAL_PDF", artifact.sha256(), Hashes.sha256Hex(materializedPdf), stringValue(metadata, "dssMaterializationStatus"));
            return new RecursalPdfArtifact(
                    materializedPdf,
                    artifact.filename(),
                    artifact.mediaType(),
                    Hashes.sha256Hex(materializedPdf),
                    artifact.pageCount(),
                    Map.copyOf(cleanMap(metadata))
            );
        } catch (Exception ex) {
            metadata.put("dssMaterializationStatus", "EMBEDDING_FAILED");
            metadata.put("dssMaterializationFailure", ex.getClass().getSimpleName());
            auditLedgerService.appendSafely("RECURSAL_PDF_DSS_VRI_FAILED", "RECURSAL_PDF", artifact.sha256(), null, ex.getClass().getSimpleName());
            return artifact.withMergedMetadata(cleanMap(metadata));
        }
    }

    @Nullable
    private COSStream createRevocationEvidenceStream(PDDocument document,
                                                     @Nullable JudicialCertificateValidationReport certificateValidation,
                                                     Processo processo,
                                                     LegalAppealType appealType,
                                                     Map<String, Object> sigiloRecursal) throws IOException {
        if (certificateValidation == null) {
            return null;
        }
        LinkedHashMap<String, Object> evidence = cleanMap(
                "validatedAt", certificateValidation.validatedAt() == null ? null : certificateValidation.validatedAt().toString(),
                "status", certificateValidation.status(),
                "validNow", certificateValidation.validNow(),
                "pathValidationSucceeded", certificateValidation.pathValidationSucceeded(),
                "revocationAttempted", certificateValidation.revocationAttempted(),
                "revocationSoftFailed", certificateValidation.revocationSoftFailed(),
                "revocationHardFailed", certificateValidation.revocationHardFailed(),
                "sha256Fingerprint", certificateValidation.sha256Fingerprint(),
                "processoId", processo == null ? null : processo.getId(),
                "appealType", appealType == null ? null : appealType.name(),
                "timestampExternalAuthority", bool(sigiloRecursal, "timestampExternalAuthority"),
                "metadata", certificateValidation.metadata(),
                "warnings", certificateValidation.warnings(),
                "blockers", certificateValidation.blockers()
        );
        COSStream stream = document.getDocument().createCOSStream();
        stream.setName(COSName.TYPE, "EmbeddedFile");
        stream.setName(COSName.SUBTYPE, "application/pjb-revocation-evidence+json");
        stream.setString(COSName.getPDFName("PBJRevocationStatus"), certificateValidation.status());
        writeBytes(stream, evidence.toString().getBytes(StandardCharsets.UTF_8));
        return stream;
    }

    @Nullable
    private JudicialCertificateValidationReport validateCertificate(Processo processo,
                                                                    Map<String, Object> assinaturaVinculada,
                                                                    Map<String, Object> metadata,
                                                                    List<String> warnings) {
        String keyStoreRef = firstNonBlank(stringValue(assinaturaVinculada, "connectorKeyStoreRef"), stringValue(assinaturaVinculada, "keyStoreRef"));
        if (keyStoreRef == null) {
            warnings.add("CERTIFICATE_VALIDATION_KEYSTORE_UNAVAILABLE");
            return null;
        }
        JudicialResolvedSecurityBinding binding = new JudicialResolvedSecurityBinding(
                "recursal-pdf-signature",
                JudicialSystem.PJE,
                processo == null ? null : processo.getTribunal(),
                "recursal-pdf",
                true,
                JudicialConnectorTlsMode.MTLS,
                keyStoreRef,
                firstNonBlank(stringValue(assinaturaVinculada, "trustStoreRef"), properties.trustStoreRef()),
                stringValue(assinaturaVinculada, "keyAlias"),
                stringValue(assinaturaVinculada, "certificateAlias"),
                true,
                true,
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                List.of("TLSv1.3"),
                List.of(),
                List.of(),
                cleanMap("artifactSha256", metadata.get("nativePdfSignedSha256"), "channel", "RECURSAL_PDF")
        );
        JudicialCertificateValidationReport report = certificateValidationService.validate(JudicialSystem.PJE, processo == null ? null : processo.getTribunal(), binding, cleanMap("channel", "RECURSAL_PDF"));
        if (!report.validNow() || !report.pathValidationSucceeded()) {
            warnings.add("CERTIFICATE_PATH_VALIDATION_PENDING_OR_FAILED");
        }
        if (Boolean.TRUE.equals(properties.requireRevocationMaterialization()) && !report.revocationAttempted()) {
            warnings.add("CERTIFICATE_REVOCATION_NOT_ATTEMPTED");
        }
        return report;
    }

    private String resolveProfile(Map<String, Object> metadata,
                                  @Nullable JudicialCertificateValidationReport certificateValidation,
                                  @Nullable RecursalTimestampAuthorityService.RecursalTimeStampToken archiveToken,
                                  List<String> warnings) {
        boolean nativeSignature = bool(metadata, "nativePdfSignatureEmbedded");
        boolean documentTimestamp = bool(metadata, "documentTimestampEmbedded");
        boolean dssMaterialized = bool(metadata, "dssMaterialized");
        boolean vriMaterialized = bool(metadata, "vriMaterialized");
        boolean revocationMaterialized = bool(metadata, "revocationMaterialized");
        boolean certificateOk = certificateValidation != null
                && certificateValidation.validNow()
                && certificateValidation.pathValidationSucceeded()
                && !certificateValidation.revocationHardFailed();
        boolean externalArchive = archiveToken != null && !archiveToken.mocked();
        boolean externalActSatisfied = !Boolean.TRUE.equals(properties.requireExternalActForLta()) || externalArchive;
        if (nativeSignature && documentTimestamp && certificateOk && dssMaterialized && vriMaterialized && revocationMaterialized && archiveToken != null && externalActSatisfied) {
            return "PADES_LTA_EVIDENCE_CANDIDATE";
        }
        if (nativeSignature && documentTimestamp && certificateOk && dssMaterialized && vriMaterialized && (!Boolean.TRUE.equals(properties.requireRevocationMaterialization()) || revocationMaterialized)) {
            if (archiveToken != null && !externalActSatisfied) {
                warnings.add("LTA_PROFILE_DOWNGRADED_EXTERNAL_ACT_REQUIRED");
            }
            return "PADES_LT_EVIDENCE_CANDIDATE";
        }
        if (nativeSignature && documentTimestamp) {
            if (certificateOk && (!dssMaterialized || !vriMaterialized)) {
                warnings.add("LT_PROFILE_DOWNGRADED_DSS_VRI_MISSING");
            }
            return "PADES_T_EVIDENCE_CANDIDATE";
        }
        if (nativeSignature) {
            return "PADES_B_EVIDENCE_CANDIDATE";
        }
        return "PROOF_ENVELOPE_ONLY";
    }

    private List<X509Certificate> loadCertificateChain(Map<String, Object> assinaturaVinculada) {
        String keyStoreRef = firstNonBlank(stringValue(assinaturaVinculada, "connectorKeyStoreRef"), stringValue(assinaturaVinculada, "keyStoreRef"));
        if (keyStoreRef == null) {
            return List.of();
        }
        try {
            JudicialKeyStoreMaterial material = judicialKeyStoreLoader.loadKeyStore(keyStoreRef);
            String alias = firstNonBlank(stringValue(assinaturaVinculada, "keyAlias"), material.preferredAlias());
            if (alias == null) {
                return List.of();
            }
            KeyStore keyStore = material.keyStore();
            Certificate[] chain = keyStore.getCertificateChain(alias);
            ArrayList<X509Certificate> certificates = new ArrayList<>();
            if (chain != null) {
                for (Certificate certificate : chain) {
                    if (certificate instanceof X509Certificate x509Certificate) {
                        certificates.add(x509Certificate);
                    }
                }
            } else {
                Certificate certificate = keyStore.getCertificate(alias);
                if (certificate instanceof X509Certificate x509Certificate) {
                    certificates.add(x509Certificate);
                }
            }
            return List.copyOf(certificates);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static COSDictionary dictionary(org.apache.pdfbox.cos.COSBase base) {
        return base instanceof COSDictionary dictionary ? dictionary : new COSDictionary();
    }

    private static COSDictionary ensureDictionary(COSDictionary dictionary, COSName key) {
        COSDictionary value = dictionary(dictionary.getDictionaryObject(key));
        dictionary.setItem(key, value);
        return value;
    }

    private static COSArray ensureArray(COSDictionary dictionary, COSName key) {
        org.apache.pdfbox.cos.COSBase base = dictionary.getDictionaryObject(key);
        COSArray array = base instanceof COSArray cosArray ? cosArray : new COSArray();
        dictionary.setItem(key, array);
        return array;
    }

    private static void writeBytes(COSStream stream, byte[] bytes) throws IOException {
        try (OutputStream outputStream = stream.createOutputStream()) {
            outputStream.write(bytes);
        }
    }

    private static String signatureKey(PDSignature signature, byte[] pdfBytes) {
        try {
            byte[] contents = signature.getContents(pdfBytes);
            if (contents != null && contents.length > 0) {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(contents)).toUpperCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        String fallback = String.join("|",
                safe(signature.getSubFilter()),
                safe(signature.getFilter()),
                signature.getSignDate() == null ? "" : String.valueOf(signature.getSignDate().getTimeInMillis())
        );
        return Hashes.sha256Hex(fallback).substring(0, 40).toUpperCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private String proofEnvelopeHash(Object proofEnvelope) {
        if (!(proofEnvelope instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = ((Map<String, Object>) map).get("envelopeSha256");
        return value == null ? null : String.valueOf(value);
    }

    private static LinkedHashMap<String, Object> cleanMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null || source.size() == 0) {
            return out;
        }
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out;
    }

    private static LinkedHashMap<String, Object> cleanMap(Object... values) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (values == null) {
            return out;
        }
        for (int i = 0; i + 1 < values.length; i += 2) {
            Object key = values[i];
            Object value = values[i + 1];
            if (key instanceof String stringKey && value != null) {
                out.put(stringKey, value);
            }
        }
        return out;
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private static String stringValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isBlank() ? null : out;
    }

    private static boolean bool(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return false;
        }
        return booleanValue(map, key);
    }

    private static boolean booleanValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return false;
        }
        Object value = map.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isBlank()) {
            return new byte[0];
        }
        return HexFormat.of().parseHex(hex);
    }

    private static Instant parseInstant(String value) {
        try {
            return value == null ? null : Instant.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return 0;
        }
    }
}
