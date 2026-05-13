package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.application.InstitutionalExternalIntegrationApplicationService;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

@Component
public class ExternalBridgeInstitutionalDeliveryHandler implements InstitutionalDeliveryHandler {

    private final InstitutionalExternalIntegrationApplicationService integrationApplicationService;

    public ExternalBridgeInstitutionalDeliveryHandler(InstitutionalExternalIntegrationApplicationService integrationApplicationService) {
        this.integrationApplicationService = Objects.requireNonNull(integrationApplicationService);
    }

    @Override
    public boolean supports(CanalComunicacaoInstitucional channel) {
        return channel != null && channel.isPrincipalJuridico() && channel != CanalComunicacaoInstitucional.PJB_INBOX;
    }

    @Override
    public InstitutionalDeliveryDispatchResult dispatch(InstitutionalDeliveryJob job) {
        if (!supports(job.currentChannel())) {
            return InstitutionalDeliveryDispatchResult.falhaTerminal(
                    com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional.CANAL_NAO_SUPORTADO,
                    "UNSUPPORTED_CHANNEL",
                    "Canal não suportado pelo bridge institucional atual."
            );
        }
        return integrationApplicationService.despachar(job);
    }
}
