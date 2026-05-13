package com.tcc.pjb.backend.service.ui.accessibility.governance;

import java.util.List;

public record AccessibilityAbacPolicyFile(
    int version,
    Decision defaultDecision,
    List<Rule> rules
) {
  public record Decision(
      boolean enabled,
      int minScoreToSuggest,
      long allowFlagsMask,
      long denyFlagsMask
  ) {
  }

  public record Match(
      String uf,
      String comarca,
      String tipoUsuario,
      String enteFederativo
  ) {
  }

  public record Rule(
      String name,
      Match match,
      Decision decision
  ) {
  }
}
