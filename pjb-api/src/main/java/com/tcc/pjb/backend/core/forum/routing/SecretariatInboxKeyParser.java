package com.tcc.pjb.backend.core.forum.routing;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public final class SecretariatInboxKeyParser {

  private SecretariatInboxKeyParser() {
  }

  public static Optional<Parts> parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    String key = InboxKeyCompat.normalizeSecretariatInboxKey(raw.trim());
    String[] p = key.split(":");
    if (p.length < 6) {
      return Optional.empty();
    }
    if (!"SEC".equalsIgnoreCase(p[0])) {
      return Optional.empty();
    }

    String org = safe(p, 1);
    String inst = safe(p, 2);
    String lane = safe(p, 3);
    String uf = safe(p, 4);

    String comarca = p.length >= 7 ? safe(p, 5) : "-";
    String juris = p.length >= 7 ? safe(p, 6) : "";

    return Optional.of(new Parts(
        key,
        org,
        inst,
        lane,
        uf,
        comarca,
        juris,
        p.length >= 7
    ));
  }

  public static String slugTerritory(String s) {
    if (s == null) return "-";
    String n = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    return n.isEmpty() ? "-" : n;
  }

  private static String safe(String[] p, int idx) {
    if (idx < 0 || idx >= p.length) return "";
    return p[idx] == null ? "" : p[idx].trim();
  }

  public record ParsedInboxKey(String normalized, String org, String instance, String lane, String uf, String comarca, String jurisdicao, boolean hasTerritory) {
    public Parts toParts() {
      return new Parts(normalized, org, instance, lane, uf, comarca, jurisdicao, hasTerritory);
    }
  }

  public record Parts(
      String normalized,
      String org,
      String instance,
      String lane,
      String uf,
      String comarca,
      String jurisdicao,
      boolean hasTerritory
  ) {
  }
}
