package com.tcc.pjb.backend.core.prazos.calendario.domain;

import java.time.LocalDate;

public record PrazoFeriadoBloqueioView(LocalDate dia, String tipo, boolean bloqueado) {}
