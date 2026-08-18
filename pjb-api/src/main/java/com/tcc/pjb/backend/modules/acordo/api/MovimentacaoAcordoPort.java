package com.tcc.pjb.backend.modules.acordo.api;

public interface MovimentacaoAcordoPort {

    void registrarSalaAberta(MovimentacaoAcordoCommand command);

    void registrarTermoEnviadoHomologacao(MovimentacaoAcordoCommand command);

    void registrarHomologacao(MovimentacaoAcordoCommand command);

    void registrarRejeicaoHomologacao(MovimentacaoAcordoCommand command);

    void registrarEncerramentoSemAcordo(MovimentacaoAcordoCommand command);
}
