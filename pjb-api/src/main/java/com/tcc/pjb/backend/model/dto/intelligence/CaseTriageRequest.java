package com.tcc.pjb.backend.model.dto.intelligence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaseTriageRequest(
        @NotBlank(message = "resumo é obrigatório")
        @Size(max = 12000, message = "resumo muito longo (máx 12000 caracteres)")
        String resumo,

        @Size(max = 400, message = "materia muito longa")
        String materia,

        @Size(max = 200, message = "orgao muito longo")
        String orgao,

        @Size(max = 80, message = "pais muito longo")
        String pais,

        @Size(max = 200, message = "tratado muito longo")
        String tratado,

        @Size(min = 2, max = 2, message = "uf deve ter 2 letras")
        String uf,

        @Size(max = 120, message = "comarca muito longa")
        String comarca,

        
        String rito,

        
        Integer topK
) {
}
