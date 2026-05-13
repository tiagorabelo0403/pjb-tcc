package com.tcc.pjb.backend.model.dto.processual.pauta;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record PautaAudienciaRequest(
        @NotNull Long usuarioId,
        Long processoId,
        @NotBlank @Size(max = 16) String tribunalCodigo,
        @Size(min = 2, max = 2) String uf,
        @Size(max = 120) String comarca,
        @NotNull RamoDireito ramo,
        @NotNull GrauJurisdicao grau,
        @NotNull LocalDateTime inicio,
        @Positive Integer duracaoMinutos,
        @Size(max = 60) String tipo,
        @Size(max = 160) String local,
        @Size(max = 255) String detailsUrl) {
}
