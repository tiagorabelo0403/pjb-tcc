package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjeNotificarPartesCommand(LocalDate hoje, int batchSize) {
}
