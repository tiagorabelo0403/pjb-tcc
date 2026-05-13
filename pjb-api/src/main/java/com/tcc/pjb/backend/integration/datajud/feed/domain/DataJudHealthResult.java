package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudHealthResult(DataJudFeedHealthSnapshot health, DataJudCheckpointView checkpoint) {
    public DataJudFeedHealthSnapshot view() { return health; }
}
