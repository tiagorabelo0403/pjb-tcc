package com.tcc.pjb.backend.model.dto.cidadao.govbr;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoGovBrAcessoFederadoResponse(
        LocalDateTime generatedAt,
        String sistemaOrigem,
        String sistemaLabel,
        String tribunalCodigo,
        String numeroProcesso,
        String modoDescoberta,
        String modoAcesso,
        String modoDocumento,
        String modoSincronizacao,
        boolean discoveryAllowed,
        boolean capaAllowed,
        boolean timelineAllowed,
        boolean documentosAllowed,
        boolean midiaAllowed,
        boolean exigeStepUp,
        boolean exigeCredencialInstitucional,
        boolean transporteSeguroPronto,
        boolean proxySoberanoElegivel,
        String decisaoAcesso,
        List<String> alertas,
        List<String> bloqueios,
        Links links
) {
    public record Links(
            String detailUrl,
            String sourceBridgeUrl,
            String sourceSystemUrl,
            String stepUpUrl,
            String authenticityUrl
    ) {
    }
}
