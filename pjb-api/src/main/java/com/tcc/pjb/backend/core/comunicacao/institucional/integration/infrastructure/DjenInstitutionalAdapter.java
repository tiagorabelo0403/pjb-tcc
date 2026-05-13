package com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Component
public class DjenInstitutionalAdapter extends AbstractOutboxInstitutionalExternalAdapter {

    public DjenInstitutionalAdapter(OutboxPublisher outboxPublisher) {
        super(outboxPublisher);
    }

    @Override
    public boolean supports(CanalComunicacaoInstitucional channel) {
        return channel == CanalComunicacaoInstitucional.DJEN;
    }

    @Override
    protected String buildResponse(String outboxId) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("provider", "DJEN");
        response.put("outboxId", outboxId);
        response.put("contractVersion", "2026-03");
        return buildJson(response);
    }
}
