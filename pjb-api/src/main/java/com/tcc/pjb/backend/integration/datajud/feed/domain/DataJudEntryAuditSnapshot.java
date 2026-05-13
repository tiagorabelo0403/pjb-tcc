package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudEntryAuditSnapshot(Long processoId,
                                        String tribunalCodigo,
                                        String classeTpuCodigo,
                                        String statusProcesso) {}
