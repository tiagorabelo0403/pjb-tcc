package com.tcc.pjb.backend.service.ui.presentation.compiler;

import java.util.Collections;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto;

final class UiTokenTable {

  private final EnumMap<UiCssTokenKey, String> map = new EnumMap<>(UiCssTokenKey.class);

  void put(UiCssTokenKey key, String value) {
    if (key == null || value == null || value.isBlank()) {
      return;
    }
    map.put(key, value);
  }

  String get(UiCssTokenKey key) {
    return map.get(key);
  }

  List<UiCssTokenDto> toDtoList() {
    List<UiCssTokenDto> out = new ArrayList<>(map.size());
    for (UiCssTokenKey k : UiCssTokenKey.values()) {
      String v = map.get(k);
      if (v != null) {
        out.add(new UiCssTokenDto(k.css(), v));
      }
    }
    return List.copyOf(out);
  }

  Map<String, String> toStringMap() {
    EnumMap<UiCssTokenKey, String> m = map;
    Map<String, String> out = new java.util.LinkedHashMap<>(m.size());
    for (UiCssTokenKey k : UiCssTokenKey.values()) {
      String v = m.get(k);
      if (v != null) {
        out.put(k.css(), v);
      }
    }
    return Collections.unmodifiableMap(out);
  }
}
