package com.tcc.pjb.backend.platform.jusos.v2.conciliacao;

import java.time.Instant;

record CachedResultadoRegistro(
        CejuscEngine.ResultadoRegistro value,
        Instant expiresAt,
        Instant touchedAt) {
    boolean expired(Instant now) {
        return now != null && expiresAt != null && !expiresAt.isAfter(now);
    }
}
