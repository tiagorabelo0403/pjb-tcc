package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.math.BigDecimal;

public record AdvogadoHonorariosResponse(
        Long processoId,
        String numeroProcesso,
        BigDecimal percentualAplicado,
        BigDecimal valorHonorarios,
        String fundamentacao
) {}
