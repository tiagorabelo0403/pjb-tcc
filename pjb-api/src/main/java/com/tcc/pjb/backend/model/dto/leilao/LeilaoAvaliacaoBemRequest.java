package com.tcc.pjb.backend.model.dto.leilao;

import java.math.BigDecimal;

public record LeilaoAvaliacaoBemRequest(
        String tipoBem,
        BigDecimal valorMercado,
        BigDecimal valorAvaliacaoJudicial,
        String estadoConservacao,
        String ocupacao,
        boolean possuiRestricaoRegistral,
        boolean emComarcaLiquida
) {
}
