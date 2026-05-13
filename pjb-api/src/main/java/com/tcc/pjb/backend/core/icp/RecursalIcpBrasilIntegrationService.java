package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignerMaterialSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignaturePolicySnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateSnapshot;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class RecursalIcpBrasilIntegrationService {

    private final IcpBrasilSignatureProperties properties;
    private final IcpBrasilSignerCertificateResolver certificateResolver;
    private final IcpBrasilChainValidator chainValidator;
    private final RecursalIcpBrasilMetadataAssembler metadataAssembler;

    public RecursalIcpBrasilIntegrationService(IcpBrasilSignatureProperties properties,
                                               IcpBrasilSignerCertificateResolver certificateResolver,
                                               IcpBrasilChainValidator chainValidator,
                                               RecursalIcpBrasilMetadataAssembler metadataAssembler) {
        this.properties = Objects.requireNonNull(properties);
        this.certificateResolver = Objects.requireNonNull(certificateResolver);
        this.chainValidator = Objects.requireNonNull(chainValidator);
        this.metadataAssembler = Objects.requireNonNull(metadataAssembler);
    }


    public IcpBrasilSignaturePolicySnapshot policySnapshot() {
        return chainValidator.policySnapshot();
    }

    public IcpBrasilSignerMaterialSnapshot signerMaterialSnapshot() {
        X509Certificate certificate = certificateResolver.resolveRecursalCertificate();
        return new IcpBrasilSignerMaterialSnapshot("RECURSAL_KEYSTORE", properties.recursalKeyAlias(), certificate != null);
    }

    public IcpBrasilCertificateSnapshot certificateSnapshot() {
        X509Certificate certificate = certificateResolver.resolveRecursalCertificate();
        if (certificate == null) {
            return new IcpBrasilCertificateSnapshot(null, null, null, null);
        }
        IcpBrasilValidationResult validation = chainValidator.validate(certificate);
        return chainValidator.certificateSnapshot(validation.profile());
    }

    public RecursalPdfArtifact apply(Processo processo,
                                     Usuario usuario,
                                     RecursalPdfArtifact artifact,
                                     @Nullable Map<String, Object> assinaturaVinculada,
                                     @Nullable Map<String, Object> sigiloRecursal) {
        if (artifact == null || !artifact.available() || !properties.enabled()) {
            return artifact;
        }
        X509Certificate certificate = certificateResolver.resolveRecursalCertificate();
        String candidate = blankToDefault(properties.recursalProfileCandidate(), "LTA");
        String docHash = artifact.sha256();
        if (certificate == null) {
            if (properties.recursalRequireCertificate()) {
                String reason = "certificado icp-brasil recursal indisponivel";
                chainValidator.registerFailure(processo == null ? null : processo.getId(), docHash, reason);
                throw new IllegalStateException(reason);
            }
            return artifact.withMergedMetadata(Map.of("icpBrasil", metadataAssembler.skippedNoCertificate(candidate).toMap()));
        }
        IcpBrasilValidationResult validation = chainValidator.validate(certificate);
        if (!validation.valid()) {
            chainValidator.registerFailure(processo == null ? null : processo.getId(), docHash, validation.failureReason());
            if (properties.enforceChainValidation()) {
                throw new IllegalStateException(validation.failureReason());
            }
            return artifact.withMergedMetadata(Map.of("icpBrasil", metadataAssembler.validationFailed(candidate, validation.failureReason()).toMap()));
        }
        IcpBrasilCertProfile profile = validation.profile();
        chainValidator.registerSuccess(
                processo == null ? null : processo.getId(),
                docHash,
                candidate,
                "LTA",
                profile,
                true,
                hasMap(artifact.metadata(), "dss"),
                hasMap(artifact.metadata(), "vri")
        );
        RecursalIcpBrasilMetadata metadata = metadataAssembler.validated(candidate, "LTA", profile, usuario);
        return artifact.withMergedMetadata(Map.of("icpBrasil", metadata.toMap()));
    }

    private static boolean hasMap(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        Object value = metadata.get(key);
        return value instanceof Map<?, ?> map && !map.isEmpty();
    }

    private static String blankToDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
