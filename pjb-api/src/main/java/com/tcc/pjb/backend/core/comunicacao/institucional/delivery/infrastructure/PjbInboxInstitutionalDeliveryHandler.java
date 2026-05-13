package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

@Component
public class PjbInboxInstitutionalDeliveryHandler implements InstitutionalDeliveryHandler {

    @Override
    public boolean supports(CanalComunicacaoInstitucional channel) {
        return channel == CanalComunicacaoInstitucional.PJB_INBOX;
    }

    @Override
    public InstitutionalDeliveryDispatchResult dispatch(InstitutionalDeliveryJob job) {
        return InstitutionalDeliveryDispatchResult.entregue(
                "PJB-INBOX:" + job.expedicaoUuid(),
                "LOCAL_DELIVERED",
                "Entrega institucional consolidada na caixa nativa do PJB."
        );
    }
}
