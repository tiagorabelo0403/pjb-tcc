package com.tcc.pjb.backend.model.dto.criminal;

import java.time.LocalDate;

public record InqueritoMovimentacaoRequest(
        String status,
        String faseAtual,
        String diligenciasPendentes,
        String indiciosResumo,
        String ultimaMovimentacaoResumo,
        LocalDate prazoConclusao,
        boolean remeterAoMinisterioPublico,
        boolean encaminharAoJudiciario
) {
}
