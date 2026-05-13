package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorOperationalProfileService {

    private final JudicialConnectorRegistry registry;
    private final JudicialConnectorHomologationService homologationService;
    private final JudicialConnectorReadinessService readinessService;

    public JudicialConnectorOperationalProfileService(JudicialConnectorRegistry registry,
                                                      JudicialConnectorHomologationService homologationService,
                                                      JudicialConnectorReadinessService readinessService) {
        this.registry = Objects.requireNonNull(registry);
        this.homologationService = Objects.requireNonNull(homologationService);
        this.readinessService = Objects.requireNonNull(readinessService);
    }

    public JudicialConnectorOperationalProfileReport analyze(JudicialSystem system,
                                                             ProtocolSubmissionRequest request) {
        JudicialSubmissionCapability capability = registry.find(system).map(JudicialProcessConnector::capability).orElse(null);
        return analyze(system, capability, request);
    }

    public JudicialConnectorOperationalProfileReport analyze(JudicialSystem system,
                                                             JudicialSubmissionCapability capability,
                                                             ProtocolSubmissionRequest request) {
        JudicialConnectorHomologationReport homologation = homologationService.analyze(system, capability, request);
        ProtocolSubmissionRequest effectiveRequest = homologationService.apply(request, homologation);
        JudicialConnectorReadinessReport readiness = readinessService.analyze(system, capability, effectiveRequest);
        JudicialConnectorAuthMode authMode = JudicialConnectorAuthMode.from(readiness.metadata().get("authMode"));
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        blockers.addAll(readiness.blockers());
        blockers.addAll(homologation.blockers());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        warnings.addAll(readiness.warnings());
        warnings.addAll(homologation.warnings());
        boolean registered = registry.find(system).isPresent();
        boolean enabled = capability != null && capability.enabled();
        boolean operational = capability != null && capability.operational();
        boolean readyForTribunalSubmission = readiness.readyForSubmission() && homologation.submitHomologated() && !homologation.tribunalBlocked();
        boolean readyForProduction = readyForTribunalSubmission && homologation.productionReady();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseUrl", capability != null ? capability.baseUrl() : null);
        metadata.put("requestId", effectiveRequest != null ? effectiveRequest.requestId() : null);
        metadata.put("tribunalCodigo", effectiveRequest != null ? effectiveRequest.tribunalCodigo() : homologation.tribunalCodigo());
        metadata.put("submitPath", homologation.effectiveSubmitPath());
        metadata.put("dryRunPath", homologation.effectiveDryRunPath());
        metadata.put("snapshotPath", homologation.effectiveSnapshotPath());
        metadata.put("eventsPath", homologation.effectiveEventsPath());
        metadata.put("supportsDryRun", capability != null && capability.supportsDryRun());
        metadata.put("supportsSnapshotSync", capability != null && capability.supportsSnapshotSync());
        metadata.put("supportsEventSync", capability != null && capability.supportsEventSync());
        metadata.put("supportsExternalMedia", capability != null && capability.supportsExternalMedia());
        metadata.put("acceptedDocumentTypes", capability != null ? capability.acceptedDocumentTypes() : List.of());
        metadata.put("acceptedRamos", capability != null ? capability.acceptedRamos() : List.of());
        metadata.put("acceptedScopes", capability != null ? capability.acceptedScopes() : List.of());
        metadata.put("homologation", homologation.toMap());
        metadata.put("readiness", readiness.toMap());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorOperationalProfileReport(
                Instant.now(),
                system,
                effectiveRequest != null ? effectiveRequest.tribunalCodigo() : homologation.tribunalCodigo(),
                registered,
                enabled,
                operational,
                readyForProduction,
                readyForTribunalSubmission,
                authMode,
                homologation,
                readiness,
                List.copyOf(blockers),
                List.copyOf(warnings),
                Map.copyOf(metadata)
        );
    }
}
