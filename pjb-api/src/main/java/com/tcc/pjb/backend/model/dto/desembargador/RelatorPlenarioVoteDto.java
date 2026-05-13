package com.tcc.pjb.backend.model.dto.desembargador;

import java.time.LocalDateTime;

public record RelatorPlenarioVoteDto(
        Long votoId,
        Integer ordem,
        String magistradoNome,
        String magistradoCargo,
        String papel,
        String votoTipo,
        String votoResumo,
        LocalDateTime proferidoEm,
        boolean divergente
) {
}
