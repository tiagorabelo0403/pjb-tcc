package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardBucket;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshIndexDriftReport;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshOperationalAlert;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshOperationalAlertReport;

@Service
public class RecursalMeshOperationalAlertService {

    private final RecursalMeshDashboardService dashboardService;
    private final RecursalMeshIndexDriftService driftService;
    private final ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider;

    public RecursalMeshOperationalAlertService(RecursalMeshDashboardService dashboardService,
                                               RecursalMeshIndexDriftService driftService,
                                               ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider) {
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
        this.driftService = Objects.requireNonNull(driftService, "driftService");
        this.telemetryProvider = Objects.requireNonNull(telemetryProvider, "telemetryProvider");
    }

    public RecursalMeshOperationalAlertReport report(Integer scanLimit, Integer bucketLimit) {
        int normalizedScanLimit = normalize(scanLimit, 1000, 100, 5000);
        int normalizedBucketLimit = normalize(bucketLimit, 10, 3, 25);
        RecursalMeshDashboardResponse dashboard = dashboardService.dashboard(new RecursalMeshDashboardRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                normalizedScanLimit,
                normalizedBucketLimit
        ));
        RecursalMeshIndexDriftReport drift = driftService.assess(Math.min(500, Math.max(50, normalizedBucketLimit * 10)));
        RecursalMeshOperationalTelemetryService telemetry = telemetryProvider.getIfAvailable();
        List<RecursalMeshDashboardBucket> failuresByChannel = telemetry == null ? List.of() : telemetry.notificationFailureBuckets(normalizedBucketLimit);
        List<RecursalMeshDashboardBucket> retryExhausted = telemetry == null ? List.of() : telemetry.retryExhaustedBuckets(normalizedBucketLimit);
        List<RecursalMeshOperationalAlert> alerts = buildAlerts(dashboard, drift, failuresByChannel, retryExhausted);
        return new RecursalMeshOperationalAlertReport(
                dashboard.source(),
                drift,
                failuresByChannel,
                retryExhausted,
                dashboard.gargalosPorTribunal(),
                dashboard.gargalosPorAutoridadeAtual(),
                alerts,
                Instant.now()
        );
    }

    private List<RecursalMeshOperationalAlert> buildAlerts(RecursalMeshDashboardResponse dashboard,
                                                           RecursalMeshIndexDriftReport drift,
                                                           List<RecursalMeshDashboardBucket> failuresByChannel,
                                                           List<RecursalMeshDashboardBucket> retryExhausted) {
        Instant now = Instant.now();
        List<RecursalMeshOperationalAlert> alerts = new ArrayList<>();
        String driftSeverity = normalizeSeverity(drift == null ? null : drift.severity());
        if (!"HEALTHY".equals(driftSeverity)) {
            alerts.add(new RecursalMeshOperationalAlert(
                    "INDEX_DRIFT",
                    driftSeverity,
                    "Drift relevante entre projeção recursal e índice operacional",
                    "Diferenças detectadas no índice recursal com severidade " + driftSeverity + ".",
                    now
            ));
        }
        firstNonZero(failuresByChannel).ifPresent(bucket -> alerts.add(new RecursalMeshOperationalAlert(
                "NOTIFICATION_FAILURE_CHANNEL",
                bucket.total() >= 10 ? "HIGH" : "MONITOR",
                "Falhas de notificação recursal por canal",
                "Canal com maior incidência: " + bucket.key() + " com " + bucket.total() + " falhas acumuladas.",
                now
        )));
        firstNonZero(retryExhausted).ifPresent(bucket -> alerts.add(new RecursalMeshOperationalAlert(
                "RETRY_EXHAUSTED_TARGET",
                bucket.total() >= 5 ? "HIGH" : "MONITOR",
                "Retentativas exauridas em operação recursal",
                "Alvo com exaustão de retry: " + bucket.key() + " em " + bucket.total() + " ocorrências.",
                now
        )));
        firstNonZero(dashboard.gargalosPorTribunal()).ifPresent(bucket -> alerts.add(new RecursalMeshOperationalAlert(
                "STUCK_TRIBUNAL",
                bucket.total() >= 20 ? "HIGH" : "MONITOR",
                "Gargalo recursal por tribunal",
                "Tribunal com maior concentração de SLA vencido: " + bucket.key() + " em " + bucket.total() + " itens.",
                now
        )));
        firstNonZero(dashboard.gargalosPorAutoridadeAtual()).ifPresent(bucket -> alerts.add(new RecursalMeshOperationalAlert(
                "STUCK_AUTHORITY",
                bucket.total() >= 20 ? "HIGH" : "MONITOR",
                "Gargalo recursal por autoridade",
                "Autoridade com maior concentração de SLA vencido: " + bucket.key() + " em " + bucket.total() + " itens.",
                now
        )));
        return alerts;
    }

    private java.util.Optional<RecursalMeshDashboardBucket> firstNonZero(List<RecursalMeshDashboardBucket> buckets) {
        return (buckets == null ? List.<RecursalMeshDashboardBucket>of() : buckets).stream()
                .filter(bucket -> bucket != null && bucket.total() > 0L)
                .findFirst();
    }

    private int normalize(Integer value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "UNKNOWN";
        }
        return severity.trim().toUpperCase(Locale.ROOT);
    }
}
