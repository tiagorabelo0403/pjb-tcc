package com.tcc.pjb.backend.controller.ui;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.ratelimit.FixedWindowRateLimiter;

@Component
public class UiAccessibilityRateLimiter {

  private static final long WINDOW = 60_000L;

  private final UiAccessibilityRateLimitProperties props;
  private final FixedWindowRateLimiter limiter;

  public UiAccessibilityRateLimiter(UiAccessibilityRateLimitProperties props) {
    this.props = Objects.requireNonNull(props);
    this.limiter = new FixedWindowRateLimiter(java.time.Clock.systemUTC());
  }

  public void assertAllowed(long userId, String ip) {
    if (!props.isEnabled()) return;

    String ipKey = ip == null || ip.isBlank() ? "ip:unknown" : "ip:" + sha256_12(ip);
    String userKey = userId > 0 ? "u:" + userId : "u:unknown";

    boolean okUser = limiter.allow(userKey, WINDOW, props.getPerUserPerMinute());
    boolean okIp = limiter.allow(ipKey, WINDOW, props.getPerIpPerMinute());

    if (!okUser || !okIp) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "rate limit");
    }
  }

  private static String sha256_12(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
      String hex = HexFormat.of().formatHex(dig);
      return hex.substring(0, 12);
    } catch (Exception e) {
      return Integer.toHexString(s.hashCode());
    }
  }
}
