package com.tcc.pjb.backend.modules.atendimento.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AtendimentoAttachmentPropertiesTest {

    @Test
    void usaDefaultsQuandoConfigAusente() {
        AtendimentoAttachmentProperties props = new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("pjb.atendimento.attachments", AtendimentoAttachmentProperties.class);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getMaxPerMessage()).isEqualTo(3);
        assertThat(props.getMaxTotalBytesPerMessage()).isEqualTo(20_971_520L);
    }

    @Test
    void ligaChavesCamelCase() {
        Map<String, Object> source = new HashMap<>();
        source.put("pjb.atendimento.attachments.enabled", "true");
        source.put("pjb.atendimento.attachments.maxPerMessage", "5");
        source.put("pjb.atendimento.attachments.maxTotalBytesPerMessage", "1048576");

        AtendimentoAttachmentProperties props = new Binder(new MapConfigurationPropertySource(source))
                .bindOrCreate("pjb.atendimento.attachments", AtendimentoAttachmentProperties.class);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getMaxPerMessage()).isEqualTo(5);
        assertThat(props.getMaxTotalBytesPerMessage()).isEqualTo(1_048_576L);
    }
}
