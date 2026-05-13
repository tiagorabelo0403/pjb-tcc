package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudTribunalHealthResult(
        DataJudFeedHealthSnapshot health,
        DataJudTribunalWindowSnapshot window
) {}
