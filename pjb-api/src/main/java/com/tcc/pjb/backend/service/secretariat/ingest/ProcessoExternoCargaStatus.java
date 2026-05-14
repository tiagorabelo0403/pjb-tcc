package com.tcc.pjb.backend.service.secretariat.ingest;

public enum ProcessoExternoCargaStatus {
    PENDENTE_RECEBIMENTO,
    RECEBIDO_MANUALMENTE,
    RECEBIDO_AUTOMATICAMENTE,
    IMPORTADO,
    REJEITADO,
    COM_DIVERGENCIA,
    CONSOLIDADO;

    public boolean eAtivo() {
        return this == PENDENTE_RECEBIMENTO
                || this == RECEBIDO_MANUALMENTE
                || this == RECEBIDO_AUTOMATICAMENTE;
    }

    public boolean eTerminal() {
        return this == IMPORTADO || this == REJEITADO || this == CONSOLIDADO;
    }
}
