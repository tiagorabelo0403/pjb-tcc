package com.tcc.pjb.backend.modules.atendimento.controller;

import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadNotificationSettingsDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoUpdateThreadNotificationSettingsRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadPolicyDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoUpdateThreadPolicyRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadDigestDto;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoThreadDigestService;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoThreadPolicyService;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoThreadSettingsService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atendimento")
public class AtendimentoThreadSettingsController {

  private final AtendimentoThreadSettingsService settings;
  private final AtendimentoThreadPolicyService policy;
  private final AtendimentoThreadDigestService digest;
  private final CapabilityRateLimiter rateLimiter;

  public AtendimentoThreadSettingsController(AtendimentoThreadSettingsService settings,
                                            AtendimentoThreadPolicyService policy,
                                            AtendimentoThreadDigestService digest,
                                            CapabilityRateLimiter rateLimiter) {
    this.settings = Objects.requireNonNull(settings);
    this.policy = Objects.requireNonNull(policy);
    this.digest = Objects.requireNonNull(digest);
    this.rateLimiter = Objects.requireNonNull(rateLimiter);
  }

  @GetMapping("/threads/{threadId}/notification-settings")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<AtendimentoThreadNotificationSettingsDto> getSettings(Authentication authentication,
                                                                             @PathVariable("threadId") @Positive Long threadId) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_thread_settings_get", ApiVersion.V1);
    return ResponseEntity.ok(settings.get(threadId));
  }

  @PutMapping("/threads/{threadId}/notification-settings")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<AtendimentoThreadNotificationSettingsDto> updateSettings(Authentication authentication,
                                                                                @PathVariable("threadId") @Positive Long threadId,
                                                                                @Valid @RequestBody AtendimentoUpdateThreadNotificationSettingsRequest req) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_thread_settings_put", ApiVersion.V1);
    return ResponseEntity.ok(settings.update(threadId, req));
  }

  @GetMapping("/threads/{threadId}/policy")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<AtendimentoThreadPolicyDto> getPolicy(Authentication authentication,
                                                             @PathVariable("threadId") @Positive Long threadId) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_thread_policy_get", ApiVersion.V1);
    return ResponseEntity.ok(policy.get(threadId));
  }

  @PutMapping("/threads/{threadId}/policy")
  @PreAuthorize("hasRole('ADVOGADO')")
  public ResponseEntity<AtendimentoThreadPolicyDto> updatePolicy(Authentication authentication,
                                                                @PathVariable("threadId") @Positive Long threadId,
                                                                @Valid @RequestBody AtendimentoUpdateThreadPolicyRequest req) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_thread_policy_put", ApiVersion.V1);
    return ResponseEntity.ok(policy.update(threadId, req));
  }

  @GetMapping("/threads/{threadId}/digest")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<AtendimentoThreadDigestDto> digest(Authentication authentication,
                                                          @PathVariable("threadId") @Positive Long threadId) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_thread_digest", ApiVersion.V1);
    return ResponseEntity.ok(digest.digest(threadId));
  }
}
