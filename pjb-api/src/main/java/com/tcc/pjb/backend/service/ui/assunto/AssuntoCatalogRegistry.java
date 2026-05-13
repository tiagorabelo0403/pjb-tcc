package com.tcc.pjb.backend.service.ui.assunto;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.ui.UiPersona;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.service.ui.UiColorUtil;

@Component
public class AssuntoCatalogRegistry {

  private final ObjectMapper mapper;
  private final ResourceLoader resourceLoader;
  private final String externalLocation;

  private final AtomicReference<State> state = new AtomicReference<>(State.empty());

  public AssuntoCatalogRegistry(
      ObjectMapper mapper,
      ResourceLoader resourceLoader,
      @Value("${pjb.ui.assunto.catalog:}") String externalLocation
  ) {
    this.mapper = Objects.requireNonNull(mapper);
    this.resourceLoader = Objects.requireNonNull(resourceLoader);
    this.externalLocation = externalLocation == null ? "" : externalLocation.trim();
    reload();
  }

  public int version() {
    return state.get().version;
  }

  public Instant loadedAt() {
    return state.get().loadedAt;
  }

  public List<AssuntoGroup> groups() {
    return state.get().groups;
  }

  public void reload() {
    
    AssuntoCatalogFile file = readExternal().orElseGet(this::readClasspathOrDefault);
    List<AssuntoGroup> groups = sanitize(file.groups());
    int v = Math.max(1, file.version());
    state.set(new State(v, Instant.now(), groups));
  }

  private java.util.Optional<AssuntoCatalogFile> readExternal() {
    try {
      if (externalLocation != null && !externalLocation.isBlank()) {
        Resource external = resourceLoader.getResource(externalLocation);
        if (external.exists() && external.isReadable()) {
          try (InputStream in = external.getInputStream()) {
          AssuntoCatalogFile f = mapper.readValue(in, AssuntoCatalogFile.class);
          if (f != null && f.groups() != null && !f.groups().isEmpty()) {
            return java.util.Optional.of(f);
          }
          }
        }
      }
    } catch (Exception ignored) {
    }
    return java.util.Optional.empty();
  }

  private AssuntoCatalogFile readClasspathOrDefault() {
    try (InputStream in = AssuntoCatalogRegistry.class.getClassLoader()
        .getResourceAsStream("ui/national-assunto-catalog.json")) {
      if (in != null) {
        AssuntoCatalogFile f = mapper.readValue(in, AssuntoCatalogFile.class);
        if (f != null && f.groups() != null && !f.groups().isEmpty()) {
          return f;
        }
      }
    } catch (Exception ignored) {
    }
    return new AssuntoCatalogFile(1, List.of());
  }

  private static List<AssuntoGroup> sanitize(List<AssuntoGroup> groups) {
    if (groups == null || groups.isEmpty()) {
      return List.of();
    }
    List<AssuntoGroup> out = new ArrayList<>(groups.size());
    for (AssuntoGroup g : groups) {
      if (g == null) continue;
      String id = g.id() == null ? "" : g.id().trim();
      if (id.isEmpty()) continue;

      Map<UiTheme, String> colors = g.colors() == null ? Map.of() : g.colors();
      String light = UiColorUtil.normalizeHex(colors.getOrDefault(UiTheme.LIGHT, "#546E7A"));
      String dark = UiColorUtil.normalizeHex(colors.getOrDefault(UiTheme.DARK, "#B0BEC5"));

      String icon = g.icon() == null || g.icon().isBlank() ? "tag" : g.icon().trim();
      String pattern = g.pattern() == null || g.pattern().isBlank() ? "solid" : g.pattern().trim();

      Map<UiPersona, String> labels = g.labels() == null ? Map.of() : g.labels();
      Map<UiPersona, String> desc = g.descriptions() == null ? Map.of() : g.descriptions();

      List<String> match = g.matchAny() == null ? List.of() : g.matchAny().stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(s -> s.toLowerCase(Locale.ROOT))
          .toList();

      out.add(new AssuntoGroup(
          id,
          Map.of(UiTheme.LIGHT, light, UiTheme.DARK, dark),
          icon,
          pattern,
          labels,
          desc,
          match
      ));
    }
    return Collections.unmodifiableList(out);
  }

  private record State(int version, Instant loadedAt, List<AssuntoGroup> groups) {
    static State empty() {
      return new State(1, Instant.EPOCH, List.of());
    }
  }
}
