package com.tcc.pjb.backend.service.ui.presentation.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class UiPresentationSsePropertiesTest {

    @Test
    void usaDefaultsQuandoConfigAusente() {
        UiPresentationSseProperties props = new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("pjb.ui.presentation.sse", UiPresentationSseProperties.class);

        assertThat(props.getReplayBuffer()).isEqualTo(300);
        assertThat(props.getMaxBatchEvents()).isEqualTo(200);
        assertThat(props.getMaxChannels()).isEqualTo(4096);
        assertThat(props.getIdleChannelTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void ligaChavesCamelCaseEDuration() {
        Map<String, Object> source = new HashMap<>();
        source.put("pjb.ui.presentation.sse.replayBuffer", "111");
        source.put("pjb.ui.presentation.sse.maxSubscribersPerTopic", "9");
        source.put("pjb.ui.presentation.sse.emitterTimeoutMs", "123456");
        source.put("pjb.ui.presentation.sse.idleChannelTtl", "7m");

        UiPresentationSseProperties props = new Binder(new MapConfigurationPropertySource(source))
                .bindOrCreate("pjb.ui.presentation.sse", UiPresentationSseProperties.class);

        assertThat(props.getReplayBuffer()).isEqualTo(111);
        assertThat(props.getMaxSubscribersPerTopic()).isEqualTo(9);
        assertThat(props.getEmitterTimeoutMs()).isEqualTo(123456L);
        assertThat(props.getIdleChannelTtl()).isEqualTo(Duration.ofMinutes(7));
    }
}
