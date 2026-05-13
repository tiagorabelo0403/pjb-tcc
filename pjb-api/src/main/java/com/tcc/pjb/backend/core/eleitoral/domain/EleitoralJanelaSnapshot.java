package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record EleitoralJanelaSnapshot(String uf,
                                      LocalDate data,
                                      boolean naJanela) {}
