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
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorControlPlaneService {

    private final JudicialConnectorRegistry registry;
    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorGovernanceService governanceService;
    private final JudicialConnectorOperationalProfileService operationalProfileService;

    public JudicialConnectorControlPlaneService(JudicialConnectorRegistry registry,
                                                JudicialIntegrationProperties integrationProperties,
                                                JudicialConnectorGovernanceService governanceService,
                                                JudicialConnectorOperationalProfileService operationalProfileService) {
        this.registry = Objects.requireNonNull(registry);
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
    }

    public JudicialConnectorControlPlaneReport nationalReport() {
        return buildReport(null);
    }

    public JudicialConnectorControlPlaneReport tribunalReport(String tribunalCodigo) {
        return buildReport(normalizeCode(tribunalCodigo));
    }

    private JudicialConnectorControlPlaneReport buildReport(String tribunalCodigo) {
        JudicialConnectorGovernanceReport governance = governanceService.report();
        JudicialConnectorTribunalLandscapeReport landscape = tribunalCodigo == null ? null : governanceService.reportForTribunal(tribunalCodigo);
        Map<JudicialSystem, JudicialConnectorGovernanceItem> governanceIndex = new LinkedHashMap<>();
        if (governance.connectors() != null) {
            governance.connectors().forEach(item -> governanceIndex.put(item.system(), item));
        }
        List<JudicialConnectorControlPlaneSystemReport> systems = Arrays.stream(JudicialSystem.values())
                .map(system -> buildSystemReport(system, governanceIndex.get(system), tribunalCodigo))
                .sorted(Comparator
                        .comparing(JudicialConnectorControlPlaneSystemReport::productionReady).reversed()
                        .thenComparing(JudicialConnectorControlPlaneSystemReport::tribunalReady).reversed()
                        .thenComparing(item -> item.system() != null ? item.system().name() : "ZZZ"))
                .toList();
        List<String> tribunalReadySystems = systems.stream()
                .filter(JudicialConnectorControlPlaneSystemReport::tribunalReady)
                .map(item -> item.system() != null ? item.system().name() : JudicialSystem.OUTRO.name())
                .toList();
        List<String> productionReadySystems = systems.stream()
                .filter(JudicialConnectorControlPlaneSystemReport::productionReady)
                .map(item -> item.system() != null ? item.system().name() : JudicialSystem.OUTRO.name())
                .toList();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (governance.blockers() != null) {
            blockers.addAll(governance.blockers());
        }
        if (governance.warnings() != null) {
            warnings.addAll(governance.warnings());
        }
        systems.forEach(item -> {
            if (item.blockers() != null) {
                blockers.addAll(item.blockers());
            }
            if (item.warnings() != null) {
                warnings.addAll(item.warnings());
            }
        });
        if (landscape != null && landscape.warnings() != null) {
            warnings.addAll(landscape.warnings());
        }
        if (tribunalCodigo != null && tribunalReadySystems.isEmpty()) {
            blockers.add("CONTROL_PLANE_NO_TRIBUNAL_READY_CONNECTOR");
        }
        if (productionReadySystems.isEmpty()) {
            warnings.add("CONTROL_PLANE_NO_PRODUCTION_READY_CONNECTOR");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("governance", governance.toMap());
        metadata.put("tribunalLandscape", landscape != null ? landscape.toMap() : null);
        metadata.put("registeredSystems", registry.all().stream().map(JudicialProcessConnector::system).filter(Objects::nonNull).map(Enum::name).sorted().toList());
        metadata.put("declaredSystems", Arrays.stream(JudicialSystem.values()).map(Enum::name).sorted().toList());
        metadata.put("controlPlaneMode", tribunalCodigo == null ? "NATIONAL" : "TRIBUNAL");
        metadata.put("contingencyPresent", registry.find(JudicialSystem.OUTRO).isPresent());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorControlPlaneReport(
                Instant.now(),
                tribunalCodigo,
                governance.registeredConnectorCount(),
                governance.enabledConnectorCount(),
                governance.operationalConnectorCount(),
                tribunalReadySystems,
                productionReadySystems,
                systems,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private JudicialConnectorControlPlaneSystemReport buildSystemReport(JudicialSystem system,
                                                                        JudicialConnectorGovernanceItem governanceItem,
                                                                        String tribunalCodigo) {
        JudicialIntegrationProperties.Connector cfg = integrationProperties.connectorFor(system);
        Optional<JudicialProcessConnector> connector = registry.find(system);
        JudicialSubmissionCapability capability = connector.map(JudicialProcessConnector::capability).orElse(null);
        String effectiveTribunal = firstNonBlank(tribunalCodigo, defaultTribunal(cfg));
        ProtocolSubmissionRequest probe = new ProtocolSubmissionRequest(
                "CONTROL-" + (system != null ? system.name() : JudicialSystem.OUTRO.name()) + '-' + firstNonBlank(effectiveTribunal, "DEFAULT"),
                null,
                "Control Plane Probe",
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
                Map.of("probe", true, "plane", "control")
        );
        JudicialConnectorOperationalProfileReport profile = operationalProfileService.analyze(system, capability, probe);
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (governanceItem != null) {
            blockers.addAll(governanceItem.blockers());
            warnings.addAll(governanceItem.warnings());
        }
        blockers.addAll(profile.blockers());
        warnings.addAll(profile.warnings());
        String controlStatus = resolveControlStatus(system, governanceItem, profile, blockers, warnings);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseUrl", capability != null ? capability.baseUrl() : null);
        metadata.put("tribunalCodigo", effectiveTribunal);
        metadata.put("homologatedTribunals", governanceItem != null ? governanceItem.homologatedTribunals() : normalizeCodes(cfg.getHomologatedTribunals()));
        metadata.put("blockedTribunals", governanceItem != null ? governanceItem.blockedTribunals() : normalizeCodes(cfg.getBlockedTribunals()));
        metadata.put("productionReady", governanceItem != null ? governanceItem.productionReady() : cfg.isProductionReady());
        metadata.put("registered", connector.isPresent());
        metadata.put("governanceRelevant", governanceItem != null || connector.isPresent() || cfg.isEnabled());
        metadata.put("defaultTribunal", defaultTribunal(cfg));
        metadata.put("controlPlaneMode", tribunalCodigo == null ? "DEFAULT_SCOPE" : "TRIBUNAL_SCOPE");
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorControlPlaneSystemReport(
                Instant.now(),
                system,
                effectiveTribunal,
                controlStatus,
                connector.isPresent() || governanceItem != null && governanceItem.connectorRegistered(),
                governanceItem != null ? governanceItem.configuredEnabled() : capability != null && capability.enabled(),
                governanceItem != null ? governanceItem.operational() : capability != null && capability.operational(),
                profile.readyForProduction(),
                profile.readyForTribunalSubmission(),
                profile.authMode(),
                governanceItem,
                profile,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }

    private String resolveControlStatus(JudicialSystem system,
                                        JudicialConnectorGovernanceItem governanceItem,
                                        JudicialConnectorOperationalProfileReport profile,
                                        Set<String> blockers,
                                        Set<String> warnings) {
        if (system == JudicialSystem.OUTRO && blockers.isEmpty() && governanceItem != null && governanceItem.connectorRegistered()) {
            return "CONTINGENCY_READY";
        }
        if (!blockers.isEmpty()) {
            return "BLOCKED";
        }
        if (profile.readyForProduction()) {
            return warnings.isEmpty() ? "PRODUCTION_READY" : "PRODUCTION_READY_WITH_WARNINGS";
        }
        if (profile.readyForTribunalSubmission()) {
            return warnings.isEmpty() ? "TRIBUNAL_READY" : "TRIBUNAL_READY_WITH_WARNINGS";
        }
        if (profile.connectorEnabled()) {
            return warnings.isEmpty() ? "CONFIGURED" : "CONFIGURED_WITH_WARNINGS";
        }
        return "DISABLED";
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
}
