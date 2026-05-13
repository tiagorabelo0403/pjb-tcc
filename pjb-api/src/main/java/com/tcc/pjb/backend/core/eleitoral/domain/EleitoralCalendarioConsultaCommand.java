package com.tcc.pjb.backend.core.eleitoral.domain;
import java.time.LocalDate;
public record EleitoralCalendarioConsultaCommand(String uf, LocalDate data) {}
