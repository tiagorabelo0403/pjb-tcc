package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjeLifecycleCommand(LocalDate hoje, int batchSize) {
}
