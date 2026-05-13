package com.tcc.pjb.backend.model.dto.pericia;

import java.time.LocalDate;
import java.util.List;

public record PeritoSorteioResponse(
        Long peritoId,
        String peritoNome,
        String especialidadeCodigo,
        String comarca,
        LocalDate data,
        double score,
        long nomeacoesAtivas,
        List<String> fundamentos
) {
}
