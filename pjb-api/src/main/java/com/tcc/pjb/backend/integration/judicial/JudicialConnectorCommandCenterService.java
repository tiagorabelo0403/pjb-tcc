package com.tcc.pjb.backend.integration.judicial;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorCommandCenterService {

    private final JudicialConnectorGovernanceService governanceService;
    private final JudicialConnectorControlPlaneService controlPlaneService;
    private final JudicialConnectorDataPlaneService dataPlaneService;
    private final JudicialConnectorCryptographyProfileService cryptographyProfileService;
    private final JudicialConnectorObservabilityService observabilityService;
    private final JudicialConnectorPolicyService policyService;
    private final JudicialConnectorAdminOpsService adminOpsService;

    public JudicialConnectorCommandCenterService(JudicialConnectorGovernanceService governanceService,
                                                 JudicialConnectorControlPlaneService controlPlaneService,
                                                 JudicialConnectorDataPlaneService dataPlaneService,
                                                 JudicialConnectorCryptographyProfileService cryptographyProfileService,
                                                 JudicialConnectorObservabilityService observabilityService,
                                                 JudicialConnectorPolicyService policyService,
                                                 JudicialConnectorAdminOpsService adminOpsService) {
        this.governanceService = Objects.requireNonNull(governanceService);
        this.controlPlaneService = Objects.requireNonNull(controlPlaneService);
        this.dataPlaneService = Objects.requireNonNull(dataPlaneService);
        this.cryptographyProfileService = Objects.requireNonNull(cryptographyProfileService);
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.policyService = Objects.requireNonNull(policyService);
        this.adminOpsService = Objects.requireNonNull(adminOpsService);
    }

    public JudicialConnectorCommandCenterReport nationalReport(Duration horizon) { return buildReport(null, horizon); }
    public JudicialConnectorCommandCenterReport tribunalReport(String tribunalCodigo, Duration horizon) { return buildReport(tribunalCodigo, horizon); }

    private JudicialConnectorCommandCenterReport buildReport(String tribunalCodigo, Duration horizon) {
        Duration effectiveHorizon = safeHorizon(horizon);
        JudicialConnectorGovernanceReport governance = governanceService.report();
        JudicialConnectorControlPlaneReport controlPlane = tribunalCodigo == null ? controlPlaneService.nationalReport() : controlPlaneService.tribunalReport(tribunalCodigo);
        JudicialConnectorDataPlaneReport dataPlane = tribunalCodigo == null ? dataPlaneService.nationalReport(effectiveHorizon) : dataPlaneService.tribunalReport(tribunalCodigo, effectiveHorizon);
        JudicialConnectorCryptographyReport cryptography = tribunalCodigo == null ? cryptographyProfileService.nationalReport() : cryptographyProfileService.tribunalReport(tribunalCodigo);
        JudicialConnectorObservabilityReport observability = tribunalCodigo == null ? observabilityService.nationalReport(effectiveHorizon) : observabilityService.tribunalReport(tribunalCodigo, effectiveHorizon);
        JudicialConnectorPolicyReport policies = policyService.report();
        List<Map<String, Object>> recentOperations = adminOpsService.recentOperations();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        alerts.addAll(governance.blockers());
        alerts.addAll(governance.warnings());
        alerts.addAll(controlPlane.blockers());
        alerts.addAll(controlPlane.warnings());
        alerts.addAll(dataPlane.alerts());
        alerts.addAll(cryptography.blockers());
        alerts.addAll(cryptography.warnings());
        alerts.addAll(observability.alerts());
        alerts.addAll(policies.warnings());
        if (tribunalCodigo != null && controlPlane.tribunalReadySystems().isEmpty()) alerts.add("COMMAND_CENTER_NO_TRIBUNAL_READY_CONNECTOR");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("recentOperationCount", recentOperations.size());
        metadata.put("mode", tribunalCodigo == null ? "NATIONAL" : "TRIBUNAL");
        metadata.put("securityPosture", JudicialMapSupport.compact(
                "strongAuthenticationCount", cryptography.strongAuthenticationCount(),
                "certificateReadyCount", cryptography.certificateReadyCount(),
                "blockedCount", cryptography.blockedCount()
        ));
        metadata.put("observabilityPosture", JudicialMapSupport.compact(
                "healthySystems", observability.healthySystems(),
                "degradedSystems", observability.degradedSystems(),
                "blockedSystems", observability.blockedSystems()
        ));
        return new JudicialConnectorCommandCenterReport(
                Instant.now(),
                tribunalCodigo,
                governance,
                controlPlane,
                dataPlane,
                cryptography,
                observability,
                policies,
                List.copyOf(recentOperations),
                new ArrayList<>(alerts),
                Map.copyOf(metadata)
        );
    }

    private Duration safeHorizon(Duration horizon) { return horizon == null || horizon.isNegative() || horizon.isZero() ? Duration.ofHours(24) : horizon; }
}
