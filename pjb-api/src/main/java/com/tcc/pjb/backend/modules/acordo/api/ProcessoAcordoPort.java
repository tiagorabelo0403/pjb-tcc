package com.tcc.pjb.backend.modules.acordo.api;

public interface ProcessoAcordoPort {

    boolean existeProcesso(Long processoId);

    ProcessoAcordoContexto obterContextoProcessual(Long processoId);

    boolean processoEstaEmSegredo(Long processoId);

    void registrarMovimentacaoAcordo(Long processoId, String tipo, String descricao);
}
