package com.tcc.pjb.backend.integration.datajud.feed;

import java.util.List;

public interface DataJudFeedHttpClient {

    void bulkIndex(List<DataJudFeedEntry> entries);
}
