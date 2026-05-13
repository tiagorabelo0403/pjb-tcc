package com.tcc.pjb.backend.core.prazos.auditoria.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import java.time.LocalDateTime;

public record PrazoAuditQuery(Long processoId,
                              String eventoRef,
                              int quantidadeSolicitada,
                              PrazoRegime regimeAplicado,
                              LocalDateTime inicio,
                              LocalDateTime fim,
                              String uf,
                              String comarca) {

    public PrazoAuditQuery(Long processoId, String eventoRef) {
        this(processoId, eventoRef, 0, PrazoRegime.UTEIS, null, null, null, null);
    }
}
