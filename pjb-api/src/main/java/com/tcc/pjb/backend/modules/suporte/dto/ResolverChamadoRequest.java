package com.tcc.pjb.backend.modules.suporte.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolverChamadoRequest(
        @NotBlank String resposta,
        boolean aprovado
) {}
