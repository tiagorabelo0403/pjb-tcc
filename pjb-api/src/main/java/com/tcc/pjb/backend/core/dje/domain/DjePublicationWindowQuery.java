package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjePublicationWindowQuery(String tribunalCodigo, LocalDate hoje, int batchSize) {}
