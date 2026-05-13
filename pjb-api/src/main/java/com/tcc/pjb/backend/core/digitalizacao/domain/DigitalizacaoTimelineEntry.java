package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.time.Instant;

public record DigitalizacaoTimelineEntry(String etapa, Instant quando, String detalhe) {}
