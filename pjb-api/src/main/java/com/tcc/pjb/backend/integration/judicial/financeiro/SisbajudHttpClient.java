package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse;
import java.math.BigDecimal;

public interface SisbajudHttpClient {

    SisbajudHttpResponse solicitarBloqueio(String cpfDevedor, BigDecimal valorSolicitado, String numeroOficio);
}
