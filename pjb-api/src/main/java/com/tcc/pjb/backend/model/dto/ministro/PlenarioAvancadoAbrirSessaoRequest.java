package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record PlenarioAvancadoAbrirSessaoRequest(
        @NotBlank String orgaoJulgador,
        String materiaResumo,
        String observacoes,
        Integer quorumMinimo,
        boolean segredoAteProclamacao
) {
}
