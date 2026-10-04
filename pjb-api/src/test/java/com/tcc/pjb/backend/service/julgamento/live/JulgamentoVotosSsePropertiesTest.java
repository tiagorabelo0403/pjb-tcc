package com.tcc.pjb.backend.service.julgamento.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class JulgamentoVotosSsePropertiesTest {

    @Test
    void usaDefaultsQuandoConfigAusente() {
        JulgamentoVotosSseProperties props = new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("pjb.julgamento.votos.sse", JulgamentoVotosSseProperties.class);

        assertThat(props.getReplayBuffer()).isEqualTo(300);
        assertThat(props.getMaxSubscribersPerTopic()).isEqualTo(16);
        assertThat(props.getMaxChannels()).isEqualTo(4096);
        assertThat(props.getIdleChannelTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void ligaChavesCamelCaseEDuration() {
        Map<String, Object> source = new HashMap<>();
        source.put("pjb.julgamento.votos.sse.replayBuffer", "111");
        source.put("pjb.julgamento.votos.sse.maxSubscribersPerTopic", "20");
        source.put("pjb.julgamento.votos.sse.emitterTimeoutMs", "123456");
        source.put("pjb.julgamento.votos.sse.idleChannelTtl", "7m");

        JulgamentoVotosSseProperties props = new Binder(new MapConfigurationPropertySource(source))
                .bindOrCreate("pjb.julgamento.votos.sse", JulgamentoVotosSseProperties.class);

        assertThat(props.getReplayBuffer()).isEqualTo(111);
        assertThat(props.getMaxSubscribersPerTopic()).isEqualTo(20);
        assertThat(props.getEmitterTimeoutMs()).isEqualTo(123456L);
        assertThat(props.getIdleChannelTtl()).isEqualTo(Duration.ofMinutes(7));
    }
}
