package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RecursalMeshOperationalTelemetryServiceTest {

    @Test
    void shouldBoundNotificationFailureBuckets() {
        RecursalMeshOperationalTelemetryService telemetry = new RecursalMeshOperationalTelemetryService(new SimpleMeterRegistry());

        for (int i = 0; i < 120; i++) {
            telemetry.recordNotificationDelivery("channel-" + i, false);
        }

        var buckets = telemetry.notificationFailureBuckets(200);

        assertThat(buckets).hasSizeLessThanOrEqualTo(64);
        assertThat(buckets).anyMatch(bucket -> "other".equals(bucket.key()));
    }

    @Test
    void shouldBoundRetryExhaustedBuckets() {
        RecursalMeshOperationalTelemetryService telemetry = new RecursalMeshOperationalTelemetryService(new SimpleMeterRegistry());

        for (int i = 0; i < 220; i++) {
            telemetry.recordRetryExhausted("operation-" + i, "target-" + i);
        }

        var buckets = telemetry.retryExhaustedBuckets(300);

        assertThat(buckets).hasSizeLessThanOrEqualTo(128);
        assertThat(buckets).anyMatch(bucket -> "other".equals(bucket.key()));
    }
}
