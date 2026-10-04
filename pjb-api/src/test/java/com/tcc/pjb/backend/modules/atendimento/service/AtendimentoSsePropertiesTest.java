package com.tcc.pjb.backend.modules.atendimento.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AtendimentoSsePropertiesTest {

    @Test
    void usaDefaultsQuandoConfigAusente() {
        AtendimentoSseProperties props = new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("pjb.atendimento.sse", AtendimentoSseProperties.class);

        assertThat(props.getReplayBuffer()).isEqualTo(200);
        assertThat(props.getMaxChannels()).isEqualTo(2048);
        assertThat(props.getMaxSubscribersPerTopic()).isEqualTo(5);
        assertThat(props.getIdleChannelTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void ligaChavesCamelCaseEDuration() {
        Map<String, Object> source = new HashMap<>();
        source.put("pjb.atendimento.sse.replayBuffer", "111");
        source.put("pjb.atendimento.sse.maxChannels", "1024");
        source.put("pjb.atendimento.sse.emitterTimeoutMs", "123456");
        source.put("pjb.atendimento.sse.idleChannelTtl", "7m");

        AtendimentoSseProperties props = new Binder(new MapConfigurationPropertySource(source))
                .bindOrCreate("pjb.atendimento.sse", AtendimentoSseProperties.class);

        assertThat(props.getReplayBuffer()).isEqualTo(111);
        assertThat(props.getMaxChannels()).isEqualTo(1024);
        assertThat(props.getEmitterTimeoutMs()).isEqualTo(123456L);
        assertThat(props.getIdleChannelTtl()).isEqualTo(Duration.ofMinutes(7));
    }
}
