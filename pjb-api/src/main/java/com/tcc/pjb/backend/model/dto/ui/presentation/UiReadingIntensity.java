package com.tcc.pjb.backend.model.dto.ui.presentation;

import java.util.Locale;

public enum UiReadingIntensity {
  SOFT,
  MEDIUM,
  STRONG;

  public static UiReadingIntensity fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return SOFT;
    }
    String v = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    for (UiReadingIntensity i : values()) {
      if (i.name().equals(v)) {
        return i;
      }
    }
    return SOFT;
  }
}
