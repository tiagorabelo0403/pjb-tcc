package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoAuditHealthView(String calendarioVersaoHash, long totalBloqueios) {
    public long totalFeriadosBloqueados() { return totalBloqueios; }
}
