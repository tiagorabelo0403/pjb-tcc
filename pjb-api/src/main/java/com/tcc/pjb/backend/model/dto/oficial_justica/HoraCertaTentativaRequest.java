package com.tcc.pjb.backend.model.dto.oficial_justica;

import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoHoraCertaEngine;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record HoraCertaTentativaRequest(
        @NotNull Long processoId,
        @Min(1) int numeroTentativa,
        double latitude,
        double longitude,
        String enderecoConfirmado,
        @NotEmpty List<CitacaoHoraCertaEngine.EvidenciaMorfologica> evidencias,
        String observacoes,
        boolean destinatarioAusente
) {
}
