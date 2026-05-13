package com.tcc.pjb.backend.integration.mni.domain;

public record MniConsultaRemessaResult(MniRemessaProjection remessa,
                                       MniRemessaAuditSnapshot audit) {
}
