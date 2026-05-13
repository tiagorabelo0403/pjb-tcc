package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorCryptographicContextService {

    private final JudicialConnectorSecurityProperties properties;
    private final JudicialConnectorSecurityPackService securityPackService;
    private final JudicialKeyStoreLoader keyStoreLoader;
    private final JudicialConnectorLowLevelSecurityAuditService auditService;

    public JudicialConnectorCryptographicContextService(JudicialConnectorSecurityProperties properties,
                                                        JudicialConnectorSecurityPackService securityPackService,
                                                        JudicialKeyStoreLoader keyStoreLoader,
                                                        JudicialConnectorLowLevelSecurityAuditService auditService) {
        this.properties = Objects.requireNonNull(properties);
        this.securityPackService = Objects.requireNonNull(securityPackService);
        this.keyStoreLoader = Objects.requireNonNull(keyStoreLoader);
        this.auditService = Objects.requireNonNull(auditService);
    }

    public JudicialResolvedSecurityBinding resolveBinding(JudicialSystem system,
                                                          String tribunalCodigo,
                                                          URI targetUri,
                                                          JudicialIntegrationProperties.Connector connectorConfig,
                                                          Map<String, Object> metadata) {
        String environmentName = firstNonBlank(text(metadata != null ? metadata.get("connectorEnvironment") : null), properties.getEnvironmentName());
        String tribunal = normalized(tribunalCodigo);
        String host = targetUri != null ? normalized(targetUri.getHost()) : null;
        JudicialResolvedSecurityPack securityPack = securityPackService.resolve(system, tribunalCodigo, targetUri, connectorConfig, metadata);
        List<Candidate> candidates = properties.getBindings().entrySet().stream()
                .map(entry -> toCandidate(entry.getKey(), entry.getValue(), system, tribunal, environmentName))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Candidate::specificity).reversed().thenComparing(Candidate::bindingId))
                .toList();
        JudicialConnectorSecurityProperties.ConnectorBinding chosen = candidates.isEmpty() ? null : candidates.getFirst().binding();
        String bindingId = candidates.isEmpty() ? "DEFAULT" : candidates.getFirst().bindingId();
        JudicialConnectorTlsMode tlsMode = parseTlsMode(metadata != null ? metadata.get("connectorTlsMode") : null);
        if (tlsMode == null) {
            if (chosen != null && chosen.getTlsMode() != null) {
                tlsMode = chosen.getTlsMode();
            } else if (securityPack.tlsMode() != null) {
                tlsMode = securityPack.tlsMode();
            } else if (targetUri != null && "https".equalsIgnoreCase(targetUri.getScheme())) {
                tlsMode = JudicialConnectorTlsMode.TLS;
            } else {
                tlsMode = JudicialConnectorTlsMode.DISABLED;
            }
        }
        List<String> protocols = nonEmptyStringList(metadata != null ? metadata.get("connectorTlsProtocols") : null);
        if (protocols.isEmpty()) {
            protocols = chosen != null && chosen.getProtocols() != null && !chosen.getProtocols().isEmpty() ? clean(chosen.getProtocols()) : securityPack.protocols();
        }
        List<String> cipherSuites = nonEmptyStringList(metadata != null ? metadata.get("connectorTlsCipherSuites") : null);
        if (cipherSuites.isEmpty()) {
            cipherSuites = chosen != null && chosen.getCipherSuites() != null && !chosen.getCipherSuites().isEmpty() ? clean(chosen.getCipherSuites()) : securityPack.cipherSuites();
        }
        boolean hostnameVerification = booleanValue(metadata != null ? metadata.get("connectorHostnameVerification") : null, chosen != null ? chosen.isHostnameVerification() : securityPack.hostnameVerification());
        String keyStoreRef = firstNonBlank(text(metadata != null ? metadata.get("connectorKeyStoreRef") : null), chosen != null ? chosen.getKeyStoreRef() : null, securityPack.keyStoreRef());
        String trustStoreRef = firstNonBlank(text(metadata != null ? metadata.get("connectorTrustStoreRef") : null), chosen != null ? chosen.getTrustStoreRef() : null, securityPack.trustStoreRef());
        String keyAlias = firstNonBlank(
                text(metadata != null ? metadata.get("connectorKeyAlias") : null),
                text(metadata != null ? metadata.get("connectorCertificateAlias") : null),
                chosen != null ? chosen.getKeyAlias() : null,
                chosen != null ? chosen.getCertificateAlias() : null,
                securityPack.keyAlias(),
                connectorConfig != null ? connectorConfig.getCertificateAlias() : null
        );
        Duration connectTimeout = firstNonNull(duration(metadata != null ? metadata.get("connectorConnectTimeoutMillis") : null), chosen != null ? chosen.getConnectTimeout() : null, securityPack.connectTimeout(), properties.getDefaultConnectTimeout());
        Duration readTimeout = firstNonNull(duration(metadata != null ? metadata.get("connectorReadTimeoutMillis") : null), chosen != null ? chosen.getReadTimeout() : null, securityPack.readTimeout(), properties.getDefaultReadTimeout());
        boolean requireClientCertificate = booleanValue(
                metadata != null ? metadata.get("connectorRequireClientCertificate") : null,
                chosen != null && chosen.isRequireClientCertificate() || securityPack.requireClientCertificate()
        );
        boolean enabled = booleanValue(metadata != null ? metadata.get("connectorSecurityEnabled") : null, (chosen == null || chosen.isEnabled()) && securityPack.enabled());
        List<String> allowedHosts = chosen != null && chosen.getAllowedHosts() != null && !chosen.getAllowedHosts().isEmpty() ? clean(chosen.getAllowedHosts()) : securityPack.allowedHosts();
        if (!allowedHosts.isEmpty() && host != null && !matchesHostPolicy(host, allowedHosts)) {
            throw new JudicialConnectorCryptographicException("Target host is not allowed by judicial connector security policy.");
        }
        LinkedHashMap<String, Object> bindingMetadata = new LinkedHashMap<>();
        bindingMetadata.put("targetHost", host);
        bindingMetadata.put("environmentName", environmentName);
        bindingMetadata.put("bindingSource", chosen != null ? "PROPERTY_BINDING" : "DEFAULT_BINDING");
        bindingMetadata.put("securityPackId", securityPack.packId());
        bindingMetadata.put("securityPackSelectionMode", securityPack.metadata().get("packSelectionMode"));
        bindingMetadata.put("connectorRequiresCertificate", connectorConfig != null && connectorConfig.isRequiresCertificate());
        bindingMetadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialResolvedSecurityBinding(
                bindingId,
                system,
                tribunal,
                environmentName,
                enabled,
                tlsMode,
                keyStoreRef,
                trustStoreRef,
                keyAlias,
                keyAlias,
                requireClientCertificate,
                hostnameVerification,
                connectTimeout,
                readTimeout,
                protocols,
                cipherSuites,
                allowedHosts,
                Map.copyOf(bindingMetadata)
        );
    }

    public JudicialConnectorCryptographicContext resolve(JudicialSystem system,
                                                         String tribunalCodigo,
                                                         URI targetUri,
                                                         JudicialIntegrationProperties.Connector connectorConfig,
                                                         Map<String, Object> metadata) {
        JudicialResolvedSecurityBinding binding = resolveBinding(system, tribunalCodigo, targetUri, connectorConfig, metadata);
        if (!binding.enabled()) {
            return new JudicialConnectorCryptographicContext(binding, null, null, null, false, Map.of("securityDisabled", true));
        }
        if (targetUri == null || normalized(targetUri.getScheme()) == null) {
            throw new JudicialConnectorCryptographicException("A target URI is required to resolve judicial connector cryptography.");
        }
        if ("http".equalsIgnoreCase(targetUri.getScheme())) {
            if (binding.transportSecurityEnabled()) {
                JudicialConnectorCryptographicException exception = new JudicialConnectorCryptographicException("Cleartext HTTP is forbidden for active judicial connector cryptography.");
                auditService.recordContextFailure(system, tribunalCodigo, targetUri, "SSL_CONTEXT_RESOLUTION", binding, exception, correlationId(metadata), Map.of("cleartextHttp", true));
                throw exception;
            }
            return new JudicialConnectorCryptographicContext(binding, null, null, null, false, Map.of("cleartextHttp", true));
        }
        if (!"https".equalsIgnoreCase(targetUri.getScheme())) {
            JudicialConnectorCryptographicException exception = new JudicialConnectorCryptographicException("Unsupported judicial connector transport scheme " + targetUri.getScheme() + '.');
            auditService.recordContextFailure(system, tribunalCodigo, targetUri, "SSL_CONTEXT_RESOLUTION", binding, exception, correlationId(metadata), Map.of("unsupportedScheme", targetUri.getScheme()));
            throw exception;
        }
        try {
            JudicialKeyStoreMaterial keyMaterial = binding.mutualTls() || binding.requireClientCertificate() || binding.keyStoreRef() != null ? keyStoreLoader.loadKeyStore(binding.keyStoreRef()) : null;
            JudicialKeyStoreMaterial trustMaterial = binding.trustStoreRef() != null ? keyStoreLoader.loadTrustStore(binding.trustStoreRef()) : null;
            KeyManager[] keyManagers = buildKeyManagers(keyMaterial, binding);
            TrustManager[] trustManagers = buildTrustManagers(trustMaterial);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, SecureRandom.getInstanceStrong());
            SSLParameters sslParameters = new SSLParameters();
            if (!binding.protocols().isEmpty()) {
                sslParameters.setProtocols(binding.protocols().toArray(String[]::new));
            }
            if (!binding.cipherSuites().isEmpty()) {
                sslParameters.setCipherSuites(binding.cipherSuites().toArray(String[]::new));
            }
            if (binding.hostnameVerification()) {
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            }
            LinkedHashMap<String, Object> contextMetadata = new LinkedHashMap<>();
            contextMetadata.put("system", system != null ? system.name() : null);
            contextMetadata.put("tribunalCodigo", tribunalCodigo);
            contextMetadata.put("targetHost", normalized(targetUri.getHost()));
            contextMetadata.put("keyStoreRef", binding.keyStoreRef());
            contextMetadata.put("trustStoreRef", binding.trustStoreRef());
            contextMetadata.put("tlsMode", binding.tlsMode().name());
            contextMetadata.put("protocols", binding.protocols());
            contextMetadata.put("cipherSuites", binding.cipherSuites());
            contextMetadata.put("hardwareBacked", keyMaterial != null && keyMaterial.hardwareBacked());
            contextMetadata.entrySet().removeIf(entry -> entry.getValue() == null);
            return new JudicialConnectorCryptographicContext(
                    binding,
                    sslContext,
                    sslParameters,
                    binding.keyAlias(),
                    keyMaterial != null && keyMaterial.hardwareBacked(),
                    Map.copyOf(contextMetadata)
            );
        } catch (Exception ex) {
            auditService.recordContextFailure(system, tribunalCodigo, targetUri, "SSL_CONTEXT_RESOLUTION", binding, ex, correlationId(metadata), Map.of("bindingId", binding.bindingId()));
            throw new JudicialConnectorCryptographicException("Unable to resolve SSLContext for judicial connector " + (system != null ? system.name() : "OUTRO") + '.', ex);
        }
    }

    private KeyManager[] buildKeyManagers(JudicialKeyStoreMaterial keyMaterial,
                                          JudicialResolvedSecurityBinding binding) throws Exception {
        if (!binding.mutualTls() && !binding.requireClientCertificate() && keyMaterial == null) {
            return null;
        }
        if (keyMaterial == null) {
            throw new JudicialConnectorCryptographicException("Mutual TLS requires a KeyStore binding.");
        }
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] keyPassword = firstNonNull(keyMaterial.keyPasswordCopy(), keyMaterial.storePasswordCopy());
        try {
            factory.init(keyMaterial.keyStore(), keyPassword);
        } finally {
            clear(keyPassword);
        }
        KeyManager[] managers = factory.getKeyManagers();
        String alias = firstNonBlank(binding.keyAlias(), keyMaterial.preferredAlias());
        if (alias == null) {
            return managers;
        }
        KeyManager[] wrapped = new KeyManager[managers.length];
        for (int i = 0; i < managers.length; i++) {
            KeyManager manager = managers[i];
            if (manager instanceof X509ExtendedKeyManager x509ExtendedKeyManager) {
                wrapped[i] = new AliasedX509ExtendedKeyManager(x509ExtendedKeyManager, alias);
            } else {
                wrapped[i] = manager;
            }
        }
        return wrapped;
    }

    private TrustManager[] buildTrustManagers(JudicialKeyStoreMaterial trustMaterial) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustMaterial != null ? trustMaterial.keyStore() : null);
        return factory.getTrustManagers();
    }

    private Candidate toCandidate(String bindingId,
                                  JudicialConnectorSecurityProperties.ConnectorBinding binding,
                                  JudicialSystem system,
                                  String tribunalCodigo,
                                  String environmentName) {
        if (binding == null) {
            return null;
        }
        String bindingSystem = normalized(binding.getSystem());
        String bindingTribunal = normalized(binding.getTribunalCodigo());
        String bindingEnvironment = normalized(binding.getEnvironmentName());
        if (bindingSystem != null && (system == null || !bindingSystem.equalsIgnoreCase(system.name()))) {
            return null;
        }
        if (bindingTribunal != null && !bindingTribunal.equalsIgnoreCase(tribunalCodigo)) {
            return null;
        }
        if (bindingEnvironment != null && !bindingEnvironment.equalsIgnoreCase(environmentName)) {
            return null;
        }
        int specificity = 0;
        if (bindingSystem != null) {
            specificity += 8;
        }
        if (bindingTribunal != null) {
            specificity += 4;
        }
        if (bindingEnvironment != null) {
            specificity += 2;
        }
        if (binding.isRequireClientCertificate()) {
            specificity += 1;
        }
        return new Candidate(bindingId, binding, specificity);
    }

    private boolean matchesHostPolicy(String host, List<String> allowedHosts) {
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            return true;
        }
        String normalizedHost = normalized(host);
        if (normalizedHost == null) {
            return false;
        }
        for (String allowedHost : allowedHosts) {
            String candidate = normalized(allowedHost);
            if (candidate == null) {
                continue;
            }
            if (candidate.startsWith("*.")) {
                String suffix = candidate.substring(1).toLowerCase();
                if (normalizedHost.toLowerCase().endsWith(suffix)) {
                    return true;
                }
            } else if (candidate.equalsIgnoreCase(normalizedHost)) {
                return true;
            }
        }
        return false;
    }

    private Duration duration(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        try {
            return Duration.ofMillis(Long.parseLong(text));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private JudicialConnectorTlsMode parseTlsMode(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        try {
            return JudicialConnectorTlsMode.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<String> nonEmptyStringList(Object value) {
        return switch (value) {
            case null -> List.of();
            case String text -> clean(List.of(text.split(",")));
            case Iterable<?> iterable -> {
                ArrayList<String> items = new ArrayList<>();
                iterable.forEach(item -> {
                    String normalized = normalized(String.valueOf(item));
                    if (normalized != null) {
                        items.add(normalized);
                    }
                });
                yield List.copyOf(items);
            }
            default -> {
                String normalized = normalized(String.valueOf(value));
                yield normalized == null ? List.of() : List.of(normalized);
            }
        };
    }

    private List<String> clean(List<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (Object value : values) {
            String normalized = normalized(String.valueOf(value));
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    private boolean booleanValue(Object value, boolean fallback) {
        String text = text(value);
        return text == null ? fallback : Boolean.parseBoolean(text);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
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

    private void clear(char[] value) {
        if (value != null) {
            java.util.Arrays.fill(value, '\0');
        }
    }

    private String correlationId(Map<String, Object> metadata) {
        return firstNonBlank(text(metadata != null ? metadata.get("idempotencyKey") : null), text(metadata != null ? metadata.get("requestId") : null));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (normalized(value) != null) {
                return value.trim();
            }
        }
        return null;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        return normalized(String.valueOf(value));
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out;
    }

    private record Candidate(String bindingId,
                             JudicialConnectorSecurityProperties.ConnectorBinding binding,
                             int specificity) {
    }
}
