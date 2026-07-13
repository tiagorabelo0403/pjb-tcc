package com.tcc.pjb.backend.model.dto.processo.marketplace;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record MarketplaceProtocoloRequest(
        @NotBlank String clientReference,
        @NotBlank String numeroExterno,
        String tipoJustica,
        String ramoDireito,
        @NotBlank String uf,
        String comarca,
        @NotBlank String classeProcessual,
        String assunto,
        @NotBlank String pedidoPrincipal,
        String pedidosConsolidados,
        @NotBlank String parteAutoraNome,
        String parteAutoraCpf,
        String parteReuNome,
        String parteReuCpf,
        BigDecimal valorCausa
) {
}
