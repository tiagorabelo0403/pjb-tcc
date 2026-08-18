package com.tcc.pjb.backend.modules.custas.domain;

public record IsencaoCustaResult(boolean isento, String motivo) {
    public static IsencaoCustaResult naoIsento() {
        return new IsencaoCustaResult(false, null);
    }

    public static IsencaoCustaResult isento(String motivo) {
        return new IsencaoCustaResult(true, motivo);
    }
}
