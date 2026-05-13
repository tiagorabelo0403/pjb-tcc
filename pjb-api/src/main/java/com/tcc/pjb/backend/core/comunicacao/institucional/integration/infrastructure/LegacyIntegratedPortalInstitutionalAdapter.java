package com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure;

import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import org.springframework.stereotype.Component;

@Component
public class LegacyIntegratedPortalInstitutionalAdapter extends AbstractOutboxInstitutionalExternalAdapter {

    public LegacyIntegratedPortalInstitutionalAdapter(OutboxPublisher outboxPublisher) {
        super(outboxPublisher);
    }

    @Override
    public boolean supports(CanalComunicacaoInstitucional channel) {
        return channel == CanalComunicacaoInstitucional.PORTAL_LEGADO_INTEGRADO;
    }

    @Override
    protected String buildResponse(String outboxId) {
        return buildJson(java.util.Map.of(
                "status", "ACEITA",
                "provider", "LEGACY_PORTAL",
                "providerStatus", "LEGACY_PORTAL_ACCEPTED",
                "providerReference", "LEGACY-" + sanitize(outboxId),
                "outboxId", sanitize(outboxId)
        ));
    }
}
