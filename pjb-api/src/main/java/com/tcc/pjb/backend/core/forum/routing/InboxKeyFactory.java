package com.tcc.pjb.backend.core.forum.routing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class InboxKeyFactory {

  private InboxKeyFactory() {
  }

  public static String secretariatInboxKey(JudicialOrganRef organ, ForumInstance instance, String uf, String comarca, String jurisdictionCode) {
    return secretariatInboxKey(organ, instance, ForumLane.COMUM, uf, comarca, jurisdictionCode);
  }

  public static String secretariatInboxKey(JudicialOrganRef organ, ForumInstance instance, ForumLane lane, String uf, String comarca, String jurisdictionCode) {
    Objects.requireNonNull(organ, "organ");
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(lane, "lane");
    String u = up(uf);
    String c = slug(comarca, 28);
    String j = slug(jurisdictionCode, 28);

    String base = "SEC:" + organ.code() + ":" + instanceToken(instance) + ":" + lane.token() + ":" + u + ":" + c + ":" + j;
    if (base.length() <= 120) {
      return base;
    }
    String hash = shortHash(c + ":" + j);
    String compact = "SEC:" + organ.code() + ":" + instanceToken(instance) + ":" + lane.token() + ":" + u + ":" + hash;
    return compact.length() <= 120 ? compact : compact.substring(0, 120);
  }

  private static String up(String s) {
    if (s == null) return "XX";
    String v = s.trim().toUpperCase(Locale.ROOT);
    return v.isEmpty() ? "XX" : (v.length() <= 4 ? v : v.substring(0, 4));
  }

  private static String instanceToken(ForumInstance i) {
    return switch (i) {
      case FIRST -> "1G";
      case SECOND -> "2G";
      case SUPERIOR -> "SUP";
    };
  }

  private static String slug(String s, int max) {
    if (s == null) return "-";
    String n = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    if (n.isEmpty()) {
      return "-";
    }
    return n.length() <= max ? n : n.substring(0, max);
  }

  private static String shortHash(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(String.valueOf(s).getBytes(StandardCharsets.UTF_8));
      
      StringBuilder sb = new StringBuilder(16);
      for (int i = 0; i < 8; i++) {
        sb.append(String.format("%02x", dig[i]));
      }
      return sb.toString();
    } catch (Exception e) {
      return "hash";
    }
  }
}
