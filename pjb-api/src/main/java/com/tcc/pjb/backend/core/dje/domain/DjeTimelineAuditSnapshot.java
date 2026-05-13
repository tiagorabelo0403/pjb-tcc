package com.tcc.pjb.backend.core.dje.domain;

public record DjeTimelineAuditSnapshot(Long djeId, int eventos, boolean publicado, boolean partesNotificadas) {}
