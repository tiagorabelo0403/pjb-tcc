package com.tcc.pjb.backend.model.dto.secretariat;

import jakarta.validation.constraints.NotBlank;

public record AdicionarAbrangenciaRequest(@NotBlank String comarcaAtendida) {
}
