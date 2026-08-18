package com.tcc.pjb.backend.modules.custas.domain;

import java.time.LocalDate;

public record CustaVencimentoSnapshot(Long custaId, LocalDate vencimento, boolean vencida) {}
