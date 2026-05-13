package com.tcc.pjb.backend.core.financeiro.custas.domain;

import java.time.LocalDate;

public record CustaVencimentoSnapshot(Long custaId, LocalDate vencimento, boolean vencida) {}
