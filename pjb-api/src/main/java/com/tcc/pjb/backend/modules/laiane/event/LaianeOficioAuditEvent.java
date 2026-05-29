package com.tcc.pjb.backend.modules.laiane.event;

public record LaianeOficioAuditEvent(
        String acao,
        String referenciaId,
        String detalhes,
        String justificativa
) {}
