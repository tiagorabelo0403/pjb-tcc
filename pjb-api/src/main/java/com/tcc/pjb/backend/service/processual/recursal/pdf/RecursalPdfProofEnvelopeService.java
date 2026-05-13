package com.tcc.pjb.backend.service.processual.recursal.pdf;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.icp.RecursalIcpBrasilIntegrationService;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfProofEnvelope;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecursalPdfProofEnvelopeService {

    private final PjbHardwareSecurityModule hsm;
    private final AuditLedgerService auditLedgerService;
    private final RecursalTimestampAuthorityService timestampAuthorityService;
    private final RecursalIcpBrasilIntegrationService recursalIcpBrasilIntegrationService;

    public RecursalPdfProofEnvelopeService(PjbHardwareSecurityModule hsm,
                                           AuditLedgerService auditLedgerService,
                                           RecursalTimestampAuthorityService timestampAuthorityService,
                                           RecursalIcpBrasilIntegrationService recursalIcpBrasilIntegrationService) {
        this.hsm = Objects.requireNonNull(hsm, "hsm");
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService, "auditLedgerService");
        this.timestampAuthorityService = Objects.requireNonNull(timestampAuthorityService, "timestampAuthorityService");
        this.recursalIcpBrasilIntegrationService = Objects.requireNonNull(recursalIcpBrasilIntegrationService, "recursalIcpBrasilIntegrationService");
    }

    public RecursalPdfArtifact seal(Processo processo,
                                    Usuario usuario,
                                    LegalAppealType appealType,
                                    RecursalPdfArtifact artifact,
                                    Map<String, Object> assinaturaVinculada,
                                    Map<String, Object> sigiloRecursal) {
        if (artifact == null || !artifact.available()) {
            return artifact == null ? RecursalPdfArtifact.unavailable() : artifact;
        }
        Instant signedAt = Instant.now();
        String envelopeId = UUID.randomUUID().toString();
        PjbHardwareSecurityModule.AssinaturaHsm signature = hsm.assinar(artifact.bytes());
        String signatureDigestSha256 = Hashes.sha256Hex(signature.bytes());
        Instant timestampedAt = Instant.now();
        String timestampCanonical = timestampCanonical(envelopeId, processo, usuario, appealType, artifact, signatureDigestSha256, signedAt, timestampedAt, assinaturaVinculada, sigiloRecursal);
        RecursalTimestampAuthorityService.RecursalTimeStampToken timestampToken = issueProofTimestamp(timestampCanonical, processo, appealType, artifact, sigiloRecursal);
        PjbHardwareSecurityModule.AssinaturaHsm timestampSignature = timestampToken == null ? hsm.assinar(timestampCanonical.getBytes(StandardCharsets.UTF_8)) : null;
        byte[] timestampBytes = timestampToken == null ? timestampSignature.bytes() : timestampToken.tokenBytes();
        String timestampTokenSha256 = Hashes.sha256Hex(timestampBytes);
        LinkedHashMap<String, Object> proofMetadata = new LinkedHashMap<>();
        put(proofMetadata, "processoId", processo == null ? null : processo.getId());
        put(proofMetadata, "numeroProcesso", processo == null ? null : firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()));
        put(proofMetadata, "appealType", appealType == null ? null : appealType.name());
        put(proofMetadata, "signerUserId", usuario == null ? null : usuario.getId());
        put(proofMetadata, "signerCpf", usuario == null ? null : normalize(usuario.getCpf()));
        put(proofMetadata, "signatureMode", stringValue(assinaturaVinculada, "signatureMode"));
        put(proofMetadata, "proofEnvelopeMode", stringValue(assinaturaVinculada, "proofEnvelopeMode"));
        put(proofMetadata, "nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        put(proofMetadata, "timestampCanonicalHash", Hashes.sha256Hex(timestampCanonical));
        String envelopeSha256 = Hashes.sha256Hex(String.join("|",
                artifact.sha256(),
                signatureDigestSha256,
                timestampTokenSha256,
                signedAt.toString(),
                timestampedAt.toString(),
                envelopeId));
        RecursalPdfProofEnvelope envelope = new RecursalPdfProofEnvelope(
                envelopeId,
                artifact.sha256(),
                signedAt,
                signature.algoritmo(),
                signature.provedorNome(),
                signature.mockada(),
                signature.bytes(),
                signatureDigestSha256,
                timestampedAt,
                timestampToken == null
                        ? (timestampSignature.mockada() ? "LEDGER_HSM_REFERENCE" : "HSM_TIME_REFERENCE")
                        : timestampToken.authorityName(),
                timestampToken != null && timestampToken.externalAuthority(),
                timestampBytes,
                timestampTokenSha256,
                timestampToken == null ? "DETACHED_HSM_TIMESTAMP_ENVELOPE" : "DETACHED_RFC3161_PROOF_ENVELOPE",
                envelopeSha256,
                Map.copyOf(proofMetadata)
        );
        auditLedgerService.appendSafely(
                "RECURSAL_PDF_PROOF_SEALED",
                "RECURSAL_PDF",
                envelopeId,
                envelope.envelopeSha256(),
                "processoId=" + (processo == null ? null : processo.getId())
        );
        LinkedHashMap<String, Object> mergedMetadata = new LinkedHashMap<>(artifact.metadata());
        mergedMetadata.put("proofEnvelope", envelope.toMap());
        mergedMetadata.put("proofEnvelopeId", envelope.envelopeId());
        mergedMetadata.put("proofEnvelopeMode", envelope.proofMode());
        mergedMetadata.put("detachedSignature", true);
        mergedMetadata.put("timestampEmbeddedExternally", false);
        if (timestampToken != null) {
            mergedMetadata.put("proofEnvelopeTimestampProfile", timestampToken.profile());
            mergedMetadata.put("proofEnvelopeTimestampTokenSha256", timestampToken.tokenSha256());
        }
        RecursalPdfArtifact sealedArtifact = artifact.withMergedMetadata(mergedMetadata);
        RecursalPdfArtifact enrichedArtifact = recursalIcpBrasilIntegrationService.apply(processo, usuario, sealedArtifact, assinaturaVinculada, sigiloRecursal);
        return enrichedArtifact == null ? sealedArtifact : enrichedArtifact;
    }

    private RecursalTimestampAuthorityService.RecursalTimeStampToken issueProofTimestamp(String timestampCanonical,
                                                                                         Processo processo,
                                                                                         LegalAppealType appealType,
                                                                                         RecursalPdfArtifact artifact,
                                                                                         Map<String, Object> sigiloRecursal) {
        try {
            byte[] imprint = java.security.MessageDigest.getInstance("SHA-256").digest(timestampCanonical.getBytes(StandardCharsets.UTF_8));
            return timestampAuthorityService.issueSha256Token(
                    imprint,
                    "PROOF_ENVELOPE_TIMESTAMP",
                    stringValue(sigiloRecursal, "timestampAuthority"),
                    Boolean.parseBoolean(String.valueOf(sigiloRecursal == null ? false : sigiloRecursal.getOrDefault("timestampExternalAuthority", false))),
                    cleanMap(
                            "processoId", processo == null ? null : processo.getId(),
                            "appealType", appealType == null ? null : appealType.name(),
                            "artifactSha256", artifact == null ? null : artifact.sha256()
                    )
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private String timestampCanonical(String envelopeId,
                                      Processo processo,
                                      Usuario usuario,
                                      LegalAppealType appealType,
                                      RecursalPdfArtifact artifact,
                                      String signatureDigestSha256,
                                      Instant signedAt,
                                      Instant timestampedAt,
                                      Map<String, Object> assinaturaVinculada,
                                      Map<String, Object> sigiloRecursal) {
        LinkedHashMap<String, Object> canonical = new LinkedHashMap<>();
        put(canonical, "envelopeId", envelopeId);
        put(canonical, "documentSha256", artifact.sha256());
        put(canonical, "signedAt", signedAt.toString());
        put(canonical, "timestampedAt", timestampedAt.toString());
        put(canonical, "signatureDigestSha256", signatureDigestSha256);
        put(canonical, "appealType", appealType == null ? null : appealType.name());
        put(canonical, "processoId", processo == null ? null : processo.getId());
        put(canonical, "numeroProcesso", processo == null ? null : firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()));
        put(canonical, "signerUserId", usuario == null ? null : usuario.getId());
        put(canonical, "signerCpf", usuario == null ? null : normalize(usuario.getCpf()));
        put(canonical, "signatureMode", stringValue(assinaturaVinculada, "signatureMode"));
        put(canonical, "nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        StringBuilder builder = new StringBuilder();
        canonical.forEach((key, value) -> builder.append(key).append('=').append(value).append('\n'));
        return builder.toString();
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

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String stringValue(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : normalize(String.valueOf(value));
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        target.put(key, value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
