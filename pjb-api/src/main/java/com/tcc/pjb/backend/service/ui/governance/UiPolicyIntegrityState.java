package com.tcc.pjb.backend.service.ui.governance;

import java.util.concurrent.atomic.AtomicBoolean;

public final class UiPolicyIntegrityState {

  private final AtomicBoolean degraded = new AtomicBoolean();

  public boolean isDegraded() {
    return degraded.get();
  }

  public void degrade() {
    degraded.set(true);
  }
}
