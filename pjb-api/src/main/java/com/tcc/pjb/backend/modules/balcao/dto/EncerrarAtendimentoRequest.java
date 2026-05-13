package com.tcc.pjb.backend.modules.balcao.dto;

import jakarta.validation.constraints.Size;

public record EncerrarAtendimentoRequest(
        @Size(max = 2000) String observacoes
) {
}
