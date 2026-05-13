package com.tcc.pjb.backend.controller.ui;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pjb.ui.accessibility.ratelimit")
public class UiAccessibilityRateLimitProperties {

  @NotNull
  private boolean enabled = true;

  @Min(1)
  @Max(10_000)
  private int perUserPerMinute = 60;

  @Min(1)
  @Max(100_000)
  private int perIpPerMinute = 600;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getPerUserPerMinute() {
    return perUserPerMinute;
  }

  public void setPerUserPerMinute(int perUserPerMinute) {
    this.perUserPerMinute = perUserPerMinute;
  }

  public int getPerIpPerMinute() {
    return perIpPerMinute;
  }

  public void setPerIpPerMinute(int perIpPerMinute) {
    this.perIpPerMinute = perIpPerMinute;
  }
}
