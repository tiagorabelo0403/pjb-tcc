package com.tcc.pjb.backend.service.execucaopenal;

public enum RegimePrisionalTipo {
    FECHADO,
    SEMIABERTO,
    ABERTO,
    ALBERGUE_DOMICILIAR,
    REGIME_CAUTELAR;

    public boolean isMaisGraveQue(RegimePrisionalTipo outro) {
        return this.ordinal() < outro.ordinal();
    }

    public RegimePrisionalTipo proximo() {
        return switch (this) {
            case FECHADO -> SEMIABERTO;
            case SEMIABERTO -> ABERTO;
            case ABERTO -> ALBERGUE_DOMICILIAR;
            default -> this;
        };
    }
}
