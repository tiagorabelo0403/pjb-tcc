package com.tcc.pjb.backend.core.servidor.api.dto;

import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import java.time.LocalDate;

public record FuncaoServidorDesignacaoResponse(
        Long id,
        Long usuarioId,
        Long unidadeId,
        String funcao,
        LocalDate dataInicio,
        boolean ativo
) {
    public static FuncaoServidorDesignacaoResponse from(FuncaoServidorJudiciarioEntity entidade) {
        return new FuncaoServidorDesignacaoResponse(entidade.getId(), entidade.getUsuarioId(), entidade.getUnidadeId(),
                entidade.getFuncao().name(), entidade.getDataInicio(), entidade.isAtivo());
    }
}
