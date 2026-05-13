package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record NationalCommunicationInstitutionalDelegateRequest(
        @NotBlank String expedicaoUuid,
        @NotNull Long delegadoUsuarioId,
        Set<CapacidadeCaixaInstitucional> capacidades,
        Integer horasVigencia,
        String motivo
) {
}
