package com.tcc.pjb.backend.modules.acordo.api;

public interface MovimentacaoAcordoPort {

    void registrarHomologacao(Long processoId, Long magistradoId, String descricao);

    void registrarEncerramentoSemAcordo(Long processoId, Long usuarioId, String descricao);
}
