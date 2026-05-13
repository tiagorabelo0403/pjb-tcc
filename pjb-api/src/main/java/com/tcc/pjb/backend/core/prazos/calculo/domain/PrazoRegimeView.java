package com.tcc.pjb.backend.core.prazos.calculo.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;

public record PrazoRegimeView(PrazoRegime regime, String descricao) {
    public String label() { return regime == null ? null : regime.name(); }
}
