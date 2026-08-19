package com.tcc.pjb.backend.model.dto.secretariat.queue;

public record SecretariatProdutividadeItemResponse(
        Long servidorId,
        String servidorNome,
        long totalConcluidos,
        Double duracaoMediaHoras
) {}
