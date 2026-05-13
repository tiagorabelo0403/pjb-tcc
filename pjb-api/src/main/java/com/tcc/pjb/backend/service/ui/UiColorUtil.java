package com.tcc.pjb.backend.service.ui;

import java.util.Locale;

public final class UiColorUtil {

  private UiColorUtil() {
  }

  public static String normalizeHex(String hex) {
    if (hex == null) return "#000000";
    String h = hex.trim();
    if (h.isEmpty()) return "#000000";
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

  
  public static String pickOnColor(String backgroundHex) {
    String h = normalizeHex(backgroundHex);
    int r = Integer.parseInt(h.substring(1, 3), 16);
    int g = Integer.parseInt(h.substring(3, 5), 16);
    int b = Integer.parseInt(h.substring(5, 7), 16);

    
    double y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
    return y >= 0.60 ? "#1B1B1B" : "#FFFFFF";
  }
}
