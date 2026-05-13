package com.tcc.pjb.backend.service.processual.recursal.pdf;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreMaterial;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenGenerator;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class RecursalTimestampAuthorityService {

    private static final String BC_PROVIDER = "BC";

    private final PjbHardwareSecurityModule hsm;
    private final AuditLedgerService auditLedgerService;
    private final JudicialKeyStoreLoader judicialKeyStoreLoader;
    private final RecursalTimestampAuthorityProperties properties;

    public RecursalTimestampAuthorityService(PjbHardwareSecurityModule hsm,
                                             AuditLedgerService auditLedgerService,
                                             JudicialKeyStoreLoader judicialKeyStoreLoader,
                                             RecursalTimestampAuthorityProperties properties) {
        this.hsm = Objects.requireNonNull(hsm, "hsm");
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService, "auditLedgerService");
        this.judicialKeyStoreLoader = Objects.requireNonNull(judicialKeyStoreLoader, "judicialKeyStoreLoader");
        this.properties = Objects.requireNonNull(properties, "properties");
        ensureBouncyCastleProvider();
    }

    @Nullable
    public RecursalTimeStampToken issueSha256Token(byte[] messageImprint,
                                                   String purpose,
                                                   @Nullable String authorityHint,
                                                   boolean externalAuthorityPreferred,
                                                   Map<String, Object> metadata) {
        if (!properties.enabled() || messageImprint == null || messageImprint.length == 0) {
            return null;
        }
        try {
            TimeStampSignerMaterial material = resolveMaterial(authorityHint);
            if (material == null) {
                return null;
            }
            Instant now = Instant.now();
            BigInteger nonce = new BigInteger(128, new SecureRandom()).abs();
            BigInteger serial = new BigInteger(160, new SecureRandom()).abs();
            ASN1ObjectIdentifier policyOid = new ASN1ObjectIdentifier(properties.policyOid());
            TimeStampRequestGenerator requestGenerator = new TimeStampRequestGenerator();
            requestGenerator.setCertReq(true);
            requestGenerator.setReqPolicy(policyOid);
            TimeStampRequest request = requestGenerator.generate(TSPAlgorithms.SHA256, messageImprint, nonce);
            SignerInfoGenerator signerInfoGenerator = new JcaSimpleSignerInfoGeneratorBuilder()
                    .setProvider(material.providerName())
                    .build(material.signatureAlgorithm(), material.privateKey(), material.certificateChain().getFirst());
            DigestCalculator sha1Calculator = new JcaDigestCalculatorProviderBuilder()
                    .setProvider(BC_PROVIDER)
                    .build()
                    .get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
            TimeStampTokenGenerator tokenGenerator = new TimeStampTokenGenerator(signerInfoGenerator, sha1Calculator, policyOid, true);
            tokenGenerator.setResolution(TimeStampTokenGenerator.R_MILLISECONDS);
            tokenGenerator.setAccuracyMillis(250);
            tokenGenerator.setOrdering(true);
            tokenGenerator.setLocale(Locale.ROOT);
            tokenGenerator.setTSA(new GeneralName(new X500Name("CN=" + material.authorityName().replace(',', ' '))));
            if (properties.includeCertificates()) {
                tokenGenerator.addCertificates(new JcaCertStore(material.certificateChain()));
            }
            TimeStampToken token = tokenGenerator.generate(request, serial, Date.from(now));
            byte[] encoded = token.getEncoded();
            String tokenSha256 = Hashes.sha256Hex(encoded);
            LinkedHashMap<String, Object> ledgerMetadata = cleanMap(metadata);
            ledgerMetadata.put("purpose", normalize(purpose));
            ledgerMetadata.put("authority", material.authorityName());
            ledgerMetadata.put("mocked", material.mocked());
            ledgerMetadata.put("policyOid", policyOid.getId());
            auditLedgerService.appendSafely(
                    "RECURSAL_RFC3161_TOKEN_ISSUED",
                    "RECURSAL_TIMESTAMP",
                    tokenSha256,
                    Hashes.sha256Hex(messageImprint),
                    normalize(ledgerMetadata.toString())
            );
            return new RecursalTimeStampToken(
                    encoded,
                    tokenSha256,
                    now,
                    material.authorityName(),
                    externalAuthorityPreferred && !material.mocked(),
                    material.mocked(),
                    policyOid.getId(),
                    nonce.toString(16),
                    serial.toString(16),
                    normalize(purpose),
                    material.profile(),
                    Map.copyOf(cleanMap(ledgerMetadata))
            );
        } catch (Exception ex) {
            auditLedgerService.appendSafely("RECURSAL_RFC3161_TOKEN_FAILED", "RECURSAL_TIMESTAMP", normalize(purpose), null, ex.getClass().getSimpleName());
            return null;
        }
    }

    @Nullable
    private TimeStampSignerMaterial resolveMaterial(@Nullable String authorityHint) throws Exception {
        String keyStoreRef = properties.keyStoreRef();
        if (keyStoreRef != null) {
            return loadKeyStoreMaterial(keyStoreRef, properties.keyAlias(), firstNonBlank(authorityHint, properties.authorityName()));
        }
        if (!hsm.isMock() && !properties.allowEphemeralFallback()) {
            return null;
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        String authorityName = firstNonBlank(authorityHint, properties.authorityName(), "PJB TSA");
        X509Certificate certificate = generateSelfSignedCertificate(keyPair, authorityName);
        return new TimeStampSignerMaterial(
                keyPair.getPrivate(),
                List.of(certificate),
                "SHA256withRSA",
                BC_PROVIDER,
                authorityName,
                true,
                firstNonBlank(properties.profile(), "RFC3161_INTERNAL")
        );
    }

    private TimeStampSignerMaterial loadKeyStoreMaterial(String keyStoreRef,
                                                         String preferredAlias,
                                                         String authorityName) throws Exception {
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
            return new TimeStampSignerMaterial(
                    privateKey,
                    List.copyOf(certs),
                    defaultSignatureAlgorithm(privateKey.getAlgorithm()),
                    firstNonBlank(material.providerName(), BC_PROVIDER),
                    firstNonBlank(authorityName, certs.getFirst().getSubjectX500Principal().getName()),
                    false,
                    firstNonBlank(properties.profile(), "RFC3161_EXTERNAL")
            );
        } finally {
            clear(password);
        }
    }

    private X509Certificate generateSelfSignedCertificate(KeyPair keyPair,
                                                          String signerName) throws Exception {
        Instant now = Instant.now();
        X500Name subject = new X500Name("CN=" + signerName.replace(',', ' '));
        BigInteger serial = new BigInteger(64, new SecureRandom()).abs();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Principal(subject.toString()),
                serial,
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                new X500Principal(subject.toString()),
                keyPair.getPublic()
        );
        org.bouncycastle.operator.ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BC_PROVIDER)
                .build(keyPair.getPrivate());
        builder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(holder);
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

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static LinkedHashMap<String, Object> cleanMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out;
    }

    private record TimeStampSignerMaterial(
            PrivateKey privateKey,
            List<X509Certificate> certificateChain,
            String signatureAlgorithm,
            String providerName,
            String authorityName,
            boolean mocked,
            String profile
    ) {
    }

    public record RecursalTimeStampToken(
            byte[] tokenBytes,
            String tokenSha256,
            Instant generatedAt,
            String authorityName,
            boolean externalAuthority,
            boolean mocked,
            String policyOid,
            String nonceHex,
            String serialHex,
            String purpose,
            String profile,
            Map<String, Object> metadata
    ) {
        public RecursalTimeStampToken {
            tokenBytes = tokenBytes == null ? new byte[0] : tokenBytes.clone();
            metadata = metadata == null || metadata.isEmpty() ? Map.of() : Map.copyOf(cleanMap(metadata));
        }
    }
}
