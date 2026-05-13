package com.tcc.pjb.backend.service.ui.presentation.compiler;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationVariant;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.service.ui.presentation.ReadingModeProperties;
import com.tcc.pjb.backend.service.ui.presentation.color.UiColorMath;

public final class UiPresentationCompiler {

  public record Result(UiPresentationVariant variant, List<UiCssTokenDto> tokens, Map<String, String> tokenMap) {
  }

  private final ReadingModeProperties reading;

  public UiPresentationCompiler(ReadingModeProperties reading) {
    this.reading = Objects.requireNonNull(reading, "reading");
  }

  public Result compile(UiTheme theme, UiAccessibilityPreset legacyPreset, long flagsMask, boolean readingEnabled, UiReadingIntensity intensity) {
    UiTheme t = theme == null ? UiTheme.LIGHT : theme;
    UiAccessibilityPreset p = legacyPreset == null ? UiAccessibilityPreset.DEFAULT : legacyPreset;
    EnumSet<UiAccessibilityFlag> flags = UiAccessibilityFlag.fromMask(flagsMask);

    boolean rm = readingEnabled;
    UiReadingIntensity ri = intensity == null ? reading.getDefaultIntensity() : intensity;
    ReadingModeProperties.Intensity cfg = rm ? reading.resolve(ri) : baseline();

    UiTokenTable table = new UiTokenTable();

    if (t == UiTheme.DARK) {
      baseDark(table);
    } else {
      baseLight(table);
    }

    if (rm) {
      if (t == UiTheme.DARK) {
        applyReadingDark(table, ri);
      } else {
        applyReadingLight(table, ri);
      }
    }

    applyTypography(table, cfg);

    table.put(UiCssTokenKey.UNDERLINE_THICKNESS, "1px");

    if (flags.contains(UiAccessibilityFlag.HIGH_CONTRAST) || p == UiAccessibilityPreset.HIGH_CONTRAST) {
      if (t == UiTheme.DARK) {
        applyHighContrastDark(table);
      } else {
        applyHighContrastLight(table);
      }
    }

    if (flags.contains(UiAccessibilityFlag.LARGE_TEXT) || p == UiAccessibilityPreset.LARGE_TEXT) {
      applyLargeTextOverlay(table);
    }

    if (flags.contains(UiAccessibilityFlag.REDUCED_MOTION) || p == UiAccessibilityPreset.REDUCED_MOTION) {
      table.put(UiCssTokenKey.MOTION_FACTOR, "0");
    }

    if (flags.contains(UiAccessibilityFlag.KEYBOARD_ONLY) || p == UiAccessibilityPreset.KEYBOARD_ONLY) {
      applyKeyboardOverlay(table, t);
    }

    enforceContrast(table, flags.contains(UiAccessibilityFlag.HIGH_CONTRAST));

    UiPresentationVariant variant = computeVariant(rm, ri, p);
    return new Result(variant, table.toDtoList(), table.toStringMap());
  }

  private void enforceContrast(UiTokenTable t, boolean highContrast) {
    String bg = t.get(UiCssTokenKey.BG);
    double minText = highContrast ? 7.0 : 4.5;
    double minMuted = highContrast ? 7.0 : 3.5;
    double minLink = highContrast ? 7.0 : 4.5;

    t.put(UiCssTokenKey.TEXT, UiColorMath.ensureContrast(t.get(UiCssTokenKey.TEXT), bg, minText));
    t.put(UiCssTokenKey.MUTED, UiColorMath.ensureContrast(t.get(UiCssTokenKey.MUTED), bg, minMuted));
    t.put(UiCssTokenKey.LINK, UiColorMath.ensureContrast(t.get(UiCssTokenKey.LINK), bg, minLink));
  }

  private UiPresentationVariant computeVariant(boolean readingEnabled, UiReadingIntensity intensity, UiAccessibilityPreset preset) {
    if (!readingEnabled) {
      return UiPresentationVariant.DEFAULT;
    }
    boolean hc = preset == UiAccessibilityPreset.HIGH_CONTRAST;
    return switch (intensity) {
      case SOFT -> hc ? UiPresentationVariant.READING_SOFT_HIGH_CONTRAST : UiPresentationVariant.READING_SOFT;
      case MEDIUM -> hc ? UiPresentationVariant.READING_MEDIUM_HIGH_CONTRAST : UiPresentationVariant.READING_MEDIUM;
      case STRONG -> hc ? UiPresentationVariant.READING_STRONG_HIGH_CONTRAST : UiPresentationVariant.READING_STRONG;
    };
  }

