package com.tcc.pjb.backend.modules.advocacia.office.enums;

public enum OfficeActionType {
    PROTOCOL_SUBMIT_PJE(false, true),
    PETICIONAR(false, true),
    RECORRER(false, true),
    JUNTAR_DOCUMENTO(false, true),
    ASSINAR_DOCUMENTO(true, true),
    ASSINAR_ACORDO(true, true),
    DESISTIR(true, true),
    RENUNCIAR(true, true),
    CONFESSAR(true, true);

    private final boolean irreversivel;
    private final boolean patronalQueueCandidate;

    OfficeActionType(boolean irreversivel, boolean patronalQueueCandidate) {
        this.irreversivel = irreversivel;
        this.patronalQueueCandidate = patronalQueueCandidate;
    }

    public boolean isIrreversivel() {
        return irreversivel;
    }

    public boolean patronalQueueCandidate() {
        return patronalQueueCandidate;
    }
}
