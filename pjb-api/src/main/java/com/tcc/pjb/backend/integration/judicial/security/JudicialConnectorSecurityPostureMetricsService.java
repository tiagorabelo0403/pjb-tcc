package com.tcc.pjb.backend.integration.judicial.security;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorSecurityPostureMetricsService {

    private final MultiGauge inventoryStatusGauge;
    private final MultiGauge inventoryAttributeGauge;

    public JudicialConnectorSecurityPostureMetricsService(MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry);
        this.inventoryStatusGauge = MultiGauge.builder("pjb.judicial.security.inventory.status")
                .description("Current judicial connector cryptographic inventory by validation status")
                .register(meterRegistry);
        this.inventoryAttributeGauge = MultiGauge.builder("pjb.judicial.security.inventory.attribute")
                .description("Current judicial connector cryptographic inventory by posture attribute")
                .register(meterRegistry);
    }

    public void publish(Collection<JudicialConnectorCertificateInventoryReport> reports) {
        List<JudicialConnectorCertificateInventoryReport> items = reports == null ? List.of() : List.copyOf(reports);
        ArrayList<MultiGauge.Row<?>> statusRows = new ArrayList<>();
        ArrayList<MultiGauge.Row<?>> attributeRows = new ArrayList<>();
        for (Map.Entry<String, Long> entry : aggregateStatus(items).entrySet()) {
            String[] parts = entry.getKey().split("\\|", -1);
            statusRows.add(MultiGauge.Row.of(io.micrometer.core.instrument.Tags.of("system", parts[0], "status", parts[1]), entry.getValue()));
        }
        for (Map.Entry<String, Long> entry : aggregateAttributes(items).entrySet()) {
            String[] parts = entry.getKey().split("\\|", -1);
            attributeRows.add(MultiGauge.Row.of(io.micrometer.core.instrument.Tags.of("system", parts[0], "attribute", parts[1]), entry.getValue()));
        }
        inventoryStatusGauge.register(statusRows, true);
        inventoryAttributeGauge.register(attributeRows, true);
    }

    private Map<String, Long> aggregateStatus(Collection<JudicialConnectorCertificateInventoryReport> reports) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (JudicialConnectorCertificateInventoryReport report : reports) {
            String system = report.system() == null ? "OUTRO" : report.system().name();
            String status = normalized(report.validationStatus(), "UNKNOWN");
            out.merge(system + '|' + status, 1L, Long::sum);
        }
        if (out.isEmpty()) {
            out.put("OUTRO|UNKNOWN", 0L);
        }
        return out;
    }

    private Map<String, Long> aggregateAttributes(Collection<JudicialConnectorCertificateInventoryReport> reports) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (JudicialConnectorCertificateInventoryReport report : reports) {
            String system = report.system() == null ? "OUTRO" : report.system().name();
            if (report.expired()) {
                out.merge(system + "|EXPIRED", 1L, Long::sum);
            }
            if (report.expiresSoon()) {
                out.merge(system + "|EXPIRING_SOON", 1L, Long::sum);
            }
            if (report.hardwareBacked()) {
                out.merge(system + "|HARDWARE_BACKED", 1L, Long::sum);
            }
            if (report.revocationHardFailed()) {
                out.merge(system + "|REVOCATION_HARD_FAILED", 1L, Long::sum);
            }
            if (report.pathValidationSucceeded()) {
                out.merge(system + "|PATH_VALIDATED", 1L, Long::sum);
            }
        }
        if (out.isEmpty()) {
            out.put("OUTRO|NONE", 0L);
        }
        return out;
    }

    private String normalized(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
