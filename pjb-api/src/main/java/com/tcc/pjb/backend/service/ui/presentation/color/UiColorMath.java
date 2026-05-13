package com.tcc.pjb.backend.service.ui.presentation.color;

import java.util.Locale;

public final class UiColorMath {

  private UiColorMath() {
  }

  public static UiRgb parse(String hex) {
    String h = normalize(hex);
    int r = Integer.parseInt(h.substring(1, 3), 16);
    int g = Integer.parseInt(h.substring(3, 5), 16);
    int b = Integer.parseInt(h.substring(5, 7), 16);
    return new UiRgb(r, g, b);
  }

  public static String toHex(UiRgb c) {
    int r = clamp(c.r());
    int g = clamp(c.g());
    int b = clamp(c.b());
    return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
  }

  public static String mix(String a, String b, double t) {
    UiRgb ca = parse(a);
    UiRgb cb = parse(b);
    double lt = clamp01(t);

    double ar = srgbToLinear(ca.r() / 255.0);
    double ag = srgbToLinear(ca.g() / 255.0);
    double ab = srgbToLinear(ca.b() / 255.0);

    double br = srgbToLinear(cb.r() / 255.0);
    double bg = srgbToLinear(cb.g() / 255.0);
    double bb = srgbToLinear(cb.b() / 255.0);

    double rr = ar + (br - ar) * lt;
    double rg = ag + (bg - ag) * lt;
    double rb = ab + (bb - ab) * lt;

    int r = (int) Math.round(linearToSrgb(rr) * 255.0);
    int g = (int) Math.round(linearToSrgb(rg) * 255.0);
    int b2 = (int) Math.round(linearToSrgb(rb) * 255.0);

    return toHex(new UiRgb(r, g, b2));
  }

  public static String lighten(String hex, double t) {
    return mix(hex, "#FFFFFF", t);
  }

  public static String darken(String hex, double t) {
    return mix(hex, "#000000", t);
  }

  public static double relativeLuminance(String hex) {
    UiRgb c = parse(hex);
    double r = srgbToLinear(c.r() / 255.0);
    double g = srgbToLinear(c.g() / 255.0);
    double b = srgbToLinear(c.b() / 255.0);
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  }

  public static double contrastRatio(String a, String b) {
    double la = relativeLuminance(a);
    double lb = relativeLuminance(b);
    double l1 = Math.max(la, lb);
    double l2 = Math.min(la, lb);
    return (l1 + 0.05) / (l2 + 0.05);
  }

  public static String ensureContrast(String fg, String bg, double minRatio) {
    String f = normalize(fg);
    String b = normalize(bg);
    double r = contrastRatio(f, b);
    if (r >= minRatio) return f;

    boolean fgIsLighter = relativeLuminance(f) >= relativeLuminance(b);
    String target = fgIsLighter ? "#FFFFFF" : "#000000";

    double lo = 0.0;
    double hi = 1.0;
    String best = f;
    for (int i = 0; i < 18; i++) {
      double mid = (lo + hi) * 0.5;
      String cand = mix(f, target, mid);
      double cr = contrastRatio(cand, b);
      if (cr >= minRatio) {
        best = cand;
        hi = mid;
      } else {
        lo = mid;
      }
    }
    return best;
  }

  private static String normalize(String hex) {
    if (hex == null || hex.isBlank()) {
      return "#000000";
    }
    String h = hex.trim();
    if (!h.startsWith("#")) {
      h = "#" + h;
    }
    if (h.length() == 4) {
      char r = h.charAt(1);
      char g = h.charAt(2);
      char b = h.charAt(3);
      h = "#" + r + r + g + g + b + b;
    }
    if (h.length() != 7) {
      return "#000000";
    }
    return h.toUpperCase(Locale.ROOT);
  }

  private static int clamp(int v) {
    return Math.max(0, Math.min(255, v));
  }

  private static double clamp01(double v) {
    if (v <= 0.0) return 0.0;
    if (v >= 1.0) return 1.0;
    return v;
  }

  private static double srgbToLinear(double c) {
    if (c <= 0.04045) {
      return c / 12.92;
    }
    return Math.pow((c + 0.055) / 1.055, 2.4);
  }

  private static double linearToSrgb(double c) {
    if (c <= 0.0031308) {
      return 12.92 * c;
    }
    return 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
  }
}
