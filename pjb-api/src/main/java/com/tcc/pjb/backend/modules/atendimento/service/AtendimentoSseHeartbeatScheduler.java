package com.tcc.pjb.backend.modules.atendimento.service;

import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AtendimentoSseHeartbeatScheduler {

  private final AtendimentoInboxLiveHub hub;

  public AtendimentoSseHeartbeatScheduler(AtendimentoInboxLiveHub hub) {
    this.hub = Objects.requireNonNull(hub);
  }

  @Scheduled(fixedDelayString = "${pjb.atendimento.sse.heartbeatMs:15000}")
  public void heartbeat() {
    hub.heartbeat();
  }
}
