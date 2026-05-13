package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class PjbRuntimeDrainServiceTest {

    @Test
    void shouldRefuseTrafficWhenDrainBegins() {
        PjbRuntimeLifecycleProperties properties = new PjbRuntimeLifecycleProperties();
        RecordingPublisher publisher = new RecordingPublisher();
        PjbRuntimeDrainService service = new PjbRuntimeDrainService(properties, publisher);
        service.markAccepting("ready");
        boolean changed = service.beginDrain("shutdown");
        assertThat(changed).isTrue();
        assertThat(service.isDraining()).isTrue();
        assertThat(service.readyForTraffic()).isFalse();
        assertThat(service.snapshot().reason()).isEqualTo("shutdown");
        assertThat(publisher.events).isNotEmpty();
    }

    @Test
    void shouldKeepReadyForTrafficWhenDrainDoesNotFailReadiness() {
        PjbRuntimeLifecycleProperties properties = new PjbRuntimeLifecycleProperties();
        properties.setFailReadyWhenDraining(false);
        PjbRuntimeDrainService service = new PjbRuntimeDrainService(properties, event -> {
        });
        service.markAccepting("ready");
        service.beginDrain("maintenance");
        assertThat(service.isDraining()).isTrue();
        assertThat(service.readyForTraffic()).isTrue();
    }

    private static final class RecordingPublisher implements ApplicationEventPublisher {

        private final List<Object> events = new ArrayList<>();

        @Override
        public void publishEvent(ApplicationEvent event) {
            events.add(event);
        }

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }
    }
}
