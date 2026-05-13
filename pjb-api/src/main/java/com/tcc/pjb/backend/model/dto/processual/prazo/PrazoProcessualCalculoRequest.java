package com.tcc.pjb.backend.model.dto.processual.prazo;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;

public record PrazoProcessualCalculoRequest(
        @NotNull LocalDate dataInicio,
        @NotNull NationalPrazoEngine.TipoPrazo tipoPrazo,
        @NotNull RamoDireito ramo,
        @NotNull GrauJurisdicao grau,
        @NotBlank @Size(max = 16) String tribunalCodigo,
        @Size(min = 2, max = 2) String uf,
        @Size(max = 120) String comarca,
        @Positive Integer diasOverride) {
}
