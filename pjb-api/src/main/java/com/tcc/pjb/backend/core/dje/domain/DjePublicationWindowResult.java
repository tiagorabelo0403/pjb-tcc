package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjePublicationWindowResult(String tribunalCodigo, LocalDate hoje, int batchSize, boolean enabled) {}
