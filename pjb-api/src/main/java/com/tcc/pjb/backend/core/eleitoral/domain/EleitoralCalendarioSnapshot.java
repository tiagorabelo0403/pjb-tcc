package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralCalendarioSnapshot(String uf,
                                          LocalDate dataInicio,
                                          LocalDate dataFim,
                                          String fase) {
}
