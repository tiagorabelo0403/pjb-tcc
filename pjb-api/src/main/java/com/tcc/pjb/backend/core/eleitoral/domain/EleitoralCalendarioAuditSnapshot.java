package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralCalendarioAuditSnapshot(String uf, LocalDate data, boolean emJanela, String status) {}
