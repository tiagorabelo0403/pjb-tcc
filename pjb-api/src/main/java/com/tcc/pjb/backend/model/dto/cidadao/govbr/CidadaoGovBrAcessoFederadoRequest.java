package com.tcc.pjb.backend.model.dto.cidadao.govbr;

public record CidadaoGovBrAcessoFederadoRequest(
        String sistemaOrigem,
        String tribunalCodigo,
        String numeroProcesso,
        String nivelSigilo,
        boolean possuiDocumentos,
        boolean possuiMidiaExterna,
        boolean exigeAtuacao,
        boolean exigeCiencia,
        boolean processoSigiloso
) {
}
