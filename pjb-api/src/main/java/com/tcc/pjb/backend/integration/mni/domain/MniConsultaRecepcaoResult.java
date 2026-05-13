package com.tcc.pjb.backend.integration.mni.domain;
public record MniConsultaRecepcaoResult(MniRecepcaoProjection recepcao, MniRecepcaoAuditSnapshot audit) {
    public MniRecepcaoProjection projection() { return recepcao; }
}
