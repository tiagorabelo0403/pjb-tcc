package com.tcc.pjb.backend.judicial.connectors.application;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoCommandCenterService;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorHubReport;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorStructureReport;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialConnectorHubService {

    private final JudicialConnectorStructureService structureService;
    private final JudicialConnectorCommandCenterService commandCenterService;
    private final JudicialConnectorCryptoCommandCenterService cryptoCommandCenterService;

    public JudicialConnectorHubService(JudicialConnectorStructureService structureService,
                                       JudicialConnectorCommandCenterService commandCenterService,
                                       JudicialConnectorCryptoCommandCenterService cryptoCommandCenterService) {
        this.structureService = Objects.requireNonNull(structureService);
        this.commandCenterService = Objects.requireNonNull(commandCenterService);
        this.cryptoCommandCenterService = Objects.requireNonNull(cryptoCommandCenterService);
    }

    @Transactional(readOnly = true)
    public JudicialConnectorHubReport nationalReport(Duration horizon) {
        return buildReport(null, horizon);
    }

    @Transactional(readOnly = true)
    public JudicialConnectorHubReport tribunalReport(String tribunalCodigo, Duration horizon) {
        return buildReport(normalize(tribunalCodigo), horizon);
    }

    @Transactional(readOnly = true)
    public JudicialConnectorStructureReport structureReport() {
        return structureService.report();
    }

    private JudicialConnectorHubReport buildReport(String tribunalCodigo, Duration horizon) {
        Duration effectiveHorizon = safeHorizon(horizon);
        JudicialConnectorStructureReport structure = structureService.report();
        JudicialConnectorCommandCenterReport operational = tribunalCodigo == null
                ? commandCenterService.nationalReport(effectiveHorizon)
                : commandCenterService.tribunalReport(tribunalCodigo, effectiveHorizon);
        JudicialConnectorCryptoCommandCenterReport cryptography = tribunalCodigo == null
                ? cryptoCommandCenterService.nationalReport(effectiveHorizon)
                : cryptoCommandCenterService.tribunalReport(tribunalCodigo, effectiveHorizon);
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        if (operational.alerts() != null) {
            alerts.addAll(operational.alerts());
        }
        if (cryptography.alerts() != null) {
            alerts.addAll(cryptography.alerts());
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", tribunalCodigo);
        metadata.put("horizonSeconds", effectiveHorizon.toSeconds());
        metadata.put("operationalAlertCount", operational.alerts() != null ? operational.alerts().size() : 0);
        metadata.put("cryptographicAlertCount", cryptography.alerts() != null ? cryptography.alerts().size() : 0);
        metadata.put("structureNodeCount", structure.nodes().size());
        metadata.put("entrypoint", tribunalCodigo == null ? "NATIONAL" : "TRIBUNAL");
        return new JudicialConnectorHubReport(
                Instant.now(),
                tribunalCodigo,
                structure,
                operational,
                cryptography,
                alerts.stream().toList(),
                Map.copyOf(JudicialMapSupport.copyNonNull(metadata))
        );
    }

    private Duration safeHorizon(Duration horizon) {
        return horizon == null || horizon.isNegative() || horizon.isZero() ? Duration.ofHours(24) : horizon;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized.toUpperCase();
    }
}
