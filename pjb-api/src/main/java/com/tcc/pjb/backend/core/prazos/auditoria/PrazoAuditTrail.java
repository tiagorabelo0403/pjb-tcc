package com.tcc.pjb.backend.core.prazos.auditoria;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import java.time.Instant;
import java.time.LocalDateTime;

public record PrazoAuditTrail(
        Long processoId,
        String eventoRef,
        int quantidadeSolicitada,
        PrazoRegime regimeAplicado,
        LocalDateTime inicio,
        LocalDateTime fim,
        String uf,
        String comarca,
        long totalFeriadosBloqueados,
        String calendarioVersaoHash,
        Instant calculadoEm) {
}
