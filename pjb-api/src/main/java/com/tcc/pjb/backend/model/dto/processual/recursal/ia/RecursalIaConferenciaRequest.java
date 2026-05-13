package com.tcc.pjb.backend.model.dto.processual.recursal.ia;

import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RecursalIaConferenciaRequest(
        @Valid @NotNull RecursalAdmissibilityRequest admissibilidade,
        String pedidoUsuario,
        boolean executarAdmissibilidadeReal,
        boolean exigirConferenciaPreparo,
        boolean exigirConferenciaTempestividade,
        boolean exigirConferenciaCompetencia,
        Long processoId,
        String tipoRecursoInformado,
        String ramoSugerido,
        String ritoSugerido,
        boolean aprofundarBaseProcessual,
        boolean aprofundarJurisprudencia,
        boolean aprofundarJurimetria,
        boolean considerarHistoricoPericial,
        boolean exigirBlindagemAnulacao
) {
}
