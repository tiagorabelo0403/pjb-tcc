package com.tcc.pjb.backend.service.ui.accessibility.governance;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AccessibilityAbacRegistry {

  private final AccessibilityAbacProperties props;
  private final ResourceLoader loader;
  private final ObjectMapper mapper;

  private volatile Cache cache;

  public AccessibilityAbacRegistry(AccessibilityAbacProperties props, ResourceLoader loader, ObjectMapper mapper) {
    this.props = Objects.requireNonNull(props, "props");
    this.loader = Objects.requireNonNull(loader, "loader");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.cache = new Cache(load());
  }

  public AccessibilityAbacPolicyFile policy() {
    Cache c = cache;
    if (c.shouldReload(props.getPolicyFile())) {
      cache = new Cache(load());
    }
    return cache.file;
  }

  public AccessibilityAbacPolicyFile.Decision decide(AccessibilitySubject s) {
    AccessibilityAbacPolicyFile f = policy();
    AccessibilityAbacPolicyFile.Decision def = f.defaultDecision();

    List<AccessibilityAbacPolicyFile.Rule> rules = f.rules() == null ? List.of() : f.rules();
    for (AccessibilityAbacPolicyFile.Rule r : rules) {
      if (r == null || r.match() == null || r.decision() == null) continue;
      if (matches(r.match(), s)) {
        return r.decision();
      }
    }
    return def;
  }

  private boolean matches(AccessibilityAbacPolicyFile.Match m, AccessibilitySubject s) {
    return matchOne(m.uf(), s.uf())
        && matchOne(m.comarca(), s.comarca())
        && matchOne(m.tipoUsuario(), s.tipoUsuario())
        && matchOne(m.enteFederativo(), s.enteFederativo());
  }

  private boolean matchOne(String rule, String val) {
    if (rule == null || rule.isBlank() || "*".equals(rule.trim())) return true;
    if (val == null || val.isBlank()) return false;
    return rule.trim().equalsIgnoreCase(val.trim());
  }

  private AccessibilityAbacPolicyFile load() {
    AccessibilityAbacPolicyFile f = loadExternal(props.getPolicyFile());
    if (f != null) return f;
    f = loadClasspath(props.getClasspathResource());
    if (f != null) return f;
    return new AccessibilityAbacPolicyFile(1, new AccessibilityAbacPolicyFile.Decision(true, 320, 0L, 0L), List.of());
  }

  private AccessibilityAbacPolicyFile loadClasspath(String loc) {
    try {
      if (loc == null || loc.isBlank()) return null;
      Resource r = loader.getResource(loc);
      if (!r.exists()) return null;
      try (InputStream in = r.getInputStream()) {
        return mapper.readValue(in, AccessibilityAbacPolicyFile.class);
      }
    } catch (Exception e) {
      return null;
    }
  }

  private AccessibilityAbacPolicyFile loadExternal(String file) {
    try {
      if (file == null || file.isBlank()) return null;
      Path p = Path.of(file);
      if (!Files.exists(p)) return null;
      try (InputStream in = Files.newInputStream(p)) {
        return mapper.readValue(in, AccessibilityAbacPolicyFile.class);
      }
    } catch (Exception e) {
      return null;
    }
  }

  private record Cache(AccessibilityAbacPolicyFile file, long externalLastModified, String externalPath) {
    Cache(AccessibilityAbacPolicyFile file) {
      this(file, -1L, null);
    }

    boolean shouldReload(String currentPath) {
      if (currentPath == null || currentPath.isBlank()) return false;
      try {
        Path p = Path.of(currentPath);
        long lm = Files.exists(p) ? Files.getLastModifiedTime(p).toMillis() : -1L;
        if (!Objects.equals(externalPath, currentPath)) return true;
        return lm != externalLastModified;
      } catch (Exception e) {
        return false;
      }
    }
  }

  public record AccessibilitySubject(String uf, String comarca, String tipoUsuario, String enteFederativo) {
    public static AccessibilitySubject of(String uf, String comarca, String tipoUsuario, String enteFederativo) {
      return new AccessibilitySubject(norm(uf), norm(comarca), norm(tipoUsuario), norm(enteFederativo));
    }

    private static String norm(String s) {
      if (s == null) return null;
      String t = s.trim();
      if (t.isEmpty()) return null;
      return t.toUpperCase(Locale.ROOT);
    }
  }
}
