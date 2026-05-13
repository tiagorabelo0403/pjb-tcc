package com.tcc.pjb.backend.service.ui.accessibility.policy;

import java.util.List;
import java.util.Map;

public record AccessibilityPolicyFile(
    int version,
    Model model,
    List<String> presetPriority,
    Map<String, Double> signalWeights,
    Map<String, String> reasonCatalog
) {

  public record Model(
      double bias,
      double k,
      int minScoreToSuggest,
      int maxReasons
  ) {
  }
}
