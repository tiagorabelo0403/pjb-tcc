package com.tcc.pjb.backend.core.financeiro.custas.domain;

import java.util.List;

public record CustaConsultaTimelineResult(Long custaId, List<CustaTimelineEntry> eventos) {

    public List<CustaTimelineEntry> entries() {
        return eventos;
    }
}
