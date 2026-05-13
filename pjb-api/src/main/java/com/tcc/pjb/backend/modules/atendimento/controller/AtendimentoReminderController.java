package com.tcc.pjb.backend.modules.atendimento.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoCreateReminderRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoReminderDto;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoReminderService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RestController
@RequestMapping("/api/v1/atendimento")
public class AtendimentoReminderController {

  private final AtendimentoReminderService service;
  private final CapabilityRateLimiter rateLimiter;

  public AtendimentoReminderController(AtendimentoReminderService service, CapabilityRateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping("/threads/{threadId}/reminders")
  @PreAuthorize("hasRole('ADVOGADO')")
  public ResponseEntity<AtendimentoReminderDto> create(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @Valid @RequestBody AtendimentoCreateReminderRequest req
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_reminder_create", ApiVersion.V1);
    return ResponseEntity.ok(service.create(threadId, req));
  }

  @GetMapping("/threads/{threadId}/reminders")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Page<AtendimentoReminderDto>> list(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_reminders", ApiVersion.V1);
    return ResponseEntity.ok(service.list(threadId, page, size));
  }

  @PostMapping("/reminders/{reminderId}/cancel")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Void> cancel(
      Authentication authentication,
      @PathVariable("reminderId") @Positive Long reminderId
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_reminder_cancel", ApiVersion.V1);
    service.cancel(reminderId);
    return ResponseEntity.ok().build();
  }
}
