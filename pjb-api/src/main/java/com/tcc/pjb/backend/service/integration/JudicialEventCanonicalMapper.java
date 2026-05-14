package com.tcc.pjb.backend.service.integration;

import com.tcc.pjb.backend.core.events.PjbIntegrationEventEnvelope;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JudicialEventCanonicalMapper {

    public record EventoJudicialRaw(
            String sistemaOrigem,
            String tipoEvento,
            String numeroProcesso,
            RitoProcessual rito,
            Map<String, Object> payload
    ) {}

    public PjbIntegrationEventEnvelope mapear(EventoJudicialRaw raw) {
        String routingKey = construirRoutingKey(raw);
        String hash = calcularHash(raw);
        return PjbIntegrationEventEnvelope.de(routingKey, raw.payload(), hash);
    }

    private String construirRoutingKey(EventoJudicialRaw raw) {
        return "judicial." + raw.sistemaOrigem().toLowerCase()
                + "." + raw.tipoEvento().toLowerCase()
                + "." + (raw.rito() != null ? raw.rito().name().toLowerCase() : "unknown");
    }

    private String calcularHash(EventoJudicialRaw raw) {
        return Integer.toHexString(raw.payload() != null ? raw.payload().hashCode() : 0);
    }
}
