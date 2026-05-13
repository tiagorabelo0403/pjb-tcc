package com.tcc.pjb.backend.model.dto.processual.pendencia;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OperationalPendingDashboardResponse(
        Long usuarioId,
        String perfil,
        String uf,
        String comarca,
        long totalAbertasUsuario,
        long totalAbertasFila,
        int totalNaAmostra,
        long vencidasNaAmostra,
        long criticas24hNaAmostra,
        long bloqueantesNaAmostra,
        int totalProcessosAfetadosNaAmostra,
        Map<String, Long> porTipo,
        Map<String, Long> porStatus,
        Map<String, Long> porFila,
        List<OperationalPendingItemResponse> itens,
        Instant geradoEm) {
}
