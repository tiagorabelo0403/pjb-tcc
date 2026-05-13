package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialResumoResponse(
        String dominio,
        String titulo,
        String numeroProcesso,
        CalculoJudicialSolicitantePerfil perfilSolicitante,
        String narrativaPrincipal,
        String narrativaTecnica,
        BigDecimal subtotalPrincipal,
        BigDecimal subtotalAtualizacao,
        BigDecimal subtotalAcessorios,
        BigDecimal totalGeral,
        List<CalculoJudicialItemResponse> itens,
        List<String> alertas,
        List<String> fundamentos,
        List<String> trilhaAuditoria,
        Map<String, Object> metadata,
        Instant geradoEm
) {
}
