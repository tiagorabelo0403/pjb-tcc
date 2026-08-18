package com.tcc.pjb.backend.model.dto.audiencia;

import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.enums.ModalidadeAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AudienciaResponse(
        Long id,
        Long processoId,
        String numeroProcesso,
        String vara,
        TipoAudiencia tipo,
        ModalidadeAudiencia modalidade,
        StatusAudiencia status,
        @Schema(description = "Data e hora da audiência", format = "date-time",
                example = "2026-06-01T14:00:00-03:00") LocalDateTime dataHora,
        Integer duracaoMin,
        String local,
        String linkVideo,
        String pauta,
        String notas,
        @Schema(description = "Data/hora de criação do registro da audiência", format = "date-time",
                example = "2026-06-01T10:00:00-03:00") LocalDateTime criadoEm,
        @Schema(description = "Data/hora da última atualização da audiência", format = "date-time",
                example = "2026-06-01T10:05:00-03:00") LocalDateTime atualizadoEm
) {
    public static AudienciaResponse de(Audiencia a) {
        String vara = a.getProcesso() != null ? a.getProcesso().getVara() : null;
        String numero = a.getProcesso() != null ? a.getProcesso().getNumeroUnificado() : null;
        return new AudienciaResponse(
                a.getId(),
                a.getProcesso() != null ? a.getProcesso().getId() : null,
                numero, vara,
                a.getTipo(), a.getModalidade(), a.getStatus(),
                a.getDataHora(), a.getDuracaoMin(), a.getLocal(),
                a.getLinkVideo(), a.getPauta(), a.getNotas(),
                a.getCriadoEm(), a.getAtualizadoEm()
        );
    }
}
