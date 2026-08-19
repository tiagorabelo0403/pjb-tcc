package com.tcc.pjb.backend.model.dto.conclusao;

import com.tcc.pjb.backend.model.entity.processo.ConclusaoProcessual;
import java.time.Instant;

public record ConclusaoProcessualResponse(
        Long id,
        Long processoId,
        Long magistradoId,
        Long servidorId,
        String tipoConclusao,
        String motivo,
        Instant dataConclusao,
        Instant dataLimite,
        String status
) {
    public static ConclusaoProcessualResponse of(ConclusaoProcessual conclusao) {
        return new ConclusaoProcessualResponse(
                conclusao.getId(),
                conclusao.getProcessoId(),
                conclusao.getMagistradoId(),
                conclusao.getServidorId(),
                conclusao.getTipoConclusao(),
                conclusao.getMotivo(),
                conclusao.getDataConclusao(),
                conclusao.getDataLimite(),
                conclusao.getStatus());
    }
}
