package com.tcc.pjb.backend.service.secretariat.triage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record TriageSignal(
    UrgencyLevel level,
    int score,
    List<String> tags,
    List<String> keywords,
    String rationale
) {
  public TriageSignal {
    if (score < 0 || score > 100) {
      throw new IllegalArgumentException("score out of range");
    }
    tags = tags == null ? List.of() : List.copyOf(tags);
    keywords = keywords == null ? List.of() : List.copyOf(keywords);
    rationale = rationale == null ? "" : rationale.trim();
  }

  public boolean hasTag(String tag) {
    if (tag == null || tag.isBlank()) {
      return false;
    }
    String expected = normalizeToken(tag);
    for (String value : tags) {
      if (expected.equals(normalizeToken(value))) {
        return true;
      }
    }
    return false;
  }

  public boolean hasAnyTag(String... candidates) {
    if (candidates == null) {
      return false;
    }
    for (String candidate : candidates) {
      if (hasTag(candidate)) {
        return true;
      }
    }
    return false;
  }

  public Set<String> normalizedTags() {
    LinkedHashSet<String> out = new LinkedHashSet<>();
    for (String tag : tags) {
      String normalized = normalizeToken(tag);
      if (!normalized.isBlank()) {
        out.add(normalized);
      }
    }
    return Set.copyOf(out);
  }

  public String scoreBand() {
    if (score >= 85) {
      return "MAX";
    }
    if (score >= 65) {
      return "ALTO";
    }
    if (score >= 35) {
      return "MEDIO";
    }
    if (score >= 15) {
      return "BAIXO_COM_SINAL";
    }
    return "RESIDUAL";
  }

  public boolean requiresImmediateDesk() {
    return level == UrgencyLevel.CRITICAL || hasAnyTag("HABEAS_CORPUS", "MEDIDA_PROTETIVA", "UTI", "ALVARA", "PRISAO", "MEDICAMENTO");
  }

  public boolean isHearingSensitive() {
    return hasAnyTag("AUDIENCIA", "PRAZO_CURTO");
  }

  public boolean isEnforcementSensitive() {
    return hasAnyTag("SISBAJUD", "RENAJUD", "INFOJUD", "PENHORA");
  }

  public String dominantAxis() {
    if (hasAnyTag("MEDIDA_PROTETIVA", "VIOLENCIA_DOMESTICA")) {
      return "VIOLENCIA_DOMESTICA";
    }
    if (hasAnyTag("HABEAS_CORPUS", "ALVARA", "PRISAO")) {
      return "PENAL_URGENTE";
    }
    if (hasAnyTag("UTI", "MEDICAMENTO", "SAUDE")) {
      return "SAUDE";
    }
    if (hasAnyTag("ALIMENTOS", "FAMILIA")) {
      return "FAMILIA";
    }
    if (hasAnyTag("CRIANCA", "INFANCIA", "ADOCAO")) {
      return "INFANCIA";
    }
    if (isEnforcementSensitive()) {
      return "CUMPRIMENTO";
    }
    return level != null ? level.name() : "LOW";
  }

  private static String normalizeToken(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return raw.trim().toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("(^_|_$)", "");
  }
}
