package com.tcc.pjb.backend.core.forum.routing;

import java.util.Objects;

public record JudicialOrganRef(String code, JudicialOrganKind kind, String displayName) {

  public JudicialOrganRef {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(kind, "kind");
    code = code.trim();
    if (code.isEmpty() || code.length() > 16) {
      throw new IllegalArgumentException("organ code inválido");
    }
    if (displayName != null && displayName.length() > 64) {
      displayName = displayName.substring(0, 64);
    }
  }

  public static JudicialOrganRef unknown() {
    return new JudicialOrganRef("UNKNOWN", JudicialOrganKind.UNKNOWN, "Desconhecido");
  }
}
