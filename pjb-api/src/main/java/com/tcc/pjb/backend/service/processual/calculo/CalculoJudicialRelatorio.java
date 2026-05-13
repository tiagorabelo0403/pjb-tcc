package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialRelatorio(
        String dominio,
        String titulo,
        String numeroProcesso,
        CalculoJudicialSolicitantePerfil perfilSolicitante,
        String narrativaCidadao,
        String narrativaTecnica,
        BigDecimal subtotalPrincipal,
        BigDecimal subtotalAtualizacao,
        BigDecimal subtotalAcessorios,
        BigDecimal totalGeral,
        List<CalculoJudicialLinha> itens,
        List<String> alertas,
        List<String> fundamentos,
        List<String> trilhaAuditoria,
        Map<String, Object> metadata,
        Instant geradoEm
) {
}
