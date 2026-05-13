package com.tcc.pjb.backend.service.ui.assunto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.service.ui.UiColorUtil;

@Service
public class AssuntoClassifierService {

  private final AssuntoCatalogRegistry catalog;

  public AssuntoClassifierService(AssuntoCatalogRegistry catalog) {
    this.catalog = Objects.requireNonNull(catalog);
  }

  public ClassifiedAssunto classify(String raw, UiTheme theme, List<String> fallbackPalette) {
    if (raw == null || raw.isBlank()) {
      return ClassifiedAssunto.unknown(theme, fallbackPalette);
    }
    String s = raw.trim().toLowerCase(Locale.ROOT);

    for (AssuntoGroup g : catalog.groups()) {
      if (g.matchAny() == null || g.matchAny().isEmpty()) continue;
      for (String m : g.matchAny()) {
        if (m == null || m.isBlank()) continue;
        if (s.contains(m)) {
          String hex = UiColorUtil.normalizeHex(g.colors().getOrDefault(theme, "#546E7A"));
          String on = UiColorUtil.pickOnColor(hex);
          return new ClassifiedAssunto(g.id(), hex, on);
        }
      }
    }

    
    return ClassifiedAssunto.fallbackHash(raw, theme, fallbackPalette);
  }

  public int catalogVersion() {
    return catalog.version();
  }

  public record ClassifiedAssunto(String groupId, String colorHex, String onColorHex) {
    static ClassifiedAssunto unknown(UiTheme theme, List<String> palette) {
      return fallbackHash("assunto", theme, palette);
    }

    static ClassifiedAssunto fallbackHash(String raw, UiTheme theme, List<String> palette) {
      String hex = pickDeterministic(raw, palette);
      return new ClassifiedAssunto("__fallback__", UiColorUtil.normalizeHex(hex), UiColorUtil.pickOnColor(hex));
    }

    private static String pickDeterministic(String raw, List<String> palette) {
      if (palette == null || palette.isEmpty()) {
        return "#546E7A";
      }
      int idx = Math.floorMod(stableHash(raw), palette.size());
      return palette.get(idx);
    }

    private static int stableHash(String s) {
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] h = md.digest(String.valueOf(s).getBytes(StandardCharsets.UTF_8));
        int v = 0;
        for (int i = 0; i < 4; i++) {
          v = (v << 8) | (h[i] & 0xFF);
        }
        return v;
      } catch (Exception e) {
        return s == null ? 0 : s.hashCode();
      }
    }
  }
}
