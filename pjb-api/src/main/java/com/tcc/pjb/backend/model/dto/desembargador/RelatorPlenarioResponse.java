package com.tcc.pjb.backend.model.dto.desembargador;

import java.time.LocalDateTime;
import java.util.List;

public record RelatorPlenarioResponse(
        Long sessaoId,
        Long processoId,
        String numeroProcesso,
        String tribunalSigla,
        String orgaoJulgador,
        String relatorNome,
        String status,
        List<RelatorPlenarioVoteDto> votos,
        List<RelatorPlenarioDivergenciaDto> divergencias,
        String ementaSugerida,
        RelatorPlenarioResultadoDto resultado,
        String minutaAcordaoParcial,
        LocalDateTime atualizadoEm
) {
}
