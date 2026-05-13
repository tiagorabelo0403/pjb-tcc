package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.util.List;

public record DigitalizacaoTimelineResult(Long jobId, List<DigitalizacaoTimelineEntry> entries) {}
