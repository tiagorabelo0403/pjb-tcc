package com.tcc.pjb.backend.integration.datajud.feed;

import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalRunCommand;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataJudFeedScheduler {

    private final DataJudFeedService service;
    private final DataJudFeedProperties properties;

    public DataJudFeedScheduler(DataJudFeedService service,
                                DataJudFeedProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${pjb.datajud.feed.scheduler-fixed-rate-ms:300000}")
    public void run() {
        if (!properties.enabled()) {
            return;
        }
        List<String> tribunais = properties.tribunais();
        if (tribunais == null || tribunais.isEmpty()) {
            service.runIncremental(new DataJudTribunalRunCommand("CNJ", properties.maxBatchesPerRun()));
            return;
        }
        for (String tribunal : tribunais) {
            if (tribunal != null && !tribunal.isBlank()) {
                service.runIncremental(new DataJudTribunalRunCommand(tribunal, properties.maxBatchesPerRun()));
            }
        }
    }
}
