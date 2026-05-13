package com.tcc.pjb.backend.service.ui;

import java.util.EnumMap;
import java.util.Map;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.UiToken;

public final class UiPalette {

  public record ColorPair(String hex, String onHex) {
  }

  private static final Map<UiToken, ColorPair> LIGHT = new EnumMap<>(UiToken.class);
  private static final Map<UiToken, ColorPair> DARK = new EnumMap<>(UiToken.class);

  static {
    
    put(LIGHT, UiToken.URGENTE, "#C62828", "#FFFFFF");
    put(LIGHT, UiToken.ATRASADO, "#B71C1C", "#FFFFFF");
    put(LIGHT, UiToken.PENDENTE, "#F9A825", "#1B1B1B");
    put(LIGHT, UiToken.EM_EXECUCAO, "#1565C0", "#FFFFFF");
    put(LIGHT, UiToken.CONCLUIDO, "#2E7D32", "#FFFFFF");

    put(LIGHT, UiToken.ENCERRADO, "#546E7A", "#FFFFFF");
    put(LIGHT, UiToken.PROCEDENTE, "#1B5E20", "#FFFFFF");
    put(LIGHT, UiToken.IMPROCEDENTE, "#8E0000", "#FFFFFF");
    put(LIGHT, UiToken.PARCIAL, "#EF6C00", "#FFFFFF");

    put(LIGHT, UiToken.RECURSO, "#6A1B9A", "#FFFFFF");
    put(LIGHT, UiToken.AUDIENCIA, "#00796B", "#FFFFFF");
    put(LIGHT, UiToken.PERICIA, "#283593", "#FFFFFF");
    put(LIGHT, UiToken.CITACAO_INTIMACAO, "#E65100", "#FFFFFF");
    put(LIGHT, UiToken.CALCULO, "#006064", "#FFFFFF");
    put(LIGHT, UiToken.DECISAO, "#37474F", "#FFFFFF");
    put(LIGHT, UiToken.DOCUMENTO, "#455A64", "#FFFFFF");

    put(LIGHT, UiToken.BLOQUEANTE, "#AD1457", "#FFFFFF");
    put(LIGHT, UiToken.SIGILOSO, "#424242", "#FFFFFF");

    put(LIGHT, UiToken.INFO, "#546E7A", "#FFFFFF");
    put(LIGHT, UiToken.NOTIFICADO, "#25D366", "#0B0B0B");
    put(LIGHT, UiToken.NEUTRO, "#90A4AE", "#1B1B1B");

    
    put(DARK, UiToken.URGENTE, "#EF5350", "#1B1B1B");
    put(DARK, UiToken.ATRASADO, "#FF1744", "#1B1B1B");
    put(DARK, UiToken.PENDENTE, "#FFCA28", "#1B1B1B");
    put(DARK, UiToken.EM_EXECUCAO, "#42A5F5", "#0B0B0B");
    put(DARK, UiToken.CONCLUIDO, "#66BB6A", "#0B0B0B");

    put(DARK, UiToken.ENCERRADO, "#B0BEC5", "#0B0B0B");
    put(DARK, UiToken.PROCEDENTE, "#69F0AE", "#0B0B0B");
    put(DARK, UiToken.IMPROCEDENTE, "#FF5252", "#0B0B0B");
    put(DARK, UiToken.PARCIAL, "#FFB74D", "#0B0B0B");

    put(DARK, UiToken.RECURSO, "#CE93D8", "#0B0B0B");
    put(DARK, UiToken.AUDIENCIA, "#4DB6AC", "#0B0B0B");
    put(DARK, UiToken.PERICIA, "#9FA8DA", "#0B0B0B");
    put(DARK, UiToken.CITACAO_INTIMACAO, "#FF8A65", "#0B0B0B");
    put(DARK, UiToken.CALCULO, "#4DD0E1", "#0B0B0B");
    put(DARK, UiToken.DECISAO, "#CFD8DC", "#0B0B0B");
    put(DARK, UiToken.DOCUMENTO, "#B0BEC5", "#0B0B0B");

    put(DARK, UiToken.BLOQUEANTE, "#F06292", "#0B0B0B");
    put(DARK, UiToken.SIGILOSO, "#E0E0E0", "#0B0B0B");

    put(DARK, UiToken.INFO, "#B0BEC5", "#0B0B0B");
    put(DARK, UiToken.NOTIFICADO, "#00A884", "#0B0B0B");
    put(DARK, UiToken.NEUTRO, "#78909C", "#0B0B0B");
  }

  public static ColorPair color(UiTheme theme, UiToken token) {
    if (token == null) {
      token = UiToken.NEUTRO;
    }
    return (theme == UiTheme.DARK ? DARK : LIGHT).getOrDefault(token, LIGHT.get(UiToken.NEUTRO));
  }

  private static void put(Map<UiToken, ColorPair> map, UiToken token, String hex, String onHex) {
    map.put(token, new ColorPair(hex, onHex));
  }

  private UiPalette() {
  }
}
