package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResponse;

public interface RenajudHttpClient {

    RenajudRestricaoResponse solicitarRestricao(String placa, String renavam, String tipo);
}
