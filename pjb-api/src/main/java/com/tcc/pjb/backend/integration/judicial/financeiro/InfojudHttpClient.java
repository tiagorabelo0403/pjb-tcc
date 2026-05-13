package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResponse;

public interface InfojudHttpClient {

    InfojudConsultaResponse consultar(String cpfCnpjConsultado);
}
