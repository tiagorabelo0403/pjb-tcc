package com.tcc.pjb.backend.model.dto.ui;

import com.tcc.pjb.backend.core.util.EnumText;

public enum UiTheme {
  LIGHT,
  DARK;

  public static UiTheme fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return LIGHT;
    }
    String t = EnumText.normalizeToken(raw);
    if (t.isBlank()) {
      return LIGHT;
    }
    return switch (t) {
      case "DARK", "ESCURO", "NOITE" -> DARK;
      default -> LIGHT;
    };
  }
}
