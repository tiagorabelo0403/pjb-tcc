package com.tcc.pjb.backend.service.ui.accessibility.live;

import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UiAccessibilitySseHeartbeatScheduler {

  private final UiAccessibilityLiveHub hub;

  public UiAccessibilitySseHeartbeatScheduler(UiAccessibilityLiveHub hub) {
    this.hub = Objects.requireNonNull(hub);
  }

  @Scheduled(fixedDelayString = "${pjb.ui.accessibilitySse.heartbeatMs:15000}")
  public void heartbeat() {
    hub.heartbeat();
  }
}
