package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudFeedHealthSnapshot(String tribunalCodigo,
                                        boolean enabled,
                                        long totalSent,
                                        boolean healthy) {}
