package com.tcc.pjb.backend.service.secretariat.live;

import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SecretariatSseHeartbeatScheduler {

  private final SecretariatLiveHub hub;

  public SecretariatSseHeartbeatScheduler(SecretariatLiveHub hub) {
    this.hub = Objects.requireNonNull(hub);
  }

  @Scheduled(fixedDelayString = "${pjb.secretariat.sse.heartbeatMs:15000}")
  public void heartbeat() {
    hub.heartbeat();
  }
}
