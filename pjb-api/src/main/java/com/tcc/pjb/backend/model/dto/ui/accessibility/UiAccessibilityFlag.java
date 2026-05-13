package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.util.EnumSet;

public enum UiAccessibilityFlag {
  HIGH_CONTRAST(1L),
  LARGE_TEXT(1L << 1),
  REDUCED_MOTION(1L << 2),
  SCREEN_READER_OPTIMIZED(1L << 3),
  KEYBOARD_ONLY(1L << 4),
  READING_MODE(1L << 5);

  private final long bit;

  UiAccessibilityFlag(long bit) {
    this.bit = bit;
  }

  public long bit() {
    return bit;
  }

  public static long maskOf(EnumSet<UiAccessibilityFlag> flags) {
    if (flags == null || flags.isEmpty()) return 0L;
    long m = 0L;
    for (UiAccessibilityFlag f : flags) {
      m |= f.bit;
    }
    return m;
  }

  public static EnumSet<UiAccessibilityFlag> fromMask(long mask) {
    EnumSet<UiAccessibilityFlag> out = EnumSet.noneOf(UiAccessibilityFlag.class);
    for (UiAccessibilityFlag f : values()) {
      if ((mask & f.bit) != 0L) out.add(f);
    }
    return out;
  }
}
