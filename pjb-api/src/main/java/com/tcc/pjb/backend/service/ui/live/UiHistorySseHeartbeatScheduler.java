package com.tcc.pjb.backend.service.ui.live;

import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UiHistorySseHeartbeatScheduler {

  private final UiHistoryLiveHub hub;

  public UiHistorySseHeartbeatScheduler(UiHistoryLiveHub hub) {
    this.hub = Objects.requireNonNull(hub);
  }

  @Scheduled(fixedDelayString = "${pjb.ui.historySse.heartbeatMs:15000}")
  public void heartbeat() {
    hub.heartbeat();
  }
}
