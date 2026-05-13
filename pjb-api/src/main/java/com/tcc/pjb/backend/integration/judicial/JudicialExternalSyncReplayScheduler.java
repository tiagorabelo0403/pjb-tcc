package com.tcc.pjb.backend.integration.judicial;

import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Component;

@Component
public class JudicialExternalSyncReplayScheduler {

    private final JudicialExternalSyncReplayService judicialExternalSyncReplayService;

    public JudicialExternalSyncReplayScheduler(JudicialExternalSyncReplayService judicialExternalSyncReplayService) {
        this.judicialExternalSyncReplayService = judicialExternalSyncReplayService;
    }

    @PjbClusterSingletonTask(key = "judicial-sync-replay", ttl = "PT12M")
    @Scheduled(fixedDelayString = "${pjb.integration.judicial.syncReplay.delayMs:1200000}")
    public void replay() {
        judicialExternalSyncReplayService.replayPendingSynchronizations(16, 6);
    }
}
