package com.tcc.pjb.backend.model.dto.processo.marketplace;

import java.time.LocalDateTime;
import java.util.List;

public record MarketplaceProtocoloResponse(
        Long processoId,
        String numeroProcesso,
        String protocoloMarketplace,
        String status,
        String tipoJustica,
        String ramoDireito,
        LocalDateTime recebidoEm,
        boolean documentacaoCompleta,
        List<String> documentosFaltantes
) {
}