  private void baseLight(UiTokenTable t) {
    t.put(UiCssTokenKey.BG, "#FFFFFF");
    t.put(UiCssTokenKey.SURFACE, "#FFFFFF");
    t.put(UiCssTokenKey.TEXT, "#1B1B1B");
    t.put(UiCssTokenKey.MUTED, "#3D3D3D");
    t.put(UiCssTokenKey.BORDER, "#D0D4DA");
    t.put(UiCssTokenKey.LINK, "#0B57D0");
    t.put(UiCssTokenKey.FOCUS, "#1A73E8");
    t.put(UiCssTokenKey.BORDER_WIDTH, "1px");
    t.put(UiCssTokenKey.FOCUS_WIDTH, "2px");
    t.put(UiCssTokenKey.RADIUS, "10px");
    t.put(UiCssTokenKey.SHADOW_OPACITY, "0.08");
    t.put(UiCssTokenKey.MOTION_FACTOR, "1");

    
    t.put(UiCssTokenKey.CHAT_BG, "#ECE5DD");
    t.put(UiCssTokenKey.CHAT_IN_BUBBLE_BG, "#FFFFFF");
    t.put(UiCssTokenKey.CHAT_OUT_BUBBLE_BG, "#DCF8C6");
    t.put(UiCssTokenKey.CHAT_IN_TEXT, "#111B21");
    t.put(UiCssTokenKey.CHAT_OUT_TEXT, "#111B21");
    t.put(UiCssTokenKey.CHAT_ACCENT, "#00A884");
    t.put(UiCssTokenKey.CHAT_INPUT_BG, "#FFFFFF");
    t.put(UiCssTokenKey.CHAT_DIVIDER, "#D1D7DB");
    t.put(UiCssTokenKey.CHAT_BUBBLE_RADIUS, "18px");

    
    t.put(UiCssTokenKey.CHAT_ATTACH_ENABLED, "0");
    t.put(UiCssTokenKey.CHAT_ATTACH_MAX_BYTES, "10485760");
    t.put(UiCssTokenKey.CHAT_ATTACH_MAX_PER_MESSAGE, "3");

    
    t.put(UiCssTokenKey.NOTIFY_BADGE_BG, "#25D366");
    t.put(UiCssTokenKey.NOTIFY_BADGE_TEXT, "#0B0F14");
    t.put(UiCssTokenKey.NOTIFY_PULSE_MS, "900");
    t.put(UiCssTokenKey.NOTIFY_SHAKE_MS, "420");
    t.put(UiCssTokenKey.NOTIFY_SHAKE_DEG, "2.6deg");
    t.put(UiCssTokenKey.NOTIFY_PULSE_SCALE, "1.06");

    
    t.put(UiCssTokenKey.WATERMARK_TEXT, "PJB");
    t.put(UiCssTokenKey.WATERMARK_OPACITY, "0.07");
    t.put(UiCssTokenKey.WATERMARK_ROTATE, "-12deg");
  }

  private void baseDark(UiTokenTable t) {
    t.put(UiCssTokenKey.BG, "#0F1115");
    t.put(UiCssTokenKey.SURFACE, "#151A22");
    t.put(UiCssTokenKey.TEXT, "#EAEAEA");
    t.put(UiCssTokenKey.MUTED, "#C9C9C9");
    t.put(UiCssTokenKey.BORDER, "#2A303A");
    t.put(UiCssTokenKey.LINK, "#8AB4F8");
    t.put(UiCssTokenKey.FOCUS, "#AECBFA");
    t.put(UiCssTokenKey.BORDER_WIDTH, "1px");
    t.put(UiCssTokenKey.FOCUS_WIDTH, "2px");
    t.put(UiCssTokenKey.RADIUS, "10px");
    t.put(UiCssTokenKey.SHADOW_OPACITY, "0.18");
    t.put(UiCssTokenKey.MOTION_FACTOR, "1");

    
    t.put(UiCssTokenKey.CHAT_BG, "#0B141A");
    t.put(UiCssTokenKey.CHAT_IN_BUBBLE_BG, "#202C33");
    t.put(UiCssTokenKey.CHAT_OUT_BUBBLE_BG, "#005C4B");
    t.put(UiCssTokenKey.CHAT_IN_TEXT, "#E9EDEF");
    t.put(UiCssTokenKey.CHAT_OUT_TEXT, "#E9EDEF");
    t.put(UiCssTokenKey.CHAT_ACCENT, "#00A884");
    t.put(UiCssTokenKey.CHAT_INPUT_BG, "#202C33");
    t.put(UiCssTokenKey.CHAT_DIVIDER, "#2A3942");
    t.put(UiCssTokenKey.CHAT_BUBBLE_RADIUS, "18px");

    
    t.put(UiCssTokenKey.CHAT_ATTACH_ENABLED, "0");
    t.put(UiCssTokenKey.CHAT_ATTACH_MAX_BYTES, "10485760");
    t.put(UiCssTokenKey.CHAT_ATTACH_MAX_PER_MESSAGE, "3");

    
    t.put(UiCssTokenKey.NOTIFY_BADGE_BG, "#00A884");
    t.put(UiCssTokenKey.NOTIFY_BADGE_TEXT, "#0B0F14");
    t.put(UiCssTokenKey.NOTIFY_PULSE_MS, "900");
    t.put(UiCssTokenKey.NOTIFY_SHAKE_MS, "420");
    t.put(UiCssTokenKey.NOTIFY_SHAKE_DEG, "2.6deg");
    t.put(UiCssTokenKey.NOTIFY_PULSE_SCALE, "1.06");

    
    t.put(UiCssTokenKey.WATERMARK_TEXT, "PJB");
    t.put(UiCssTokenKey.WATERMARK_OPACITY, "0.05");
    t.put(UiCssTokenKey.WATERMARK_ROTATE, "-12deg");
  }

