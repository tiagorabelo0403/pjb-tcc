package com.tcc.pjb.backend.core.digitalizacao.domain;
public record DigitalizacaoQueueHealthView(Long jobId, String status, boolean queueHealthy) {}
