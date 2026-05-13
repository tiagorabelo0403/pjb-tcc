package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

public interface InstitutionalDeliveryHandler {

    boolean supports(CanalComunicacaoInstitucional channel);

    InstitutionalDeliveryDispatchResult dispatch(InstitutionalDeliveryJob job);
}
