package com.tcc.pjb.backend.core.forum.routing;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tcc.pjb.backend.domain.enums.TipoJustica;

public final class CnjJusticeParser {

  private CnjJusticeParser() {
  }

  private static final Pattern CNJ = Pattern.compile("\\b(\\d{7})-(\\d{2})\\.(\\d{4})\\.(\\d)\\.(\\d{2})\\.(\\d{4})\\b");

  public static Optional<TipoJustica> tryResolveTipoJustica(String numeroUnificado) {
    if (numeroUnificado == null || numeroUnificado.isBlank()) {
      return Optional.empty();
    }
    Matcher m = CNJ.matcher(numeroUnificado);
    if (!m.find()) {
      return Optional.empty();
    }
    String j = m.group(4);
    for (TipoJustica t : TipoJustica.values()) {
      if (t != null && j.equals(t.getCodigoCNJ())) {
        return Optional.of(t);
      }
    }
    return Optional.empty();
  }
}
