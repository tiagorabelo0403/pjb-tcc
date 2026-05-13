package com.tcc.pjb.backend.model.dto.processual.linkage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.enums.VinculoProcessualTipo;

public record ProcessoLinkageApplyRequest(
        @NotNull Long processoId,
        @NotNull Long processoRelacionadoId,
        @NotNull VinculoProcessualTipo vinculoTipo,
        @NotBlank String justificativa) {
}
