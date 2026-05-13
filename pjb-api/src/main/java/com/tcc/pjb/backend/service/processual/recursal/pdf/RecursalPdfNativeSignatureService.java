package com.tcc.pjb.backend.service.processual.recursal.pdf;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreMaterial;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import javax.security.auth.x500.X500Principal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class RecursalPdfNativeSignatureService {

    private static final String BC_PROVIDER = "BC";

    private final PjbHardwareSecurityModule hsm;
    private final AuditLedgerService auditLedgerService;
    private final JudicialKeyStoreLoader judicialKeyStoreLoader;
    private final RecursalNativePdfSignatureProperties properties;

    public RecursalPdfNativeSignatureService(PjbHardwareSecurityModule hsm,
                                             AuditLedgerService auditLedgerService,
                                             JudicialKeyStoreLoader judicialKeyStoreLoader,
                                             RecursalNativePdfSignatureProperties properties) {
        this.hsm = Objects.requireNonNull(hsm, "hsm");
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService, "auditLedgerService");
        this.judicialKeyStoreLoader = Objects.requireNonNull(judicialKeyStoreLoader, "judicialKeyStoreLoader");
        this.properties = Objects.requireNonNull(properties, "properties");
        ensureBouncyCastleProvider();
    }

    public RecursalPdfArtifact applyNativeSignature(Processo processo,
                                                    Usuario usuario,
                                                    LegalAppealType appealType,
                                                    RecursalPdfArtifact artifact,
                                                    Map<String, Object> assinaturaVinculada,
                                                    Map<String, Object> sigiloRecursal) {
        if (artifact == null || !artifact.available()) {
            return artifact == null ? RecursalPdfArtifact.unavailable() : artifact;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(artifact.metadata());
        metadata.put("nativePdfSignatureProfileRequested", properties.profile());
        metadata.put("nativePdfSignatureRequestedAt", Instant.now().toString());
        if (!properties.enabled()) {
            metadata.put("nativePdfSignatureStatus", "DISABLED_BY_CONFIGURATION");
            return artifact.withMergedMetadata(metadata);
        }
        try {
            PdfSignerMaterial material = resolveMaterial(usuario, assinaturaVinculada);
            if (material == null) {
                metadata.put("nativePdfSignatureStatus", "PENDING_CERTIFICATE_CHAIN_BINDING");
                metadata.put("nativePdfSignatureEmbedded", false);
                metadata.put("nativePdfSignatureEligible", !hsm.isMock());
                metadata.put("nativePdfSignatureReason", "CERTIFICATE_CHAIN_OR_KEYSTORE_REFERENCE_UNAVAILABLE");
                auditLedgerService.appendSafely("RECURSAL_PDF_NATIVE_SIGNATURE_SKIPPED", "RECURSAL_PDF", artifact.sha256(), null, "PENDING_CERTIFICATE_CHAIN_BINDING");
                return artifact.withMergedMetadata(metadata);
            }
            SignedPdf signedPdf = signPdfBytes(artifact.bytes(), processo, usuario, appealType, material, assinaturaVinculada, sigiloRecursal);
            metadata.put("nativePdfSignatureStatus", signedPdf.status());
            metadata.put("nativePdfSignatureEmbedded", true);
            metadata.put("nativePdfSignatureMocked", material.mocked());
            metadata.put("nativePdfSignatureProvider", material.providerName());
            metadata.put("nativePdfSignatureAlgorithm", material.signatureAlgorithm());
            metadata.put("nativePdfSignatureSubFilter", signedPdf.subFilter());
            metadata.put("nativePdfSignatureFilter", signedPdf.filter());
            metadata.put("nativePdfSignatureProfile", properties.profile());
            metadata.put("nativePdfSignatureSigner", material.signerSubject());
            metadata.put("nativePdfSignatureSignedAt", signedPdf.signedAt().toString());
            metadata.put("nativePdfOriginalSha256", artifact.sha256());
            metadata.put("nativePdfSignedSha256", Hashes.sha256Hex(signedPdf.bytes()));
            metadata.put("nativePdfByteLength", signedPdf.bytes().length);
            metadata.put("nativePdfSignatureMode", material.mode());
            metadata.put("nativePdfVisibleAuditStamp", false);
            metadata.put("nativePdfTimestampExternalAuthority", bool(sigiloRecursal, "timestampExternalAuthority"));
            auditLedgerService.appendSafely("RECURSAL_PDF_NATIVE_SIGNATURE_EMBEDDED", "RECURSAL_PDF", artifact.sha256(), Hashes.sha256Hex(signedPdf.bytes()), signedPdf.status());
            return new RecursalPdfArtifact(
                    signedPdf.bytes(),
                    artifact.filename(),
                    artifact.mediaType(),
                    Hashes.sha256Hex(signedPdf.bytes()),
                    signedPdf.pageCount(),
                    Collections.unmodifiableMap(metadata)
            );
        } catch (Exception ex) {
            metadata.put("nativePdfSignatureStatus", "EMBEDDING_FAILED");
            metadata.put("nativePdfSignatureEmbedded", false);
            metadata.put("nativePdfSignatureFailure", ex.getClass().getSimpleName());
            auditLedgerService.appendSafely("RECURSAL_PDF_NATIVE_SIGNATURE_FAILED", "RECURSAL_PDF", artifact.sha256(), null, ex.getClass().getSimpleName());
            return artifact.withMergedMetadata(metadata);
        }
    }

    @Nullable
    private PdfSignerMaterial resolveMaterial(Usuario usuario,
                                              Map<String, Object> assinaturaVinculada) throws Exception {
        String keyStoreRef = firstNonBlank(
                stringValue(assinaturaVinculada, "connectorKeyStoreRef"),
                stringValue(assinaturaVinculada, "keyStoreRef"),
                properties.keyStoreRef()
        );
        if (keyStoreRef != null) {
            return loadKeyStoreMaterial(keyStoreRef, firstNonBlank(stringValue(assinaturaVinculada, "keyAlias"), properties.keyAlias()));
        }
        if (!hsm.isMock()) {
            return null;
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        String signerName = firstNonBlank(
                usuario == null ? null : usuario.getNome(),
                properties.signerName(),
                "PJB Recursal"
        );
        X509Certificate certificate = generateSelfSignedCertificate(keyPair, signerName);
        return new PdfSignerMaterial(
                keyPair.getPrivate(),
                List.of(certificate),
                "SHA256withRSA",
                BC_PROVIDER,
                signerName,
                true,
                "EPHEMERAL_MOCK_CERTIFICATE"
        );
    }

    private PdfSignerMaterial loadKeyStoreMaterial(String keyStoreRef,
                                                   String preferredAlias) throws Exception {
        JudicialKeyStoreMaterial material = judicialKeyStoreLoader.loadKeyStore(keyStoreRef);
        String alias = firstNonBlank(preferredAlias, material.preferredAlias());
        if (alias == null) {
            return null;
        }
        char[] password = firstNonNull(material.keyPasswordCopy(), material.storePasswordCopy());
        try {
            Key key = material.keyStore().getKey(alias, password);
            if (!(key instanceof PrivateKey privateKey)) {
                return null;
            }
            Certificate[] chain = material.keyStore().getCertificateChain(alias);
            ArrayList<X509Certificate> certs = new ArrayList<>();
            if (chain != null) {
                for (Certificate certificate : chain) {
                    if (certificate instanceof X509Certificate x509Certificate) {
                        certs.add(x509Certificate);
                    }
                }
            } else {
                Certificate certificate = material.keyStore().getCertificate(alias);
                if (certificate instanceof X509Certificate x509Certificate) {
                    certs.add(x509Certificate);
                }
            }
            if (certs.isEmpty()) {
                return null;
            }
            String algorithm = defaultSignatureAlgorithm(privateKey.getAlgorithm());
            return new PdfSignerMaterial(
                    privateKey,
                    List.copyOf(certs),
                    algorithm,
                    material.providerName(),
                    certs.getFirst().getSubjectX500Principal().getName(),
                    false,
                    "KEYSTORE_CHAIN"
            );
        } finally {
            clear(password);
        }
    }

    private SignedPdf signPdfBytes(byte[] originalPdf,
                                   Processo processo,
                                   Usuario usuario,
                                   LegalAppealType appealType,
                                   PdfSignerMaterial material,
                                   Map<String, Object> assinaturaVinculada,
                                   Map<String, Object> sigiloRecursal) throws Exception {
        try (PDDocument document = Loader.loadPDF(originalPdf); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(firstNonBlank(
                    usuario == null ? null : usuario.getNome(),
                    material.signerSubject(),
                    properties.signerName()
            ));
            signature.setLocation(firstNonBlank(properties.location(), processo == null ? null : processo.getTribunal(), "PJB"));
            signature.setReason(composeReason(appealType, assinaturaVinculada, sigiloRecursal));
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
            calendar.setTimeInMillis(System.currentTimeMillis());
            signature.setSignDate(calendar);
            document.addSignature(signature);
            ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(outputStream);
            byte[] content;
            try (InputStream inputStream = externalSigning.getContent()) {
                content = inputStream.readAllBytes();
            }
            byte[] cmsSignature = buildCmsDetachedSignature(content, material);
            externalSigning.setSignature(cmsSignature);
            byte[] signedPdf = outputStream.toByteArray();
            try (PDDocument signed = Loader.loadPDF(signedPdf)) {
                return new SignedPdf(
                        signedPdf,
                        signed.getNumberOfPages(),
                        PDSignature.FILTER_ADOBE_PPKLITE.getName(),
                        PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED.getName(),
                        Instant.now(),
                        material.mocked() ? "EMBEDDED_NATIVE_CMS_SIGNATURE_MOCK" : "EMBEDDED_NATIVE_CMS_SIGNATURE"
                );
            }
        }
    }

    private byte[] buildCmsDetachedSignature(byte[] content,
                                             PdfSignerMaterial material) throws Exception {
        List<X509Certificate> chain = material.certificateChain();
        if (chain.isEmpty()) {
            throw new IllegalStateException("Certificate chain unavailable for CMS detached signature.");
        }
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(material.signatureAlgorithm());
        if (firstNonBlank(material.providerName()) != null) {
            signerBuilder.setProvider(material.providerName());
        }
        ContentSigner contentSigner = signerBuilder.build(material.privateKey());
        DigestCalculatorProvider digestProvider = new JcaDigestCalculatorProviderBuilder()
                .setProvider(BC_PROVIDER)
                .build();
        SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestProvider)
                .build(contentSigner, chain.getFirst());
        generator.addSignerInfoGenerator(signerInfoGenerator);
        generator.addCertificates(new JcaCertStore(chain));
        CMSTypedData data = new CMSProcessableByteArray(content);
        CMSSignedData signedData = generator.generate(data, false);
        return signedData.getEncoded();
    }

    private X509Certificate generateSelfSignedCertificate(KeyPair keyPair,
                                                          String signerName) throws Exception {
        Instant now = Instant.now();
        X500Name subject = new X500Name("CN=" + signerName.replace(',', ' '));
        BigInteger serial = new BigInteger(64, new SecureRandom()).abs();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Principal(subject.toString()),
                serial,
                java.util.Date.from(now.minus(1, ChronoUnit.DAYS)),
                java.util.Date.from(now.plus(365, ChronoUnit.DAYS)),
                new X500Principal(subject.toString()),
                keyPair.getPublic()
        );
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BC_PROVIDER)
                .build(keyPair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider(BC_PROVIDER)
                .getCertificate(holder);
    }

    private String composeReason(LegalAppealType appealType,
                                 Map<String, Object> assinaturaVinculada,
                                 Map<String, Object> sigiloRecursal) {
        return String.join(" | ", List.of(
                properties.reason(),
                firstNonBlank(appealType == null ? null : appealType.name(), "RECURSO"),
                firstNonBlank(stringValue(assinaturaVinculada, "signatureMode"), "ASSINATURA_CONTROLADA"),
                firstNonBlank(stringValue(sigiloRecursal, "nivelRecomendado"), "SIGILO_INDEFINIDO")
        ));
    }

    private String defaultSignatureAlgorithm(String keyAlgorithm) {
        String normalized = firstNonBlank(keyAlgorithm, "RSA");
        if (normalized == null) {
            return "SHA256withRSA";
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "EC", "ECDSA" -> "SHA256withECDSA";
            case "RSASSA-PSS" -> "RSASSA-PSS";
            default -> "SHA256withRSA";
        };
    }

    private static void ensureBouncyCastleProvider() {
        if (Security.getProvider(BC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static String stringValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        String out = value.toString().trim();
        return out.isBlank() ? null : out;
    }

    private static boolean bool(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return false;
        }
        Object value = map.get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return "true".equalsIgnoreCase(value.toString().trim());
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

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static void clear(char[] value) {
        if (value != null) {
            java.util.Arrays.fill(value, '\0');
        }
    }

    private record PdfSignerMaterial(
            PrivateKey privateKey,
            List<X509Certificate> certificateChain,
            String signatureAlgorithm,
            String providerName,
            String signerSubject,
            boolean mocked,
            String mode
    ) {
    }

    private record SignedPdf(
            byte[] bytes,
            int pageCount,
            String filter,
            String subFilter,
            Instant signedAt,
            String status
    ) {
    }
}
