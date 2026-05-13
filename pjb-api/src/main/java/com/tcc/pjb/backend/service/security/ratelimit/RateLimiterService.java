package com.tcc.pjb.backend.service.security.ratelimit;

import com.tcc.pjb.backend.configs.security.perimeter.SecurityPerimeterProperties;
import com.tcc.pjb.backend.service.security.SecurityBlocklistService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Service
public class RateLimiterService {

  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  private final SecurityPerimeterProperties properties;
  private final RateLimiterStore store;
  private final SecurityBlocklistService blocklistService;

  public RateLimiterService(SecurityPerimeterProperties properties,
                            RateLimiterStore store,
                            SecurityBlocklistService blocklistService) {
    this.properties = properties;
    this.store = store;
    this.blocklistService = blocklistService;
  }

  public RateLimitDecision evaluate(String clientIp) {
    return evaluate(new RateLimitContext(clientIp, null, null, null));
  }

  public RateLimitDecision evaluate(RateLimitContext ctx) {
    SecurityPerimeterProperties.Ratelimit cfg = properties.getRatelimit();
    if (cfg == null || !cfg.isEnabled()) {
      return new RateLimitDecision(true, 0, 0, 0, 0);
    }

    String ip = (ctx == null) ? null : ctx.clientIp();
    if (ip == null || ip.isBlank()) ip = "unknown";

    String method = normalizeMethod(ctx != null ? ctx.httpMethod() : null);
    String path = normalizePath(ctx != null ? ctx.path() : null);
    String userKey = normalizeUserKey(ctx != null ? ctx.userKey() : null);

    SecurityPerimeterProperties.Ratelimit.Rule rule = resolveRule(cfg, path, method);

    Duration window = safe(rule != null ? rule.getWindow() : null, safe(cfg.getWindow(), Duration.ofSeconds(60)));
    long windowSeconds = Math.max(1L, window.toSeconds());

    long limit = Math.max(1L,
        rule != null && rule.getMaxRequests() != null ? rule.getMaxRequests() : cfg.getMaxRequests());

    String ruleId = ruleId(cfg, rule);
    String subjectKey = subjectKey(rule, ip, userKey);

    long nowSec = Instant.now().getEpochSecond();
    long bucket = nowSec / windowSeconds;
    String key = cfg.getKeyPrefix() + "rule:" + ruleId + ":w" + windowSeconds + ":" + bucket + ":" + subjectKey;
    long current = store.incr(key, Duration.ofSeconds(windowSeconds + 5));

    if (current <= limit) {
      long remaining = Math.max(0L, limit - current);
      return new RateLimitDecision(true, limit, remaining, 0, current);
    }

    long retryAfter = windowSeconds - (nowSec % windowSeconds);

    Duration violationWindow = safe(rule != null ? rule.getViolationWindow() : null,
        safe(cfg.getViolationWindow(), Duration.ofMinutes(10)));
    long banAfter = Math.max(1L,
        rule != null && rule.getBanAfterViolations() != null ? rule.getBanAfterViolations() : cfg.getBanAfterViolations());
    Duration banTtl = safe(rule != null ? rule.getBanTtl() : null, safe(cfg.getBanTtl(), Duration.ofHours(1)));

    String violKey = cfg.getKeyPrefix() + "viol:" + ruleId + ":" + ip;
    long violations = store.incr(violKey, violationWindow);

    if (violations >= banAfter) {
      blocklistService.banIp(ip, "ratelimit(" + ruleId + "):" + violations, banTtl);
    }

    return new RateLimitDecision(false, limit, 0, Math.max(1L, retryAfter), current);
  }

  private SecurityPerimeterProperties.Ratelimit.Rule resolveRule(SecurityPerimeterProperties.Ratelimit cfg, String path, String method) {
    if (cfg == null) return null;
    List<SecurityPerimeterProperties.Ratelimit.Rule> rules = cfg.getRules();
    if (rules == null || rules.isEmpty()) return null;

    if (path == null) return null;

    String m = method == null ? "" : method;

    for (SecurityPerimeterProperties.Ratelimit.Rule r : rules) {
      if (r == null) continue;
      if (!matchesPath(r, path)) continue;
      if (!matchesMethod(r, m)) continue;
      return r;
    }
    return null;
  }

  private boolean matchesPath(SecurityPerimeterProperties.Ratelimit.Rule r, String path) {
    if (path == null) return false;
    List<String> paths = r.getPaths();
    if (paths == null || paths.isEmpty()) return false;
    for (String pattern : paths) {
      if (pattern == null || pattern.isBlank()) continue;
      if (pathMatcher.match(pattern.trim(), path)) return true;
    }
    return false;
  }

  private boolean matchesMethod(SecurityPerimeterProperties.Ratelimit.Rule r, String method) {
    List<String> methods = r.getMethods();
    if (methods == null || methods.isEmpty()) return true;
    for (String mm : methods) {
      if (mm == null || mm.isBlank()) continue;
      if (Objects.equals(normalizeMethod(mm), method)) return true;
    }
    return false;
  }

  private String subjectKey(SecurityPerimeterProperties.Ratelimit.Rule rule, String ip, String userKey) {
    String strategy = rule != null && rule.getKeyStrategy() != null
        ? rule.getKeyStrategy().trim().toLowerCase(Locale.ROOT)
        : "ip";
    return switch (strategy) {
      case "ip_user" -> (userKey != null ? ip + ":" + userKey : ip);
      case "ip" -> ip;
      default -> ip;
    };
  }

  private String ruleId(SecurityPerimeterProperties.Ratelimit cfg, SecurityPerimeterProperties.Ratelimit.Rule rule) {
    String base = (rule != null && rule.getName() != null && !rule.getName().isBlank())
        ? rule.getName()
        : "default";
    return base.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase(Locale.ROOT);
  }

  private String normalizeMethod(String m) {
    if (m == null) return null;
    String v = m.trim();
    return v.isEmpty() ? null : v.toUpperCase(Locale.ROOT);
  }

  private String normalizePath(String p) {
    if (p == null) return null;
    String v = p.trim();
    if (v.isEmpty()) return null;
    int q = v.indexOf('?');
    if (q >= 0) {
      v = v.substring(0, q);
    }
    v = v.replace('\\', '/');
    while (v.contains("//")) {
      v = v.replace("//", "/");
    }
    if (!v.startsWith("/")) {
      v = "/" + v;
    }
    if (v.length() > 1 && v.endsWith("/")) {
      v = v.substring(0, v.length() - 1);
    }
    return v;
  }

  private String normalizeUserKey(String u) {
    if (u == null) return null;
    String v = u.trim();
    if (v.isEmpty()) return null;
    if (v.length() > 64) v = v.substring(0, 64);
    return v;
  }

  private Duration safe(Duration v, Duration fallback) {
    if (v == null || v.isNegative() || v.isZero()) return fallback;
    return v;
  }
}
