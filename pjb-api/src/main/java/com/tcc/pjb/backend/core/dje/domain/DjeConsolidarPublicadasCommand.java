package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjeConsolidarPublicadasCommand(LocalDate hoje, int batchSize) {
}
