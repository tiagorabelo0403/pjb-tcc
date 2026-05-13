package com.tcc.pjb.backend.model.dto.publico;

import java.util.List;

public record PublicProcessoResumoSearchResponse(
        String query,
        String identityKey,
        int page,
        int size,
        long total,
        String matchMode,
        String queryMasked,
        List<PublicProcessoResumoCardDto> processos
) {
}
