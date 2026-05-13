package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.eleitoral.domain.PrestacaoContasSnapshot;

public interface TseSpcaClient {

    PrestacaoContasSnapshot consultarPrestacaoContas(Long processoId, String numeroCandidato, String anoEleitoral);
}
