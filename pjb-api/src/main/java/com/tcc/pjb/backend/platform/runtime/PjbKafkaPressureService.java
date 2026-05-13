package com.tcc.pjb.backend.platform.runtime;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PjbKafkaPressureService {

    private final PjbRuntimePressureProperties properties;
    private final Map<String, KafkaTemplate<String, Object>> kafkaTemplates;
    private final AtomicReference<Snapshot> lastSnapshot = new AtomicReference<>(new Snapshot(false, "", 1.0d, 0.0d, 0.0d, 0.0d, false, false));

    public PjbKafkaPressureService(PjbRuntimePressureProperties properties,
                                   Map<String, KafkaTemplate<String, Object>> kafkaTemplates) {
        this.properties = properties;
        this.kafkaTemplates = kafkaTemplates;
    }

    public Snapshot snapshot(boolean warmingUp) {
        KafkaTemplate<String, Object> template = kafkaTemplates.values().stream().findFirst().orElse(null);
        if (template == null) {
            Snapshot snapshot = new Snapshot(false, "", 1.0d, 0.0d, 0.0d, 0.0d, false, false);
            lastSnapshot.set(snapshot);
            return snapshot;
        }
        Map<MetricName, ? extends Metric> metrics = template.metrics();
        double bufferAvailable = metricValue(metrics, "buffer-available-bytes");
        double bufferTotal = metricValue(metrics, "buffer-total-bytes");
        double inFlight = metricValue(metrics, "requests-in-flight");
        double recordQueueTimeAvg = metricValue(metrics, "record-queue-time-avg");
        double recordErrorRate = metricValue(metrics, "record-error-rate");
        double bufferAvailableRatio = bufferTotal > 0.0d ? bufferAvailable / bufferTotal : 1.0d;
        boolean degraded = properties.isEnabled() && !warmingUp && (bufferAvailableRatio <= clampRatio(properties.getKafkaBufferAvailableRatioThreshold(), 0.20d)
                || inFlight >= Math.max(1.0d, properties.getKafkaRequestsInFlightThreshold())
                || recordQueueTimeAvg >= Math.max(1.0d, properties.getKafkaRecordQueueTimeAvgThresholdMillis())
                || recordErrorRate >= Math.max(0.0001d, properties.getKafkaRecordErrorRateThreshold()));
        boolean critical = degraded && (bufferAvailableRatio <= Math.max(0.01d, clampRatio(properties.getKafkaBufferAvailableRatioThreshold(), 0.20d) * 0.5d)
                || inFlight >= Math.max(2.0d, properties.getKafkaRequestsInFlightThreshold() * 1.5d)
                || recordErrorRate >= Math.max(0.001d, properties.getKafkaRecordErrorRateThreshold() * 2.0d));
        Snapshot snapshot = new Snapshot(true, template.getDefaultTopic() == null ? "" : template.getDefaultTopic(), round(bufferAvailableRatio), round(inFlight), round(recordQueueTimeAvg), round(recordErrorRate), degraded, critical);
        lastSnapshot.set(snapshot);
        return snapshot;
    }

    public Snapshot lastSnapshot() {
        return lastSnapshot.get();
    }

    private double metricValue(Map<MetricName, ? extends Metric> metrics, String metricName) {
        if (metrics == null || metrics.isEmpty()) {
            return 0.0d;
        }
        return metrics.entrySet().stream()
                .filter(entry -> entry.getKey() != null && metricName.equals(entry.getKey().name()))
                .map(Map.Entry::getValue)
                .map(Metric::metricValue)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToDouble(Number::doubleValue)
                .findFirst()
                .orElse(0.0d);
    }

    private double clampRatio(double value, double fallback) {
        if (value <= 0.0d || value >= 1.0d) {
            return fallback;
        }
        return value;
    }

    private double round(double value) {
        return Math.round(value * 1000.0d) / 1000.0d;
    }

    public record Snapshot(boolean available,
                           String defaultTopic,
                           double bufferAvailableRatio,
                           double requestsInFlight,
                           double recordQueueTimeAvg,
                           double recordErrorRate,
                           boolean degraded,
                           boolean critical) {
    }
}
