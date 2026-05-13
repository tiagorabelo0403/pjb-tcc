package com.tcc.pjb.backend.model.dto.intelligence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RitoPlanRequest(
        @NotBlank(message = "rito é obrigatório")
        @Size(max = 80, message = "rito muito longo")
        String rito
) {}
