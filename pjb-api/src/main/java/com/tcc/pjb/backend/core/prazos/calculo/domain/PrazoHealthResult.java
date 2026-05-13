package com.tcc.pjb.backend.core.prazos.calculo.domain;

import java.time.Instant;

public record PrazoHealthResult(String uf, String comarca, boolean calendarioDisponivel, Instant checkedAt) {
    public boolean healthy() { return calendarioDisponivel; }
}
