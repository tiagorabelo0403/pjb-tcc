package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudTribunalWindowSnapshot(String tribunalCodigo,
                                            long fromProcessoId,
                                            int batchSize) {}
