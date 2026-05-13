package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.util.Locale;

public enum UiAccessibilityPreset {
  DEFAULT,
  HIGH_CONTRAST,
  LARGE_TEXT,
  REDUCED_MOTION,
  SCREEN_READER_OPTIMIZED,
  KEYBOARD_ONLY;

  public static UiAccessibilityPreset fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return DEFAULT;
    }
    String t = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    for (UiAccessibilityPreset p : values()) {
      if (p.name().equals(t)) {
        return p;
      }
    }
    return DEFAULT;
  }
}
