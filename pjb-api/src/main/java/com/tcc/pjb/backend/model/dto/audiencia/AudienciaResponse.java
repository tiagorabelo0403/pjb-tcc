package com.tcc.pjb.backend.model.dto.audiencia;

import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.enums.ModalidadeAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;

import java.time.LocalDateTime;

public record AudienciaResponse(
        Long id,
        Long processoId,
        String numeroProcesso,
        String vara,
        TipoAudiencia tipo,
        ModalidadeAudiencia modalidade,
        StatusAudiencia status,
        LocalDateTime dataHora,
        Integer duracaoMin,
        String local,
        String linkVideo,
        String pauta,
        String notas,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
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
