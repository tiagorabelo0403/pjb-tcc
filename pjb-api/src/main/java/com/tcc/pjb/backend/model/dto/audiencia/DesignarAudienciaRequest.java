package com.tcc.pjb.backend.model.dto.audiencia;

import com.tcc.pjb.backend.model.entity.enums.ModalidadeAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record DesignarAudienciaRequest(
        @NotNull Long processoId,
        @NotNull TipoAudiencia tipo,
        @NotNull ModalidadeAudiencia modalidade,
        @NotNull @Future @Schema(description = "Data e hora da audiência (deve ser futura)", format = "date-time",
                example = "2026-07-15T14:00:00-03:00") LocalDateTime dataHora,
        @Positive Integer duracaoMinutos,
        @Size(max = 260) String local,
        @Size(max = 600) String linkVideo,
        @Size(max = 4000) String pauta,
        @NotNull @Size(max = 255) String designadaPor
) {
}
