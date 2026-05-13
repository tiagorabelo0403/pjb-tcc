package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCoberturaOperacionalInstitucional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;

public record NationalCommunicationInstitutionalCoverageCreateRequest(
        @NotBlank String unidadeCodigo,
        @NotBlank String caixaCodigo,
        @NotNull Long titularUsuarioId,
        @NotNull Long coberturaUsuarioId,
        @NotNull TipoCoberturaOperacionalInstitucional tipoCobertura,
        Set<CapacidadeCaixaInstitucional> capacidades,
        Instant inicioVigencia,
        Instant fimVigencia,
        String motivo,
        String observacoes
) {
}
