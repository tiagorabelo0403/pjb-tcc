package com.tcc.pjb.backend.model.dto.oficial_justica;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OficialJusticaBalcaoVirtualMessageRequest(
        @NotBlank @Size(max = 5000) String conteudo,
        boolean urgente,
        boolean sigiloso,
        String categoriaDestino,
        String assuntoOperacional
) {
}
