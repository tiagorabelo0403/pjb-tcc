package com.tcc.pjb.backend.service.integration;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JudicialEventCanonicalMapperTest {

    private final JudicialEventCanonicalMapper mapper = new JudicialEventCanonicalMapper();

    @Test
    void mapear_envelopeComRoutingKey_quandoEventoValido() {
        Map<String, Object> campos = Map.of("campo", "valor");
        var raw = new JudicialEventCanonicalMapper.EventoJudicialRaw(
                "PJE", "MOVIMENTACAO", "0001234-12.2025.8.06.0001",
                RitoProcessual.PROCEDIMENTO_COMUM, campos);
        var envelope = mapper.mapear(raw);
        assertThat(envelope.routingKey()).isEqualTo("judicial.pje.movimentacao.comum_ordinario");
        assertThat(envelope.uuid()).isNotNull();
        assertThat(envelope.appName()).isEqualTo("pjb");
    }

    @Test
    void mapear_routingKeyComUnknown_quandoRitoNulo() {
        Map<String, Object> vazio = Map.of();
        var raw = new JudicialEventCanonicalMapper.EventoJudicialRaw(
                "MNI", "REMESSA", "0001", null, vazio);
        var envelope = mapper.mapear(raw);
        assertThat(envelope.routingKey()).contains("unknown");
    }

    @Test
    void mapear_payloadPreservado_noEnvelope() {
        Map<String, Object> payload = Map.of("numero", "0001234-12.2025.8.06.0001");
        var raw = new JudicialEventCanonicalMapper.EventoJudicialRaw(
                "EPROC", "CITACAO", "0001234-12.2025.8.06.0001",
                RitoProcessual.PROCEDIMENTO_PENAL_COMUM, payload);
        var envelope = mapper.mapear(raw);
        assertThat(envelope.payload()).isEqualTo(payload);
        assertThat(envelope.payloadHash()).isNotNull();
    }
}
