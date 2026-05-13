package com.tcc.pjb.backend.service.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.configs.security.perimeter.SecurityPerimeterProperties;
import com.tcc.pjb.backend.service.security.SecurityBlocklistService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RateLimiterServiceTest {

  @Test
  void rate_limit_disabled_allows() {
    SecurityPerimeterProperties props = new SecurityPerimeterProperties();
    props.getRatelimit().setEnabled(false);

    RateLimiterStore store = new InMemoryRateLimiterStore();
    SecurityBlocklistService block = Mockito.mock(SecurityBlocklistService.class);

    RateLimiterService svc = new RateLimiterService(props, store, block);
    RateLimitDecision d = svc.evaluate("1.1.1.1");

    assertThat(d.allowed()).isTrue();
  }

  @Test
  void below_limit_allows() {
    SecurityPerimeterProperties props = new SecurityPerimeterProperties();
    props.getRatelimit().setEnabled(true);
    props.getRatelimit().setMaxRequests(2);
    props.getRatelimit().setWindow(Duration.ofSeconds(60));

    RateLimiterStore store = new InMemoryRateLimiterStore();
    SecurityBlocklistService block = Mockito.mock(SecurityBlocklistService.class);

    RateLimiterService svc = new RateLimiterService(props, store, block);

    RateLimitDecision d1 = svc.evaluate("1.1.1.1");
    RateLimitDecision d2 = svc.evaluate("1.1.1.1");

    assertThat(d1.allowed()).isTrue();
    assertThat(d2.allowed()).isTrue();
    assertThat(d2.remaining()).isEqualTo(0);
  }

  @Test
  void above_limit_denies() {
    SecurityPerimeterProperties props = new SecurityPerimeterProperties();
    props.getRatelimit().setEnabled(true);
    props.getRatelimit().setMaxRequests(2);
    props.getRatelimit().setWindow(Duration.ofSeconds(60));

    RateLimiterStore store = new InMemoryRateLimiterStore();
    SecurityBlocklistService block = Mockito.mock(SecurityBlocklistService.class);

    RateLimiterService svc = new RateLimiterService(props, store, block);

    svc.evaluate("1.1.1.1");
    svc.evaluate("1.1.1.1");
    RateLimitDecision d3 = svc.evaluate("1.1.1.1");

    assertThat(d3.allowed()).isFalse();
    assertThat(d3.retryAfterSeconds()).isGreaterThan(0);
  }

  @Test
  void ban_is_triggered_after_repeated_violations() {
    SecurityPerimeterProperties props = new SecurityPerimeterProperties();
    props.getRatelimit().setEnabled(true);
    props.getRatelimit().setMaxRequests(1);
    props.getRatelimit().setWindow(Duration.ofSeconds(60));
    props.getRatelimit().setBanAfterViolations(2);
    props.getRatelimit().setBanTtl(Duration.ofMinutes(5));

    RateLimiterStore store = new InMemoryRateLimiterStore();
    SecurityBlocklistService block = Mockito.mock(SecurityBlocklistService.class);

    RateLimiterService svc = new RateLimiterService(props, store, block);

    svc.evaluate("9.9.9.9"); 
    svc.evaluate("9.9.9.9"); 
    svc.evaluate("9.9.9.9"); 

    verify(block, times(1)).banIp(eq("9.9.9.9"), contains("ratelimit"), eq(Duration.ofMinutes(5)));
  }

  @Test
  void path_null_does_not_throw_npe() {
    SecurityPerimeterProperties props = new SecurityPerimeterProperties();
    props.getRatelimit().setEnabled(true);
    props.getRatelimit().setMaxRequests(10);

    RateLimiterStore store = new InMemoryRateLimiterStore();
    SecurityBlocklistService block = Mockito.mock(SecurityBlocklistService.class);

    RateLimiterService svc = new RateLimiterService(props, store, block);

    RateLimitDecision d = svc.evaluate(new RateLimitContext("1.2.3.4", null, null, null));
    assertThat(d.allowed()).isTrue();
  }

  @Test
  void path_normalization_enables_rule_match() {
    SecurityPerimeterProperties props = new SecurityPerimeterProperties();
    props.getRatelimit().setEnabled(true);
    props.getRatelimit().setMaxRequests(100);
    props.getRatelimit().setWindow(Duration.ofSeconds(60));

    SecurityPerimeterProperties.Ratelimit.Rule r = new SecurityPerimeterProperties.Ratelimit.Rule();
    r.setName("lawyer");
    r.getPaths().add("/api/v1/laiane/**");
    r.setMaxRequests(1L);
    props.getRatelimit().getRules().add(r);

    RateLimiterStore store = new InMemoryRateLimiterStore();
    SecurityBlocklistService block = Mockito.mock(SecurityBlocklistService.class);
    RateLimiterService svc = new RateLimiterService(props, store, block);

    RateLimitDecision d1 = svc.evaluate(new RateLimitContext("7.7.7.7", "GET", "api/v1/laiane/lawyer//procuracoes?x=1", null));
    RateLimitDecision d2 = svc.evaluate(new RateLimitContext("7.7.7.7", "GET", "/api/v1/laiane/lawyer/procuracoes/", null));

    assertThat(d1.allowed()).isTrue();
    assertThat(d2.allowed()).isFalse();
  }
}
