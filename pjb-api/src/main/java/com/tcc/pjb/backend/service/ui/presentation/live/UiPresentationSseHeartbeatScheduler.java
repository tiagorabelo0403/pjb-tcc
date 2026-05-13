package com.tcc.pjb.backend.service.ui.presentation.live;

import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UiPresentationSseHeartbeatScheduler {

  private final UiPresentationLiveHub hub;

  public UiPresentationSseHeartbeatScheduler(UiPresentationLiveHub hub) {
    this.hub = Objects.requireNonNull(hub);
  }

  @Scheduled(fixedDelayString = "${pjb.ui.presentationSse.heartbeatMs:15000}")
  public void heartbeat() {
    hub.heartbeat();
  }
}
