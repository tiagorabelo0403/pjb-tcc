package com.tcc.pjb.backend.core.forum.routing;
public final class InboxKeyCompat {

  private InboxKeyCompat() {
  }

  public static String normalizeSecretariatInboxKey(String inboxKey) {
    if (inboxKey == null) return null;
    String k = inboxKey.trim();
    if (k.isEmpty()) return k;
    if (!k.startsWith("SEC:")) return k;

    String[] p = k.split(":");
    if (p.length >= 7) {
      return k;
    }
    if (p.length == 6) {
      return "SEC:" + p[1] + ":" + p[2] + ":" + ForumLane.COMUM.name() + ":" + p[3] + ":" + p[4] + ":" + p[5];
    }
    if (p.length == 5) {
      return "SEC:" + p[1] + ":" + p[2] + ":" + ForumLane.COMUM.name() + ":" + p[3] + ":" + p[4];
    }
    return k;
  }
}
