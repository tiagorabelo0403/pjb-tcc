package com.tcc.pjb.backend.integration.judicial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

class JudicialConnectorCertificateValidationServiceTest {

    private static final String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDaTCCAlGgAwIBAgIUPOoG4BFJ++8Ec7+zaGfJyROps+owDQYJKoZIhvcNAQEL\n" +
            "BQAwRDEUMBIGA1UEAwwLUEpCIFRlc3QgQ0ExDDAKBgNVBAoMA1BKQjERMA8GA1UE\n" +
            "CwwIU2VjdXJpdHkxCzAJBgNVBAYTAkJSMB4XDTI2MDMxMDIyMDUwOFoXDTM2MDMw\n" +
            "NzIyMDUwOFowRDEUMBIGA1UEAwwLUEpCIFRlc3QgQ0ExDDAKBgNVBAoMA1BKQjER\n" +
            "MA8GA1UECwwIU2VjdXJpdHkxCzAJBgNVBAYTAkJSMIIBIjANBgkqhkiG9w0BAQEF\n" +
            "AAOCAQ8AMIIBCgKCAQEAnKbqumGK++pVHSS54BxN9Z7fWW5JRyzRzZVBywTNwrLH\n" +
            "CNOumApW7Q5dUSRb9k16AEy09ulMR+EUhW+jhje7vSu5ms0Y9Qwb083yRPIOBtTz\n" +
            "r+vy7DRgVxuPc/WWd6buUabBnBEpffQE7S3cJlICtx/sMYzhng7iYpwagr7jf7lr\n" +
            "W62gjrH9RytENQly7b1sZVSzZNAsN/G8BIk1grLWyABgC+fWzyqxMHci+qlRxXlc\n" +
            "dRBeL0ydUiun5vLV3YuWjxbeVS8GltwDPJne+qkfFaNJpItjKUQyr9Zefva2cVBq\n" +
            "as5ALCI9KSrLefwWRVFzpHUOdIetqicvb+c6ydLF5QIDAQABo1MwUTAdBgNVHQ4E\n" +
            "FgQUfGLB9F0/ZiWu6KYX59GKMyqPwPcwHwYDVR0jBBgwFoAUfGLB9F0/ZiWu6KYX\n" +
            "59GKMyqPwPcwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAdzta\n" +
            "HK/NigeUpikIhA25WqyiTXHxRqUggf5874k7c4AqJ9hMpqupa+UV7ECg7QjegImd\n" +
            "Gg/Q27OsoM6Dizn7aYcrAkZzRHtvPbcL9Yb1AHK9t8bMbWevOxp+kmWo0Xsi5Q1c\n" +
            "9YryyxpLGhM3REZMLPvUKb2tdulPqb8GYBMKUXCQGwtzlTVYuEXGmXpH5oM6Gky6\n" +
            "WgBxP0/intmnAXijkyKFVlqlsv+sxubFrfAOMUN9RA+TS9J0Hfm+OhqWQHIiGCni\n" +
            "AyhZ+s9L60KaFCsvhT5aScNsylPeulo46/DPwq8ofsaAs/u5UimSIPp+c8anELjJ\n" +
            "chfzVxEhSrL9iQyCaA==\n" +
            "-----END CERTIFICATE-----\n";

    @Test
    void validatesCertificateEntryAgainstTrustAnchor() throws Exception {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(CERTIFICATE.getBytes(StandardCharsets.UTF_8)));
        Path keyStorePath = Files.createTempFile("pjb-cert-validation", ".jks");
        char[] password = "changeit".toCharArray();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, password);
        keyStore.setCertificateEntry("tribunal-a3", certificate);
        try (var outputStream = Files.newOutputStream(keyStorePath)) {
            keyStore.store(outputStream, password);
        }

        JudicialConnectorSecurityProperties properties = new JudicialConnectorSecurityProperties();
        properties.setEnvironmentName("prod");
        properties.getCertificateValidation().setRequireClientAuthExtendedKeyUsage(false);
        properties.getCertificateValidation().setRequireDigitalSignatureKeyUsage(false);
        JudicialConnectorSecurityProperties.KeyStoreSource keyStoreSource = new JudicialConnectorSecurityProperties.KeyStoreSource();
        keyStoreSource.setType("JKS");
        keyStoreSource.setLocation(keyStorePath.toString());
        keyStoreSource.setPassword("literal:changeit");
        keyStoreSource.setAlias("tribunal-a3");
        properties.getKeyStores().put("client-cert", keyStoreSource);
        JudicialConnectorSecurityProperties.TrustStoreSource trustStoreSource = new JudicialConnectorSecurityProperties.TrustStoreSource();
        trustStoreSource.setType("JKS");
        trustStoreSource.setLocation(keyStorePath.toString());
        trustStoreSource.setPassword("literal:changeit");
        properties.getTrustStores().put("judicial-ca", trustStoreSource);

        JudicialPropertySecretResolver secretResolver = new JudicialPropertySecretResolver(new MockEnvironment(), new DefaultResourceLoader());
        JudicialIntegrationProperties integrationProperties = new JudicialIntegrationProperties();
        JudicialConnectorSecurityPackService packService = new JudicialConnectorSecurityPackService(properties, integrationProperties);
        JudicialConnectorCertificateValidationService service = new JudicialConnectorCertificateValidationService(
                properties,
                packService,
                mock(JudicialConnectorCryptographicContextService.class),
                new JudicialKeyStoreLoader(properties, new JudicialPkcs11ProviderRegistry(properties, secretResolver), secretResolver, new DefaultResourceLoader()),
                mock(JudicialConnectorLowLevelSecurityAuditService.class),
                mock(JudicialConnectorSecurityTelemetryService.class),
                Clock.fixed(Instant.parse("2026-03-10T22:10:00Z"), ZoneOffset.UTC)
        );

        JudicialResolvedSecurityBinding binding = new JudicialResolvedSecurityBinding(
                "test-binding",
                JudicialSystem.PJE,
                "TJCE",
                "prod",
                true,
                JudicialConnectorTlsMode.MTLS,
                "client-cert",
                "judicial-ca",
                "tribunal-a3",
                "tribunal-a3",
                true,
                true,
                null,
                null,
                java.util.List.of("TLSv1.3"),
                java.util.List.of(),
                java.util.List.of(),
                Map.of()
        );

        JudicialCertificateValidationReport report = service.validate(JudicialSystem.PJE, "TJCE", binding, Map.of("requestId", "REQ-1"));

        assertThat(report.certificatePresent()).isTrue();
        assertThat(report.pathValidationSucceeded()).isTrue();
        assertThat(report.status()).isEqualTo("VALID");
        assertThat(report.sha256Fingerprint()).hasSize(64);
        assertThat(report.keyAlias()).isEqualTo("tribunal-a3");
    }
}
