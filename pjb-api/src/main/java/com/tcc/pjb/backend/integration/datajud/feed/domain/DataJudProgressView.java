package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudProgressView(String tribunalCodigo,
                                  int batchSent,
                                  long totalSent,
                                  boolean ok) {
}
