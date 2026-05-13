package com.tcc.pjb.backend.core.criminal.custodia;

import com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult;

public interface BnmpConsultaService {

    BnmpConsultaResult consultarMandadoAtivo(String cpf);
}
