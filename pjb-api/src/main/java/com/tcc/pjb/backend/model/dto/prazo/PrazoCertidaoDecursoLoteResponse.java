package com.tcc.pjb.backend.model.dto.prazo;

import java.util.List;

public record PrazoCertidaoDecursoLoteResponse(
        String vara,
        int totalCertificadas,
        List<PrazoCertidaoDecursoItemResponse> certidoes
) {}
