package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.live.NoOpLiveClusterStateStore;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PjbLivePressureServiceTest {

    @Test
    void shouldDetectLiveSurgeWhenSubscribersRiseQuickly() {
        PjbRuntimePressureProperties properties = new PjbRuntimePressureProperties();
        properties.setLiveTotalSubscribersThreshold(100);
        properties.setLiveSubscriberRisingFastDelta(50);
        properties.setLiveTrendWindow(Duration.ofSeconds(30));
        NoOpLiveClusterStateStore store = new NoOpLiveClusterStateStore();
        PjbLivePressureService service = new PjbLivePressureService(properties, store);
        service.snapshot(false);
        store.syncSubscriberCount("secretariat", "caixa-a", 180L, Duration.ofMinutes(1));
        PjbLivePressureService.Snapshot snapshot = service.snapshot(false);
        assertThat(snapshot.degraded()).isTrue();
        assertThat(snapshot.risingFast()).isTrue();
        assertThat(snapshot.criticalSurge()).isTrue();
    }
}
