package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.math.BigDecimal;
import java.util.List;

public record AdvogadoPainelFinanceiroResponse(
        Long processoId,
        List<AdvogadoCustaItemResponse> custas,
        int quantidadeCustas,
        int quantidadeCustasPendentes,
        int quantidadeCustasPagas,
        BigDecimal totalCustasPendentes,
        BigDecimal totalCustasPagas
) {}
