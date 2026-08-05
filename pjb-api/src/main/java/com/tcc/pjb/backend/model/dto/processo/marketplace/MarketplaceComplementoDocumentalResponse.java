package com.tcc.pjb.backend.model.dto.processo.marketplace;

import java.time.LocalDateTime;
import java.util.List;

public record MarketplaceComplementoDocumentalResponse(
        Long processoId,
        String numeroProcesso,
        String status,
        boolean documentacaoCompleta,
        List<String> documentosFaltantes,
        List<String> documentosRecebidos,
        LocalDateTime recebidoEm
) {
}
