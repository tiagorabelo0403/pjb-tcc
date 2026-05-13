package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoTimelineHealthView(
        String codigoTema,
        long totalEventos,
        boolean healthy,
        String summary
) {}
