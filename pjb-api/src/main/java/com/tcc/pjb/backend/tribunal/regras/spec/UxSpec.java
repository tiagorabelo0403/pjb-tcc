package com.tcc.pjb.backend.tribunal.regras.spec;


    public record UxSpec(
            Boolean exibeNupPadrao,
            Boolean exibeQrCodeNosDocumentos,
            String formatoNumeroLocal,
            Boolean habilitaChatProcesso,
            Boolean habilitaVideoAudiencia,
            Boolean habilitaAssinaturaDigital,
            Boolean habilitaNotificacaoWhatsApp,
            Boolean habilitaProcessoFisico,
            String fusoHorario,
            String formatoData,
            String moeda,
            Integer itensPorPaginaPadrao,
            Boolean modoEscuroDisponivel,
            Boolean exibeCalculadoraPrazos,
            Boolean vlibras,
            String nivelConformeWcag
    ) {}
