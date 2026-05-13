package com.tcc.pjb.backend.core.eleitoral.domain;
public record EleitoralTimelineAuditSnapshot(Long processoId, int totalEventos, boolean diplomado, boolean extinto) {}
