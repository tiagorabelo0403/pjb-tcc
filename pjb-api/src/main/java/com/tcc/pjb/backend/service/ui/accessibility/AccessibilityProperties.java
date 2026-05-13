package com.tcc.pjb.backend.service.ui.accessibility;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.ui.accessibility")
public class AccessibilityProperties {

  private boolean enabled = true;
  private boolean useOrchestrator = true;

  
  private int minScoreToSuggest = 320;

  private Duration snoozeDuration = Duration.ofDays(90);
  private Duration reevaluateMinInterval = Duration.ofDays(1);
  private int maxReasons = 6;

  
  private String policyFile;

  
  private int snapshotRetentionDays = 180;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isUseOrchestrator() {
    return useOrchestrator;
  }

  public void setUseOrchestrator(boolean useOrchestrator) {
    this.useOrchestrator = useOrchestrator;
  }

  public int getMinScoreToSuggest() {
    return minScoreToSuggest;
  }

  public void setMinScoreToSuggest(int minScoreToSuggest) {
    this.minScoreToSuggest = minScoreToSuggest;
  }

  public Duration getSnoozeDuration() {
    return snoozeDuration;
  }

  public void setSnoozeDuration(Duration snoozeDuration) {
    this.snoozeDuration = snoozeDuration;
  }

  public Duration getReevaluateMinInterval() {
    return reevaluateMinInterval;
  }

  public void setReevaluateMinInterval(Duration reevaluateMinInterval) {
    this.reevaluateMinInterval = reevaluateMinInterval;
  }

  public int getMaxReasons() {
    return maxReasons;
  }

  public void setMaxReasons(int maxReasons) {
    this.maxReasons = maxReasons;
  }

  public String getPolicyFile() {
    return policyFile;
  }

  public void setPolicyFile(String policyFile) {
    this.policyFile = policyFile;
  }

  public int getSnapshotRetentionDays() {
    return snapshotRetentionDays;
  }

  public void setSnapshotRetentionDays(int snapshotRetentionDays) {
    this.snapshotRetentionDays = snapshotRetentionDays;
  }
}
