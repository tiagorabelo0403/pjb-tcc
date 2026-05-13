package com.tcc.pjb.backend.core.prazos.auditoria.domain;

import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrail;

public record PrazoAuditResult(PrazoAuditTrail auditTrail, boolean encontrado) {

    public PrazoAuditTrail trail() {
        return auditTrail;
    }
}
