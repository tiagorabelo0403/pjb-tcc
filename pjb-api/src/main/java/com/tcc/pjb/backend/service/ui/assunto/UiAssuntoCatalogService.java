package com.tcc.pjb.backend.service.ui.assunto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.ui.UiAssuntoGroupDto;
import com.tcc.pjb.backend.model.dto.ui.UiPersona;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.service.ui.UiColorUtil;

@Service
public class UiAssuntoCatalogService {

  private final AssuntoCatalogRegistry registry;

  public UiAssuntoCatalogService(AssuntoCatalogRegistry registry) {
    this.registry = Objects.requireNonNull(registry);
  }

  public int version() {
    return registry.version();
  }

  public List<UiAssuntoGroupDto> list(UiTheme theme, UiPersona persona) {
    UiTheme t = theme == null ? UiTheme.LIGHT : theme;
    UiPersona p = persona == null ? UiPersona.OUTRO : persona;

    List<AssuntoGroup> groups = registry.groups();
    if (groups.isEmpty()) {
      return List.of();
    }
    List<UiAssuntoGroupDto> out = new ArrayList<>(groups.size());
    for (AssuntoGroup g : groups) {
      String hex = UiColorUtil.normalizeHex(g.colors().getOrDefault(t, "#546E7A"));
      String on = UiColorUtil.pickOnColor(hex);

      String label = firstNonBlank(
          g.labels() == null ? null : g.labels().get(p),
          g.labels() == null ? null : g.labels().get(UiPersona.OUTRO),
          g.id()
      );
      String desc = firstNonBlank(
          g.descriptions() == null ? null : g.descriptions().get(p),
          g.descriptions() == null ? null : g.descriptions().get(UiPersona.OUTRO),
          ""
      );

      out.add(new UiAssuntoGroupDto(
          g.id(),
          hex,
          on,
          g.icon(),
          g.pattern(),
          label,
          desc
      ));
    }
    return List.copyOf(out);
  }

  private static String firstNonBlank(String a, String b, String fallback) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return fallback == null ? "" : fallback;
  }
}
