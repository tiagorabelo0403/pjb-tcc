package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoQueryResult(MniRecepcaoProjection projection,
                                     MniRecepcaoAuditSnapshot audit,
                                     MniEnvelopeSnapshot envelope) {}
