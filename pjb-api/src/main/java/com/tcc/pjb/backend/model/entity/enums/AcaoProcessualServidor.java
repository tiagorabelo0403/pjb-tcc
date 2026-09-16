package com.tcc.pjb.backend.model.entity.enums;

public enum AcaoProcessualServidor {
    PROFERIR,
    CONCLUIR,
    INTIMAR,
    DISTRIBUIR,
    ARQUIVAR;

    public boolean permiteExecutarPor(FuncaoServidorJudiciario funcao) {
        return switch (this) {
            case PROFERIR -> funcao.podeProferir();
            case CONCLUIR -> funcao.podeConcluir();
            case INTIMAR -> funcao.podeIntimar();
            case DISTRIBUIR -> funcao.podeDistribuir();
            case ARQUIVAR -> funcao.podeArquivar();
        };
    }
}
