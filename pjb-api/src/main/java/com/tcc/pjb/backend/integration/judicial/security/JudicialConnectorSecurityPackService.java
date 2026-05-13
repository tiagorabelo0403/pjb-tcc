package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorSecurityPackService {

    private final JudicialConnectorSecurityProperties properties;
    private final JudicialIntegrationProperties integrationProperties;

    public JudicialConnectorSecurityPackService(JudicialConnectorSecurityProperties properties,
                                                JudicialIntegrationProperties integrationProperties) {
        this.properties = Objects.requireNonNull(properties);
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
    }

    public JudicialResolvedSecurityPack resolve(JudicialSystem system,
                                                String tribunalCodigo,
                                                URI targetUri,
                                                JudicialIntegrationProperties.Connector connectorConfig,
                                                Map<String, Object> metadata) {
        JudicialSystem resolvedSystem = system == null ? JudicialSystem.OUTRO : system;
        String normalizedTribunal = normalizeCode(tribunalCodigo);
        String environmentName = firstNonBlank(text(metadata != null ? metadata.get("connectorEnvironment") : null), properties.getEnvironmentName());
        Candidate selected = properties.getPacks().entrySet().stream()
                .map(entry -> toCandidate(entry.getKey(), entry.getValue(), resolvedSystem, normalizedTribunal, environmentName))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Candidate::specificity).reversed().thenComparing(Candidate::packId))
                .findFirst()
                .orElse(null);
        JudicialConnectorSecurityProperties.SecurityPack pack = selected != null ? selected.pack() : null;
        JudicialConnectorSecurityProperties.CertificateValidation defaults = properties.getCertificateValidation();
        boolean enabled = pack == null || pack.isEnabled();
        JudicialConnectorTlsMode tlsMode = pack != null && pack.getTlsMode() != null
                ? pack.getTlsMode()
                : connectorConfig != null && connectorConfig.isRequiresCertificate() ? JudicialConnectorTlsMode.MTLS : JudicialConnectorTlsMode.TLS;
        String keyStoreRef = firstNonBlank(text(metadata != null ? metadata.get("connectorKeyStoreRef") : null), pack != null ? pack.getKeyStoreRef() : null);
        String trustStoreRef = firstNonBlank(text(metadata != null ? metadata.get("connectorTrustStoreRef") : null), pack != null ? pack.getTrustStoreRef() : null);
        String keyAlias = firstNonBlank(text(metadata != null ? metadata.get("connectorKeyAlias") : null), pack != null ? pack.getKeyAlias() : null, connectorConfig != null ? connectorConfig.getCertificateAlias() : null);
        boolean requireClientCertificate = booleanValue(text(metadata != null ? metadata.get("connectorRequireClientCertificate") : null), pack != null && pack.getRequireClientCertificate() != null ? pack.getRequireClientCertificate() : connectorConfig != null && connectorConfig.isRequiresCertificate());
        boolean hostnameVerification = booleanValue(text(metadata != null ? metadata.get("connectorHostnameVerification") : null), pack != null && pack.getHostnameVerification() != null ? pack.getHostnameVerification() : properties.isDefaultHostnameVerification());
        Duration connectTimeout = firstNonNull(duration(text(metadata != null ? metadata.get("connectorConnectTimeoutMillis") : null)), pack != null ? pack.getConnectTimeout() : null, properties.getDefaultConnectTimeout());
        Duration readTimeout = firstNonNull(duration(text(metadata != null ? metadata.get("connectorReadTimeoutMillis") : null)), pack != null ? pack.getReadTimeout() : null, properties.getDefaultReadTimeout());
        List<String> protocols = takeStrings(text(metadata != null ? metadata.get("connectorTlsProtocols") : null), pack != null ? pack.getProtocols() : null, properties.getDefaultProtocols());
        List<String> cipherSuites = takeStrings(text(metadata != null ? metadata.get("connectorTlsCipherSuites") : null), pack != null ? pack.getCipherSuites() : null, properties.getDefaultCipherSuites());
        List<String> allowedHosts = pack != null ? clean(pack.getAllowedHosts()) : List.of();
        String targetHost = targetUri != null ? normalizeHost(targetUri.getHost()) : null;
        if (!allowedHosts.isEmpty() && targetHost != null && allowedHosts.stream().noneMatch(entry -> matchesHost(targetHost, entry))) {
            throw new JudicialConnectorCryptographicException("Target host is not allowed by judicial security pack.");
        }
        JudicialCertificateRevocationMode revocationMode = pack != null && pack.getRevocationMode() != null ? pack.getRevocationMode() : defaults.getRevocationMode();
        boolean ocspEnabled = pack != null && pack.getOcspEnabled() != null ? pack.getOcspEnabled() : defaults.isOcspEnabled();
        boolean crlEnabled = pack != null && pack.getCrlEnabled() != null ? pack.getCrlEnabled() : defaults.isCrlEnabled();
        boolean preferCrl = pack != null && pack.getPreferCrl() != null ? pack.getPreferCrl() : defaults.isPreferCrl();
        Duration minimumRemainingValidity = firstNonNull(pack != null ? pack.getMinimumRemainingValidity() : null, defaults.getMinimumRemainingValidity());
        Duration allowedClockSkew = firstNonNull(pack != null ? pack.getAllowedClockSkew() : null, defaults.getAllowedClockSkew());
        boolean requireDigitalSignatureKeyUsage = pack != null && pack.getRequireDigitalSignatureKeyUsage() != null ? pack.getRequireDigitalSignatureKeyUsage() : defaults.isRequireDigitalSignatureKeyUsage();
        boolean requireClientAuthExtendedKeyUsage = pack != null && pack.getRequireClientAuthExtendedKeyUsage() != null ? pack.getRequireClientAuthExtendedKeyUsage() : defaults.isRequireClientAuthExtendedKeyUsage();
        boolean requireTrustStoreForPathValidation = pack != null && pack.getRequireTrustStoreForPathValidation() != null ? pack.getRequireTrustStoreForPathValidation() : defaults.isRequireTrustStoreForPathValidation();
        LinkedHashMap<String, Object> resolvedMetadata = new LinkedHashMap<>();
        resolvedMetadata.put("targetHost", targetHost);
        resolvedMetadata.put("packSelectionMode", selected != null ? "PROPERTY_PACK" : "DEFAULT_POLICY");
        resolvedMetadata.put("connectorBaseUrl", connectorConfig != null ? connectorConfig.getBaseUrl() : null);
        resolvedMetadata.put("requiresCertificate", connectorConfig != null && connectorConfig.isRequiresCertificate());
        resolvedMetadata.put("packSpecificity", selected != null ? selected.specificity() : 0);
        return new JudicialResolvedSecurityPack(
                selected != null ? selected.packId() : "DEFAULT",
                resolvedSystem,
                normalizedTribunal,
                environmentName,
                enabled,
                tlsMode,
                keyStoreRef,
                trustStoreRef,
                keyAlias,
                requireClientCertificate,
                hostnameVerification,
                connectTimeout,
                readTimeout,
                protocols,
                cipherSuites,
                allowedHosts,
                revocationMode,
                ocspEnabled,
                crlEnabled,
                preferCrl,
                minimumRemainingValidity,
                allowedClockSkew,
                requireDigitalSignatureKeyUsage,
                requireClientAuthExtendedKeyUsage,
                requireTrustStoreForPathValidation,
                Map.copyOf(cleanMap(resolvedMetadata))
        );
    }

    public List<JudicialConnectorSecurityPackReport> effectivePacks() {
        List<JudicialConnectorSecurityPackReport> reports = new ArrayList<>();
        for (Target target : targets()) {
            reports.add(effectivePack(target.system(), target.tribunalCodigo()));
        }
        return List.copyOf(reports);
    }

    public JudicialConnectorSecurityPackReport effectivePack(JudicialSystem system, String tribunalCodigo) {
        JudicialIntegrationProperties.Connector connector = integrationProperties.connectorFor(system == null ? JudicialSystem.OUTRO : system);
        URI targetUri = connector != null && connector.getBaseUrl() != null && !connector.getBaseUrl().isBlank() ? URI.create(connector.getBaseUrl()) : null;
        JudicialResolvedSecurityPack pack = resolve(system, tribunalCodigo, targetUri, connector, Map.of());
        return toReport(pack);
    }

    public JudicialConnectorSecurityPackSummary summary() {
        List<JudicialConnectorSecurityPackReport> packs = effectivePacks();
        int mutualTlsPacks = 0;
        int hostnameVerifiedPacks = 0;
        int revocationEnforcedPacks = 0;
        int tribunalScopedPacks = 0;
        int hardwareBoundKeyStoreReferences = 0;
        ArrayList<Map<String, Object>> targets = new ArrayList<>();
        for (JudicialConnectorSecurityPackReport pack : packs) {
            if (pack.tlsMode() == JudicialConnectorTlsMode.MTLS) {
                mutualTlsPacks++;
            }
            if (pack.hostnameVerification()) {
                hostnameVerifiedPacks++;
            }
            if (pack.revocationMode() != null && pack.revocationMode() != JudicialCertificateRevocationMode.DISABLED) {
                revocationEnforcedPacks++;
            }
            if (pack.tribunalCodigo() != null) {
                tribunalScopedPacks++;
            }
            if (pack.keyStoreRef() != null && pack.keyStoreRef().toUpperCase(Locale.ROOT).contains("PKCS11")) {
                hardwareBoundKeyStoreReferences++;
            }
            targets.add(JudicialMapSupport.compact(
                    "system", pack.system() != null ? pack.system().name() : null,
                    "tribunalCodigo", pack.tribunalCodigo(),
                    "packId", pack.packId(),
                    "tlsMode", pack.tlsMode() != null ? pack.tlsMode().name() : null,
                    "keyStoreRef", pack.keyStoreRef()
            ));
        }
        return new JudicialConnectorSecurityPackSummary(
                Instant.now(),
                packs.size(),
                mutualTlsPacks,
                hostnameVerifiedPacks,
                revocationEnforcedPacks,
                tribunalScopedPacks,
                hardwareBoundKeyStoreReferences,
                List.copyOf(targets),
                Map.of("configuredPackCount", properties.getPacks().size(), "environmentName", properties.getEnvironmentName())
        );
    }

    private List<Target> targets() {
        LinkedHashSet<Target> targets = new LinkedHashSet<>();
        properties.getPacks().values().forEach(pack -> {
            JudicialSystem system = parseSystem(pack.getSystem());
            if (system != null && pack.isEnabled()) {
                targets.add(new Target(system, normalizeCode(pack.getTribunalCodigo())));
            }
        });
        properties.getBindings().values().forEach(binding -> {
            JudicialSystem system = parseSystem(binding.getSystem());
            if (system != null && binding.isEnabled()) {
                targets.add(new Target(system, normalizeCode(binding.getTribunalCodigo())));
            }
        });
        for (JudicialSystem system : JudicialSystem.values()) {
            if (system == JudicialSystem.OUTRO) {
                continue;
            }
            JudicialIntegrationProperties.Connector connector = integrationProperties.connectorFor(system);
            if (connector != null && connector.isEnabled()) {
                targets.add(new Target(system, null));
            }
        }
        return List.copyOf(targets);
    }

    private Candidate toCandidate(String packId,
                                  JudicialConnectorSecurityProperties.SecurityPack pack,
                                  JudicialSystem system,
                                  String tribunalCodigo,
                                  String environmentName) {
        if (pack == null || !pack.isEnabled()) {
            return null;
        }
        String packSystem = normalizeCode(pack.getSystem());
        String packTribunal = normalizeCode(pack.getTribunalCodigo());
        String packEnvironment = normalizeEnvironment(pack.getEnvironmentName());
        if (packSystem != null && !packSystem.equalsIgnoreCase(system.name())) {
            return null;
        }
        if (packTribunal != null && !Objects.equals(packTribunal, tribunalCodigo)) {
            return null;
        }
        if (packEnvironment != null && !packEnvironment.equalsIgnoreCase(environmentName)) {
            return null;
        }
        int specificity = 0;
        if (packSystem != null) {
            specificity += 8;
        }
        if (packTribunal != null) {
            specificity += 4;
        }
        if (packEnvironment != null) {
            specificity += 2;
        }
        if (pack.getTlsMode() == JudicialConnectorTlsMode.MTLS) {
            specificity += 1;
        }
        return new Candidate(packId, pack, specificity);
    }

    private JudicialConnectorSecurityPackReport toReport(JudicialResolvedSecurityPack pack) {
        return new JudicialConnectorSecurityPackReport(
                Instant.now(),
                pack.packId(),
                pack.system(),
                pack.tribunalCodigo(),
                pack.environmentName(),
                pack.enabled(),
                pack.tlsMode(),
                pack.keyStoreRef(),
                pack.trustStoreRef(),
                pack.keyAlias(),
                pack.requireClientCertificate(),
                pack.hostnameVerification(),
                pack.connectTimeout(),
                pack.readTimeout(),
                pack.protocols(),
                pack.cipherSuites(),
                pack.allowedHosts(),
                pack.revocationMode(),
                pack.ocspEnabled(),
                pack.crlEnabled(),
                pack.preferCrl(),
                pack.minimumRemainingValidity(),
                pack.allowedClockSkew(),
                pack.requireDigitalSignatureKeyUsage(),
                pack.requireClientAuthExtendedKeyUsage(),
                pack.requireTrustStoreForPathValidation(),
                pack.metadata()
        );
    }

    private boolean matchesHost(String host, String allowedHost) {
        String normalizedAllowed = normalizeHost(allowedHost);
        if (normalizedAllowed == null) {
            return false;
        }
        if (normalizedAllowed.startsWith("*.")) {
            return host.endsWith(normalizedAllowed.substring(1));
        }
        return normalizedAllowed.equalsIgnoreCase(host);
    }

    private List<String> takeStrings(String inline, List<String> preferred, List<String> fallback) {
        List<String> parsedInline = inline == null ? List.of() : clean(List.of(inline.split(",")));
        if (!parsedInline.isEmpty()) {
            return parsedInline;
        }
        List<String> parsedPreferred = clean(preferred);
        if (!parsedPreferred.isEmpty()) {
            return parsedPreferred;
        }
        return clean(fallback);
    }

    private List<String> clean(List<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (Object value : values) {
            String normalized = text(value);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    private boolean booleanValue(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private Duration duration(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Duration.ofMillis(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
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

    private JudicialSystem parseSystem(String value) {
        String normalized = normalizeCode(value);
        if (normalized == null) {
            return null;
        }
        try {
            return JudicialSystem.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, Object> cleanMap(Map<String, Object> metadata) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (metadata != null) {
            out.putAll(metadata);
            out.entrySet().removeIf(entry -> entry.getValue() == null);
        }
        return Map.copyOf(out);
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeEnvironment(String value) {
        String text = text(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        String text = text(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    private String normalizeHost(String value) {
        String text = text(value);
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record Candidate(String packId, JudicialConnectorSecurityProperties.SecurityPack pack, int specificity) {
    }

    private record Target(JudicialSystem system, String tribunalCodigo) {
    }
}
