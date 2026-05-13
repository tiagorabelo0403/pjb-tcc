package com.tcc.pjb.backend.core.prazos.calendario.domain;

import java.time.LocalDate;

public record PrazoFeriadoWindow(LocalDate inicio, LocalDate fim, String uf, String comarca) {}
