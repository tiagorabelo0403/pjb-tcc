package com.tcc.pjb.backend.service.processual.note;

public enum ProcessoNoteType {
    ANOTACAO,
    LEMBRETE,
    PENDENCIA,
    COMPROMISSO,
    ALERTA_OPERACIONAL;

    public boolean requerConclusao() {
        return this == PENDENCIA || this == COMPROMISSO;
    }

    public boolean eVisivelParaTodos() {
        return this == ALERTA_OPERACIONAL;
    }
}
