package com.tcc.pjb.backend.model.dto.desembargador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;

public record DesembargadorPlenarioVotoRequest(
        @NotNull Integer ordem,
        @NotBlank String papel,
        @NotNull TipoVotoColegiado votoTipo,
        @NotBlank String votoResumo,
        String documentoRef
) {
}