  private void applyReadingLight(UiTokenTable t, UiReadingIntensity intensity) {
    String paper = switch (intensity) {
      case SOFT -> "#F6F3EA";
      case MEDIUM -> "#F2EEDD";
      case STRONG -> "#EEE8D2";
    };
    String bg = UiColorMath.mix(t.get(UiCssTokenKey.BG), paper, 0.86);
    String surface = UiColorMath.mix(bg, "#FFFFFF", 0.42);
    String border = UiColorMath.mix(bg, t.get(UiCssTokenKey.TEXT), 0.10);
    t.put(UiCssTokenKey.BG, bg);
    t.put(UiCssTokenKey.SURFACE, surface);
    t.put(UiCssTokenKey.BORDER, border);
    t.put(UiCssTokenKey.TEXT, "#1C1C1C");
    t.put(UiCssTokenKey.MUTED, "#3A3A3A");

    
    t.put(UiCssTokenKey.CHAT_BG, UiColorMath.mix(t.get(UiCssTokenKey.CHAT_BG), bg, 0.60));
    t.put(UiCssTokenKey.CHAT_IN_BUBBLE_BG, UiColorMath.mix("#FFFFFF", bg, 0.15));
    t.put(UiCssTokenKey.CHAT_OUT_BUBBLE_BG, UiColorMath.mix(t.get(UiCssTokenKey.CHAT_OUT_BUBBLE_BG), bg, 0.35));
  }

  private void applyReadingDark(UiTokenTable t, UiReadingIntensity intensity) {
    String base = "#0F1115";
    String warm = switch (intensity) {
      case SOFT -> "#11151A";
      case MEDIUM -> "#11161D";
      case STRONG -> "#121820";
    };
    String bg = UiColorMath.mix(base, warm, 0.65);
    String surface = UiColorMath.lighten(bg, 0.08);
    String border = UiColorMath.lighten(bg, 0.16);
    t.put(UiCssTokenKey.BG, bg);
    t.put(UiCssTokenKey.SURFACE, surface);
    t.put(UiCssTokenKey.BORDER, border);
    t.put(UiCssTokenKey.TEXT, "#ECECEC");
    t.put(UiCssTokenKey.MUTED, "#CFCFCF");

    
    t.put(UiCssTokenKey.CHAT_BG, UiColorMath.mix(t.get(UiCssTokenKey.CHAT_BG), bg, 0.55));
    t.put(UiCssTokenKey.CHAT_IN_BUBBLE_BG, UiColorMath.mix(t.get(UiCssTokenKey.CHAT_IN_BUBBLE_BG), bg, 0.45));
    t.put(UiCssTokenKey.CHAT_OUT_BUBBLE_BG, UiColorMath.mix(t.get(UiCssTokenKey.CHAT_OUT_BUBBLE_BG), bg, 0.30));
  }

  private void applyTypography(UiTokenTable t, ReadingModeProperties.Intensity cfg) {
    double fontScale = Math.max(0.8, Math.min(2.0, cfg.getFontScalePercent() / 100.0));
    t.put(UiCssTokenKey.FONT_SCALE, format3(fontScale));
    t.put(UiCssTokenKey.LINE_HEIGHT, format3(cfg.getLineHeight()));
    t.put(UiCssTokenKey.PARAGRAPH_GAP, format3(cfg.getParagraphGapRem()) + "rem");
    t.put(UiCssTokenKey.LETTER_SPACING, format3(cfg.getLetterSpacingEm()) + "em");
    t.put(UiCssTokenKey.CONTENT_MAX_WIDTH, Integer.toString(cfg.getMaxWidthCh()) + "ch");
  }

