package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

public record CalculoJudicialAjuizamentoSignalRequest(
        @Size(max = 12000) String textoPeticao,
        @Size(max = 80) String dominioPreferencial,
        @Size(max = 80) String ramoDireito,
        BigDecimal valorDaCausaInformado,
        BigDecimal valorPedidosSomados,
        BigDecimal valorLiquidoPretendido,
        BigDecimal honorariosInformados,
        BigDecimal multasInformadas,
        Integer quantidadePedidos,
        Boolean possuiCalculosAnexos,
        Boolean possuiPedidosVincendos,
        Map<String, Object> payloadCalculo
) {
}
