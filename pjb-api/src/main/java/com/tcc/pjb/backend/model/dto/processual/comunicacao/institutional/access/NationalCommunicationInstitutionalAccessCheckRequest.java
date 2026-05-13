package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;

public record NationalCommunicationInstitutionalAccessCheckRequest(
        @NotBlank String unidadeCodigo,
        @NotBlank String caixaCodigo,
        @NotNull CapacidadeCaixaInstitucional capacidade) {
}
