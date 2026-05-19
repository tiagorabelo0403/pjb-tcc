package com.tcc.pjb.backend.model.dto.acordo;

import java.time.Instant;

public record ChatAcordoAbrirSalaRequest(
        Instant expiraEm,
        String motivoAbertura,
        boolean propostaFormalExistente,
        boolean requerimentoParte,
        boolean determinacaoJudicial,
        boolean cejuscReferenciado,
        boolean parteSemAdvogado
) {
}
