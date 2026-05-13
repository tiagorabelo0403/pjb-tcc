package com.tcc.pjb.backend.service.cidadao.dashboard;

public record CidadaoDashboardRefreshRequest(
        String cpf,
        long atEpochSec
) {
}
