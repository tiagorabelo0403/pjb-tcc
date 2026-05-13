package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record BnmpConsultaResult(boolean mandadoAtivo, String numeroMandado) {
    public BnmpConsultaResult(boolean mandadoAtivo, String numeroMandado, Instant ignored) {
        this(mandadoAtivo, numeroMandado);
    }

    public static BnmpConsultaResult vazio() {
        return new BnmpConsultaResult(false, null);
    }
}
