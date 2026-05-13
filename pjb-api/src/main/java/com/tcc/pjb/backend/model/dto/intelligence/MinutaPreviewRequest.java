package com.tcc.pjb.backend.model.dto.intelligence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MinutaPreviewRequest(
        @NotBlank(message = "resumo é obrigatório")
        @Size(max = 12000, message = "resumo muito longo (máx 12000 caracteres)")
        String resumo,

        
        @NotBlank(message = "template é obrigatório")
        String template,

        
        String materia,

        
        String orgao,

        
        String rito,

        
        String uf,

        
        String comarca
) {
}
