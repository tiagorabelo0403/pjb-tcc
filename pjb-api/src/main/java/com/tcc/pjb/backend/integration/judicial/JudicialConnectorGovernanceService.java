package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorGovernanceService {

    private final JudicialConnectorRegistry registry;
    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorOperationalProfileService operationalProfileService;

    public JudicialConnectorGovernanceService(JudicialConnectorRegistry registry,
                                              JudicialIntegrationProperties integrationProperties,
                                              JudicialConnectorOperationalProfileService operationalProfileService) {
        this.registry = Objects.requireNonNull(registry);
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
    }

    public JudicialConnectorGovernanceReport report() {
        List<JudicialConnectorGovernanceItem> connectors = new ArrayList<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        int enabledCount = 0;
        int operationalCount = 0;
        for (JudicialSystem system : JudicialSystem.values()) {
            connectors.add(buildItem(system, blockers, warnings));
        }
        connectors.sort(Comparator.comparing(item -> item.system() != null ? item.system().name() : "ZZZ"));
        for (JudicialConnectorGovernanceItem item : connectors) {
            if (item.configuredEnabled()) {
                enabledCount++;
            }
            if (item.operational()) {
                operationalCount++;
            }
        }
        boolean contingencyConnectorPresent = registry.find(JudicialSystem.OUTRO).isPresent();
        if (!contingencyConnectorPresent) {
            blockers.add("CONTINGENCY_CONNECTOR_OUTRO_NOT_REGISTERED");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("registeredSystems", registry.all().stream().map(JudicialProcessConnector::system).filter(Objects::nonNull).map(Enum::name).sorted().toList());
        metadata.put("declaredSystems", java.util.Arrays.stream(JudicialSystem.values()).map(Enum::name).sorted().toList());
        metadata.put("configuredSystems", connectors.stream().filter(JudicialConnectorGovernanceItem::configuredEnabled).map(item -> item.system().name()).toList());
        metadata.put("productionReadySystems", connectors.stream().filter(JudicialConnectorGovernanceItem::productionReady).map(item -> item.system().name()).toList());
        metadata.put("operationalSystems", connectors.stream().filter(JudicialConnectorGovernanceItem::operational).map(item -> item.system().name()).toList());
        return new JudicialConnectorGovernanceReport(
                Instant.now(),
                registry.all().size(),
                enabledCount,
                operationalCount,
                contingencyConnectorPresent,
                List.copyOf(connectors),
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    public JudicialConnectorTribunalLandscapeReport reportForTribunal(String tribunalCodigo) {
        String normalizedTribunal = normalizeCode(tribunalCodigo);
        List<JudicialConnectorOperationalProfileReport> profiles = java.util.Arrays.stream(JudicialSystem.values())
                .map(system -> operationalProfileService.analyze(system, probeRequest(system, normalizedTribunal)))
                .sorted(Comparator
                        .comparing(JudicialConnectorOperationalProfileReport::readyForProduction).reversed()
                        .thenComparing(JudicialConnectorOperationalProfileReport::readyForTribunalSubmission).reversed()
                        .thenComparing(item -> item.system() != null ? item.system().name() : "ZZZ"))
                .toList();
        List<String> readySystems = profiles.stream()
                .filter(JudicialConnectorOperationalProfileReport::readyForTribunalSubmission)
                .map(item -> item.system() != null ? item.system().name() : JudicialSystem.OUTRO.name())
                .toList();
        List<String> productionReadySystems = profiles.stream()
                .filter(JudicialConnectorOperationalProfileReport::readyForProduction)
                .map(item -> item.system() != null ? item.system().name() : JudicialSystem.OUTRO.name())
                .toList();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (profiles.stream().noneMatch(JudicialConnectorOperationalProfileReport::readyForTribunalSubmission)) {
            warnings.add("NO_SUBMISSION_READY_CONNECTOR_FOR_TRIBUNAL");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", normalizedTribunal);
        metadata.put("profileCount", profiles.size());
        metadata.put("registeredSystems", registry.all().stream().map(JudicialProcessConnector::system).filter(Objects::nonNull).map(Enum::name).sorted().toList());
        metadata.put("declaredSystems", java.util.Arrays.stream(JudicialSystem.values()).map(Enum::name).sorted().toList());
        return new JudicialConnectorTribunalLandscapeReport(
                Instant.now(),
                normalizedTribunal,
                readySystems,
                productionReadySystems,
                profiles,
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private JudicialConnectorGovernanceItem buildItem(JudicialSystem system,
                                                      Set<String> globalBlockers,
                                                      Set<String> globalWarnings) {
        JudicialIntegrationProperties.Connector cfg = integrationProperties.connectorFor(system);
        boolean registered = registry.find(system).isPresent();
        JudicialSubmissionCapability capability = registry.find(system).map(JudicialProcessConnector::capability).orElse(null);
        JudicialConnectorOperationalProfileReport defaultProfile = operationalProfileService.analyze(system, capability, probeRequest(system, defaultTribunalProbe(cfg)));
        List<String> homologated = normalizeCodes(cfg.getHomologatedTribunals());
        List<String> blocked = normalizeCodes(cfg.getBlockedTribunals());
        List<String> conflicts = homologated.stream().filter(blocked::contains).toList();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        boolean governanceRelevant = cfg.isEnabled() || registered || system == JudicialSystem.OUTRO;
        if (cfg.isEnabled() && !registered) {
            blockers.add("CONNECTOR_ENABLED_BUT_NOT_REGISTERED");
        }
        if (cfg.isProductionReady() && homologated.isEmpty() && system != JudicialSystem.OUTRO) {
            warnings.add("CONNECTOR_PRODUCTION_READY_WITHOUT_EXPLICIT_TRIBUNAL_SCOPE");
        }
        if (!conflicts.isEmpty()) {
            blockers.add("CONNECTOR_TRIBUNAL_POLICY_CONFLICT");
        }
        if (registered && capability == null) {
            blockers.add("CONNECTOR_CAPABILITY_UNAVAILABLE");
        }
        if (system == JudicialSystem.OUTRO && !registered) {
            blockers.add("CONNECTOR_CONTINGENCY_MISSING");
        }
        if (cfg.isEnabled() && !hasText(cfg.getBaseUrl())) {
            blockers.add("CONNECTOR_ENABLED_WITHOUT_BASE_URL");
        }
        if (cfg.isEnabled() && cfg.isAuthRequired() && defaultProfile.authMode() == JudicialConnectorAuthMode.MISSING) {
            blockers.add("CONNECTOR_ENABLED_WITHOUT_AUTH_MODE");
        }
        if (governanceRelevant && system != JudicialSystem.OUTRO && !defaultProfile.readyForTribunalSubmission() && cfg.isEnabled()) {
            warnings.addAll(defaultProfile.warnings());
            if (!defaultProfile.blockers().isEmpty()) {
                warnings.add("CONNECTOR_NOT_READY_FOR_DEFAULT_SCOPE");
            }
        }
        if (capability != null && capability.enabled() && !capability.operational() && hasText(capability.baseUrl())) {
            warnings.add("CONNECTOR_ENABLED_WITH_PARTIAL_OPERATIONAL_CAPABILITY");
        }
        globalBlockers.addAll(blockers);
        globalWarnings.addAll(warnings);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseUrl", cfg.getBaseUrl());
        metadata.put("submitPath", cfg.getSubmitPath());
        metadata.put("dryRunPath", cfg.getDryRunPath());
        metadata.put("snapshotPath", cfg.getSnapshotPath());
        metadata.put("eventsPath", cfg.getEventsPath());
        metadata.put("productionReady", cfg.isProductionReady());
        metadata.put("tribunalSubmitPaths", normalizePathKeys(cfg.getTribunalSubmitPaths()));
        metadata.put("tribunalDryRunPaths", normalizePathKeys(cfg.getTribunalDryRunPaths()));
        metadata.put("tribunalSnapshotPaths", normalizePathKeys(cfg.getTribunalSnapshotPaths()));
        metadata.put("tribunalEventsPaths", normalizePathKeys(cfg.getTribunalEventsPaths()));
        metadata.put("requiresCertificate", cfg.isRequiresCertificate());
        metadata.put("requiresStepUpGovBr", cfg.isRequiresStepUpGovBr());
        metadata.put("authRequired", cfg.isAuthRequired());
        metadata.put("defaultProfile", defaultProfile.toMap());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorGovernanceItem(
                system,
                registered,
                cfg.isEnabled(),
                capability != null && capability.operational(),
                cfg.isProductionReady(),
                defaultProfile.authMode(),
                homologated,
                blocked,
                conflicts,
                defaultProfile,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private ProtocolSubmissionRequest probeRequest(JudicialSystem system,
                                                   String tribunalCodigo) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("probe", true);
        metadata.put("system", system != null ? system.name() : null);
        metadata.put("connectorHeaders", Map.of("X-PJB-Probe", "true"));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new ProtocolSubmissionRequest(
                "GOV-" + (system != null ? system.name() : JudicialSystem.OUTRO.name()) + '-' + firstNonBlank(tribunalCodigo, "DEFAULT"),
                null,
                "Governance Probe",
                tribunalCodigo,
                null,
                null,
                null,
                null,
                null,
                "{}",
                null,
                null,
                null,
                false,
                Map.copyOf(metadata)
        );
    }

    private String defaultTribunalProbe(JudicialIntegrationProperties.Connector cfg) {
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
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String code = normalizeCode(value);
            if (code != null) {
                normalized.add(code);
            }
        }
        return List.copyOf(normalized);
    }

    private List<String> normalizePathKeys(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        values.keySet().forEach(key -> {
            String code = normalizeCode(key);
            if (code != null) {
                normalized.add(code);
            }
        });
        return List.copyOf(normalized);
    }

    private String normalizeCode(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
