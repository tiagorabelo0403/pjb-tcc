package com.tcc.pjb.backend.core.dje.domain;

public record DjeFailureAuditSnapshot(Long djeId, String status, String failureReason) {}
