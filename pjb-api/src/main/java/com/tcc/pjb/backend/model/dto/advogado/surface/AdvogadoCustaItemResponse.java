package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AdvogadoCustaItemResponse(
        Long custaId,
        String tipo,
        BigDecimal valor,
        String status,
        LocalDate vencimento,
        Instant pagoEm,
        BigDecimal valorPago
) {}
