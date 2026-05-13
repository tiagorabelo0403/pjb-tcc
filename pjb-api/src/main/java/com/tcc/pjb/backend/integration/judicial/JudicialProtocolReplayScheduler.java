package com.tcc.pjb.backend.integration.judicial;

import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Component;

@Component
public class JudicialProtocolReplayScheduler {

    private final JudicialProtocolReplayService judicialProtocolReplayService;

    public JudicialProtocolReplayScheduler(JudicialProtocolReplayService judicialProtocolReplayService) {
        this.judicialProtocolReplayService = judicialProtocolReplayService;
    }

    @PjbClusterSingletonTask(key = "judicial-protocol-replay", ttl = "PT10M")
    @Scheduled(fixedDelayString = "${pjb.integration.judicial.protocolReplay.delayMs:900000}")
    public void replay() {
        judicialProtocolReplayService.replayPendingProtocols(12, 4);
    }
}
