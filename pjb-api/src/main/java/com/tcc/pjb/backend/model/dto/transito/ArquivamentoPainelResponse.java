package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;

public record ArquivamentoPainelResponse(
        String vara,
        long total,
        List<ArquivamentoCandidatoResponse> candidatos
) {}
