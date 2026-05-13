package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudCheckpointQueryResult(DataJudCheckpointView checkpoint,
                                           DataJudCheckpointAuditSnapshot audit) {
    public DataJudCheckpointView view() { return checkpoint; }
}
