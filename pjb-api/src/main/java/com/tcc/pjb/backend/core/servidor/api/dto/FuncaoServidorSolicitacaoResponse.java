package com.tcc.pjb.backend.core.servidor.api.dto;

import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorSolicitacao;
import java.time.Instant;

public record FuncaoServidorSolicitacaoResponse(
        Long id,
        Long solicitanteId,
        Long unidadeId,
        String funcao,
        String motivo,
        String status,
        Instant requestedAt,
        Long decididoPorId,
        Instant decididoEm,
        String motivoRejeicao
) {
    public static FuncaoServidorSolicitacaoResponse from(FuncaoServidorSolicitacao s) {
        return new FuncaoServidorSolicitacaoResponse(s.getId(), s.getSolicitanteId(), s.getUnidadeId(),
                s.getFuncao().name(), s.getMotivo(), s.getStatus().name(), s.getRequestedAt(),
                s.getDecididoPorId(), s.getDecididoEm(), s.getMotivoRejeicao());
    }
}
