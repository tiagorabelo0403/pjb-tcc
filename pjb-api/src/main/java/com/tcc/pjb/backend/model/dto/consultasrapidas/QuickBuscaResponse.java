package com.tcc.pjb.backend.model.dto.consultasrapidas;

import java.util.List;

public record QuickBuscaResponse(
        List<QuickProcessoResumoDTO> itens,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
