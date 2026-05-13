package com.tcc.pjb.backend.service.ui.rules;

import java.util.List;
import java.util.Map;

public record UiRulesFile(
    int version,
    Map<String, UiTokenRule> tokens,
    Map<String, List<String>> assuntoPalette
) {

  public record UiTokenRule(
      Map<String, UiColorRule> colors,
      Map<String, String> labels,
      Map<String, String> descriptions,
      String icon,
      String pattern
  ) {
  }

  public record UiColorRule(
      String hex,
      String on
  ) {
  }
}
