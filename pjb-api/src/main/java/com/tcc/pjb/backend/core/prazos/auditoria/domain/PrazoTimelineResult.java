package com.tcc.pjb.backend.core.prazos.auditoria.domain;

import java.util.List;

public record PrazoTimelineResult(Long processoId, String eventoRef, List<PrazoTimelineEntry> entries) {}