  private ReadingModeProperties.Intensity baseline() {
    ReadingModeProperties.Intensity i = new ReadingModeProperties.Intensity();
    i.setMaxWidthCh(92);
    i.setLineHeight(1.55);
    i.setParagraphGapRem(0.65);
    i.setFontScalePercent(100);
    i.setLetterSpacingEm(0.0);
    return i;
  }

  private void applyHighContrastLight(UiTokenTable t) {
    t.put(UiCssTokenKey.BG, "#FFFFFF");
    t.put(UiCssTokenKey.SURFACE, "#FFFFFF");
    t.put(UiCssTokenKey.TEXT, "#000000");
    t.put(UiCssTokenKey.MUTED, "#000000");
    t.put(UiCssTokenKey.BORDER, "#000000");
    t.put(UiCssTokenKey.LINK, "#0000EE");
    t.put(UiCssTokenKey.FOCUS, "#FFD600");
    t.put(UiCssTokenKey.BORDER_WIDTH, "2px");
    t.put(UiCssTokenKey.FOCUS_WIDTH, "3px");
    t.put(UiCssTokenKey.SHADOW_OPACITY, "0");
    t.put(UiCssTokenKey.UNDERLINE_THICKNESS, "2px");
  }

  private void applyHighContrastDark(UiTokenTable t) {
    t.put(UiCssTokenKey.BG, "#000000");
    t.put(UiCssTokenKey.SURFACE, "#000000");
    t.put(UiCssTokenKey.TEXT, "#FFFFFF");
    t.put(UiCssTokenKey.MUTED, "#FFFFFF");
    t.put(UiCssTokenKey.BORDER, "#FFFFFF");
    t.put(UiCssTokenKey.LINK, "#4FC3F7");
    t.put(UiCssTokenKey.FOCUS, "#FFD600");
    t.put(UiCssTokenKey.BORDER_WIDTH, "2px");
    t.put(UiCssTokenKey.FOCUS_WIDTH, "3px");
    t.put(UiCssTokenKey.SHADOW_OPACITY, "0");
    t.put(UiCssTokenKey.UNDERLINE_THICKNESS, "2px");
  }

  private void applyLargeTextOverlay(UiTokenTable t) {
    double baseScale = parseDoubleSafe(t.get(UiCssTokenKey.FONT_SCALE), 1.0);
    double newScale = Math.min(2.5, baseScale * 1.12);
    t.put(UiCssTokenKey.FONT_SCALE, format3(newScale));

    double lh = parseDoubleSafe(t.get(UiCssTokenKey.LINE_HEIGHT), 1.65);
    t.put(UiCssTokenKey.LINE_HEIGHT, format3(Math.min(2.2, lh + 0.06)));

    String cw = t.get(UiCssTokenKey.CONTENT_MAX_WIDTH);
    int ch = parseCh(cw, 74);
    t.put(UiCssTokenKey.CONTENT_MAX_WIDTH, Integer.toString(Math.max(56, ch - 4)) + "ch");

    double pg = parseRem(t.get(UiCssTokenKey.PARAGRAPH_GAP), 0.75);
    t.put(UiCssTokenKey.PARAGRAPH_GAP, format3(Math.min(2.5, pg + 0.12)) + "rem");
  }

  private void applyKeyboardOverlay(UiTokenTable t, UiTheme theme) {
    t.put(UiCssTokenKey.FOCUS_WIDTH, "3px");
    t.put(UiCssTokenKey.BORDER_WIDTH, "2px");
    t.put(UiCssTokenKey.UNDERLINE_THICKNESS, "2px");
    if (theme == UiTheme.DARK) {
      t.put(UiCssTokenKey.FOCUS, "#00E5FF");
    } else {
      t.put(UiCssTokenKey.FOCUS, "#2962FF");
    }
  }

  private static String format3(double v) {
    return String.format(java.util.Locale.ROOT, "%.3f", v);
  }

  private static double parseDoubleSafe(String v, double def) {
    if (v == null || v.isBlank()) return def;
    try {
      return Double.parseDouble(v.trim());
    } catch (Exception ignored) {
      return def;
    }
  }

  private static int parseCh(String v, int def) {
    if (v == null || v.isBlank()) return def;
    String s = v.trim().toLowerCase(java.util.Locale.ROOT);
    if (s.endsWith("ch")) {
      s = s.substring(0, s.length() - 2);
    }
    try {
      return Integer.parseInt(s.trim());
    } catch (Exception ignored) {
      return def;
    }
  }

  private static double parseRem(String v, double def) {
    if (v == null || v.isBlank()) return def;
    String s = v.trim().toLowerCase(java.util.Locale.ROOT);
    if (s.endsWith("rem")) {
      s = s.substring(0, s.length() - 3);
    }
    try {
      return Double.parseDouble(s.trim());
    } catch (Exception ignored) {
      return def;
    }
  }
}
