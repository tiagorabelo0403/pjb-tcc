package com.tcc.pjb.backend.core.forum.routing;

import java.util.Locale;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public enum ForumLane {
  COMUM("COM", "secretariat_inbox_view"),
  JEC("JEC", "jec_inbox_view"),
  JEF("JEF", "jef_inbox_view"),
  FAZENDA("FAZ", "fazenda_inbox_view"),
  FAMILIA("FAM", "familia_inbox_view"),
  SUCESSOES("SUC", "sucessoes_inbox_view"),
  CRIMINAL("CRI", "criminal_inbox_view"),
  JURI("JUR", "juri_inbox_view"),
  VIOLENCIA_DOMESTICA("VDM", "violencia_domestica_inbox_view"),
  INFANCIA("INF", "infancia_inbox_view"),
  TRABALHO("TRB", "trabalho_inbox_view"),
  ELEITORAL("ELE", "eleitoral_inbox_view"),
  MILITAR("MIL", "militar_inbox_view"),
  PREVIDENCIARIO("PREV", "previdenciario_inbox_view"),
  EMPRESARIAL("EMP", "empresarial_inbox_view");

  private final String token;
  private final String viewName;

  ForumLane(String token, String viewName) {
    this.token = token;
    this.viewName = viewName;
  }

  public String token() {
    return token;
  }

  public String viewName() {
    return viewName;
  }

  public boolean isJuizado() {
    return this == JEC || this == JEF;
  }

  public boolean isSpecialized() {
    return this != COMUM;
  }

  public boolean requiresAudienceDesk() {
    return this == FAMILIA || this == CRIMINAL || this == JURI || this == VIOLENCIA_DOMESTICA || this == INFANCIA || this == TRABALHO || this == ELEITORAL || this == MILITAR;
  }

  public String dashboardBucket() {
    return isJuizado() ? "JUIZADOS" : isSpecialized() ? "ESPECIALIZADAS" : "COMUM";
  }


  public static ForumLane resolve(String raw) {
    return fromToken(raw).orElse(COMUM);
  }

  public static java.util.Optional<ForumLane> fromToken(String raw) {
    if (raw == null || raw.isBlank()) {
      return java.util.Optional.empty();
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    for (ForumLane lane : values()) {
      if (lane.name().equals(normalized) || lane.token.equals(normalized)) {
        return java.util.Optional.of(lane);
      }
    }
    return java.util.Optional.empty();
  }

  public static ForumLane fromProcess(Processo p) {
    if (p == null) {
      return COMUM;
    }
    RitoProcessual rito = p.getRito();
    RamoDireito ramo = p.getRamoDireito();
    if (rito != null) {
      if (rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL || rito == RitoProcessual.PREVIDENCIARIO_JEF) {
        return JEF;
      }
      if (rito == RitoProcessual.JUIZADO_ESPECIAL || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL) {
        return JEC;
      }
      if (rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA || rito.isTribFazenda() || rito == RitoProcessual.EXECUCAO_FISCAL) {
        return FAZENDA;
      }
      if (rito == RitoProcessual.CIVIL_FAMILIA_DIVORCIO || rito == RitoProcessual.CIVIL_FAMILIA_ALIMENTOS || rito == RitoProcessual.CIVIL_DISSOLUCAO_CASAMENTO) {
        return FAMILIA;
      }
      if (rito == RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO) {
        return SUCESSOES;
      }
      if (rito == RitoProcessual.PENAL_MARIA_DA_PENHA) {
        return VIOLENCIA_DOMESTICA;
      }
      if (rito == RitoProcessual.TRIBUNAL_JURI) {
        return JURI;
      }
      if (rito.isPenal()) {
        return CRIMINAL;
      }
      if (rito.isInfancia()) {
        return INFANCIA;
      }
      if (rito.isTrabalhista()) {
        return TRABALHO;
      }
      if (rito.isEleitoral()) {
        return ELEITORAL;
      }
      if (rito.isMilitar()) {
        return MILITAR;
      }
      if (rito.isPrevidenciario()) {
        return PREVIDENCIARIO;
      }
      if (rito.isEmpresarial()) {
        return EMPRESARIAL;
      }
      String r = rito.name().toUpperCase(Locale.ROOT);
      if (r.startsWith("JUIZADO_ESPECIAL")) {
        return JEC;
      }
    }
    if (ramo == RamoDireito.TRABALHISTA) {
      return TRABALHO;
    }
    if (ramo == RamoDireito.ELEITORAL) {
      return ELEITORAL;
    }
    if (ramo == RamoDireito.MILITAR) {
      return MILITAR;
    }
    if (ramo == RamoDireito.PREVIDENCIARIO) {
      return PREVIDENCIARIO;
    }
    if (ramo == RamoDireito.EMPRESARIAL) {
      return EMPRESARIAL;
    }
    if (ramo == RamoDireito.FAMILIA) {
      return FAMILIA;
    }
    if (ramo == RamoDireito.PENAL) {
      return CRIMINAL;
    }
    return COMUM;
  }
}
