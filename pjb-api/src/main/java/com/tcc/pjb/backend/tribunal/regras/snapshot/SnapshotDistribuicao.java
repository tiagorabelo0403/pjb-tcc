package com.tcc.pjb.backend.tribunal.regras.snapshot;

import java.math.BigDecimal;
import java.time.Instant;

public record SnapshotDistribuicao(
        BigDecimal limiteJuizadoSalariosMinimos,
        BigDecimal limiteJuizadoReais,
        BigDecimal limiarCongestionamento,
        int capacidadeMaximaVara,
        Instant geradoEm
) {}
