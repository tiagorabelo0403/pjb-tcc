package com.tcc.pjb.backend.model.entity.enums;

public enum OperacaoProcessualCritica {
    MINUTAR,
    REVISAR,
    ASSINAR,
    CONTRASSINAR,
    HOMOLOGAR,
    CUMPRIR,
    CERTIFICAR,
    AUDITAR,
    PUBLICAR,
    CONSULTAR_SENSIVEL;

    public boolean exigeAssinatura() {
        return this == ASSINAR || this == CONTRASSINAR || this == HOMOLOGAR || this == PUBLICAR;
    }

    public boolean exigeDuplaValidacao() {
        return this == CONTRASSINAR || this == AUDITAR || this == CONSULTAR_SENSIVEL;
    }
}
