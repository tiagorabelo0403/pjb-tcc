package com.tcc.pjb.backend.ai.legalai.dto;

import jakarta.validation.constraints.NotBlank;

public record MemoryStoreCreateRequest(
        @NotBlank String nome,
        String descricao,
        @NotBlank String moduloOrigem,
        @NotBlank String sigiloNivel
) {}
