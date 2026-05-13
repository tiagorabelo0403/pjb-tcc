package com.tcc.pjb.backend.integration.datajud.feed;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.datajud.feed")
public record DataJudFeedProperties(
        boolean enabled,
        int batchSize,
        long schedulerFixedRateMs,
        int maxBatchesPerRun,
        List<String> tribunais
) {
}
