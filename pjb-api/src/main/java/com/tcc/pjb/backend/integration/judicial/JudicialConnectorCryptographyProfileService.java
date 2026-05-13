package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorCryptographyProfileService {

    private final JudicialConnectorRegistry registry;
    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorOperationalProfileService operationalProfileService;

    public JudicialConnectorCryptographyProfileService(JudicialConnectorRegistry registry,
                                                       JudicialIntegrationProperties integrationProperties,
                                                       JudicialConnectorOperationalProfileService operationalProfileService) {
        this.registry = Objects.requireNonNull(registry);
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
    }

    public JudicialConnectorCryptographyReport nationalReport() {
        return buildReport(null);
    }

    public JudicialConnectorCryptographyReport tribunalReport(String tribunalCodigo) {
        return buildReport(normalizeCode(tribunalCodigo));
    }

    private JudicialConnectorCryptographyReport buildReport(String tribunalCodigo) {
        List<JudicialConnectorCryptographySystemReport> systems = Arrays.stream(JudicialSystem.values())
                .map(system -> buildSystemReport(system, tribunalCodigo))
                .sorted(Comparator
                        .comparing(JudicialConnectorCryptographySystemReport::strongAuthentication).reversed()
                        .thenComparing(JudicialConnectorCryptographySystemReport::certificateSatisfied).reversed()
                        .thenComparing(item -> item.system() != null ? item.system().name() : "ZZZ"))
                .toList();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        systems.forEach(item -> {
            if (item.blockers() != null) {
                blockers.addAll(item.blockers());
            }
            if (item.warnings() != null) {
                warnings.addAll(item.warnings());
            }
        });
        long strongCount = systems.stream().filter(JudicialConnectorCryptographySystemReport::strongAuthentication).count();
        long certificateCount = systems.stream().filter(JudicialConnectorCryptographySystemReport::certificateSatisfied).count();
        long blockedCount = systems.stream().filter(item -> "BLOCKED".equals(item.cryptographyStatus())).count();
        if (tribunalCodigo != null && strongCount == 0L) {
            warnings.add("CRYPTOGRAPHY_NO_STRONG_AUTH_CONNECTOR_FOR_TRIBUNAL");
        }
        if (certificateCount == 0L) {
            warnings.add("CRYPTOGRAPHY_NO_CERTIFICATE_READY_CONNECTOR");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mode", tribunalCodigo == null ? "NATIONAL" : "TRIBUNAL");
        metadata.put("declaredSystems", Arrays.stream(JudicialSystem.values()).map(Enum::name).sorted().toList());
        metadata.put("registeredSystems", registry.all().stream().map(JudicialProcessConnector::system).filter(Objects::nonNull).map(Enum::name).sorted().toList());
        return new JudicialConnectorCryptographyReport(
                Instant.now(),
                tribunalCodigo,
                (int) strongCount,
                (int) certificateCount,
                (int) blockedCount,
                systems,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private JudicialConnectorCryptographySystemReport buildSystemReport(JudicialSystem system, String tribunalCodigo) {
        JudicialIntegrationProperties.Connector cfg = integrationProperties.connectorFor(system);
        String effectiveTribunal = firstNonBlank(tribunalCodigo, defaultTribunal(cfg));
        ProtocolSubmissionRequest probe = new ProtocolSubmissionRequest(
                "CRYPTO-" + (system != null ? system.name() : JudicialSystem.OUTRO.name()) + '-' + firstNonBlank(effectiveTribunal, "DEFAULT"),
                null,
                "Cryptography Probe",
                effectiveTribunal,
                null,
                null,
                null,
                null,
                null,
                "{}",
                null,
                null,
                null,
                true,
                Map.of("probe", true, "plane", "crypto")
        );
        JudicialConnectorOperationalProfileReport profile = operationalProfileService.analyze(system, probe);
        JudicialConnectorReadinessReport readiness = profile.readiness();
        Optional<JudicialProcessConnector> connector = registry.find(system);
        JudicialSubmissionCapability capability = connector.map(JudicialProcessConnector::capability).orElse(null);
        boolean certificateRequired = cfg != null && cfg.isRequiresCertificate() || capability != null && capability.requiresCertificate();
        String certificateAlias = firstNonBlank(
                stringValue(profile.metadata().get("certificateAlias")),
                profile.homologation() != null && profile.homologation().metadata() != null ? stringValue(profile.homologation().metadata().get("policyCertificateAlias")) : null,
                cfg != null ? cfg.getCertificateAlias() : null
        );
        boolean certificateConfigured = !certificateRequired || hasText(certificateAlias);
        boolean certificateSatisfied = readiness != null && readiness.certificateSatisfied();
        boolean authenticationSatisfied = readiness != null && readiness.authenticationSatisfied();
        JudicialConnectorAuthMode authMode = profile.authMode() != null ? profile.authMode() : JudicialConnectorAuthMode.NONE;
        boolean strongAuthentication = authenticationSatisfied && authMode != JudicialConnectorAuthMode.NONE && authMode != JudicialConnectorAuthMode.MISSING || certificateRequired && certificateSatisfied;
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (profile.blockers() != null) {
            blockers.addAll(profile.blockers());
        }
        if (profile.warnings() != null) {
            warnings.addAll(profile.warnings());
        }
        if (certificateRequired && !certificateConfigured) {
            blockers.add("CRYPTOGRAPHY_CERTIFICATE_ALIAS_UNCONFIGURED");
        }
        if (profile.connectorEnabled() && !authenticationSatisfied) {
            blockers.add("CRYPTOGRAPHY_AUTHENTICATION_UNSATISFIED");
        }
        if (profile.connectorEnabled() && !strongAuthentication) {
            warnings.add("CRYPTOGRAPHY_NOT_STRONG_AUTHENTICATED");
        }
        if (certificateRequired && !certificateSatisfied) {
            blockers.add("CRYPTOGRAPHY_CERTIFICATE_UNSATISFIED");
        }
        String cryptographyStatus = resolveStatus(profile, certificateRequired, certificateSatisfied, authenticationSatisfied, strongAuthentication, blockers, warnings);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseUrl", readiness != null && readiness.metadata() != null ? readiness.metadata().get("baseUrl") : capability != null ? capability.baseUrl() : null);
        metadata.put("requiresCertificate", certificateRequired);
        metadata.put("certificateAlias", certificateAlias);
        metadata.put("submitPath", readiness != null && readiness.metadata() != null ? readiness.metadata().get("submitPath") : null);
        metadata.put("tribunalCodigo", effectiveTribunal);
        metadata.put("oauthConfigured", authMode == JudicialConnectorAuthMode.OAUTH2_CLIENT_CREDENTIALS);
        metadata.put("authMode", authMode.name());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorCryptographySystemReport(
                Instant.now(),
                system,
                effectiveTribunal,
                cryptographyStatus,
                authMode,
                profile.connectorEnabled(),
                profile.readyForProduction(),
                profile.readyForTribunalSubmission(),
                certificateRequired,
                certificateConfigured,
                certificateSatisfied,
                authenticationSatisfied,
                strongAuthentication,
                certificateAlias,
                profile,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private String resolveStatus(JudicialConnectorOperationalProfileReport profile,
                                 boolean certificateRequired,
                                 boolean certificateSatisfied,
                                 boolean authenticationSatisfied,
                                 boolean strongAuthentication,
                                 LinkedHashSet<String> blockers,
                                 LinkedHashSet<String> warnings) {
        if (!blockers.isEmpty()) {
            return "BLOCKED";
        }
        if (!profile.connectorEnabled()) {
            return "DISABLED";
        }
        if (certificateRequired && certificateSatisfied && strongAuthentication) {
            return warnings.isEmpty() ? "CERTIFICATE_HARDENED" : "CERTIFICATE_HARDENED_WITH_WARNINGS";
        }
        if (strongAuthentication) {
            return warnings.isEmpty() ? "STRONG_AUTH_READY" : "STRONG_AUTH_READY_WITH_WARNINGS";
        }
        if (authenticationSatisfied) {
            return "AUTH_READY";
        }
        return "CONFIGURED_WITH_GAPS";
    }

    private String defaultTribunal(JudicialIntegrationProperties.Connector cfg) {
        List<String> homologated = normalizeCodes(cfg != null ? cfg.getHomologatedTribunals() : List.of());
        if (!homologated.isEmpty()) {
            return homologated.getFirst();
        }
        List<String> blocked = normalizeCodes(cfg != null ? cfg.getBlockedTribunals() : List.of());
        if (!blocked.isEmpty()) {
            return blocked.getFirst();
        }
        return null;
    }

    private List<String> normalizeCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeCode(value);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return new ArrayList<>(out);
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
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

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? null : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
