package com.tcc.pjb.backend.ai.legalai.dto;

import java.time.Instant;

public record MemoryStoreResponse(
        String id,
        String nome,
        String descricao,
        String anthropicStoreId,
        String moduloOrigem,
        String sigiloNivel,
        String accessType,
        Instant criadoEm,
        Instant arquivadoEm,
        boolean ativo
) {}
