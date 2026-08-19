package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.util.List;

public record SecretariatProdutividadePainelResponse(
        String inboxKey,
        int diasJanela,
        long totalConcluidos,
        List<SecretariatProdutividadeItemResponse> ranking
) {}
