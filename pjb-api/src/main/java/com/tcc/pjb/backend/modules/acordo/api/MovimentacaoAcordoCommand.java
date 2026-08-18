package com.tcc.pjb.backend.modules.acordo.api;

public record MovimentacaoAcordoCommand(
        Long processoId,
        String tipo,
        String descricao,
        Long operadorId,
        String origem
) {
}
