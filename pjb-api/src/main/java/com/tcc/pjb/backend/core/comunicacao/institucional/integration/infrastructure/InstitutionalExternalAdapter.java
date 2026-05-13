package com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatchResult;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

public interface InstitutionalExternalAdapter {

    boolean supports(CanalComunicacaoInstitucional channel);

    InstitutionalExternalDispatchResult dispatch(InstitutionalExternalDispatch dispatch);
}
