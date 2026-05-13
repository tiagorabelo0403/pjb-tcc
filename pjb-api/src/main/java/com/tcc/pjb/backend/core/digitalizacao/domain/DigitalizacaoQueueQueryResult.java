package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.util.List;

public record DigitalizacaoQueueQueryResult(List<DigitalizacaoReviewQueueEntry> entries,
                                            int total) {}
