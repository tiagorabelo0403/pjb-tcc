package com.tcc.pjb.backend.platform.observability.ai;

import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMicrometerTelemetryTest {

    @Test
    void shouldBoundRequestMetricCardinalityWhenCapabilitiesExplode() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMicrometerTelemetry telemetry = new AiMicrometerTelemetry(registry);

        for (int i = 0; i < 200; i++) {
            telemetry.record(AiTelemetryDomain.LEGAL, "capability_" + i, ApiVersion.V1, "ok", Duration.ofMillis(5));
        }

        long requestMeters = registry.getMeters().stream()
                .filter(meter -> AiMicrometerTelemetry.METRIC_REQUESTS.equals(meter.getId().getName())
                        || AiMicrometerTelemetry.METRIC_DURATION.equals(meter.getId().getName()))
                .count();

        assertTrue(requestMeters <= 258L);
    }
}
