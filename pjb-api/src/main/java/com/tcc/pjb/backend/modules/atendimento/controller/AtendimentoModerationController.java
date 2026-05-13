package com.tcc.pjb.backend.modules.atendimento.controller;

import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationActionRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationEventDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationMessageDetailDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationQueueItemDto;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoModerationEventQueryService;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoModerationService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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

@RestController
@RequestMapping("/api/v1/atendimento/moderacao")
public class AtendimentoModerationController {

  private final AtendimentoModerationService service;
  private final AtendimentoModerationEventQueryService events;
  private final CapabilityRateLimiter rateLimiter;

  public AtendimentoModerationController(AtendimentoModerationService service,
                                        AtendimentoModerationEventQueryService events,
                                        CapabilityRateLimiter rateLimiter) {
    this.service = service;
    this.events = events;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/queue")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<Page<AtendimentoModerationQueueItemDto>> queue(Authentication authentication,
                                                                      @RequestParam(value = "status", required = false) List<String> status,
                                                                      @RequestParam(value = "cursor", required = false) Long cursor,
                                                                      @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_moderation_queue", ApiVersion.V1);
    return ResponseEntity.ok(service.listQueue(status, cursor, limit));
  }

  @GetMapping("/messages/{messageId}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<AtendimentoModerationMessageDetailDto> detail(Authentication authentication,
                                                                     @PathVariable("messageId") @Positive Long messageId) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_moderation_detail", ApiVersion.V1);
    return ResponseEntity.ok(service.messageDetail(messageId));
  }

  @PostMapping("/messages/{messageId}/release")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<Void> release(Authentication authentication,
                                     @PathVariable("messageId") @Positive Long messageId) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_moderation_action", ApiVersion.V1);
    service.releaseMessage(messageId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/messages/{messageId}/block")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<Void> block(Authentication authentication,
                                   @PathVariable("messageId") @Positive Long messageId,
                                   @RequestBody @Valid AtendimentoModerationActionRequest req) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_moderation_action", ApiVersion.V1);
    service.blockMessage(messageId, req);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/events")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<Page<AtendimentoModerationEventDto>> listEvents(Authentication authentication,
                                                                       @RequestParam(value = "from", required = false) LocalDate from,
                                                                       @RequestParam(value = "to", required = false) LocalDate to,
                                                                       @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                       @RequestParam(value = "size", required = false, defaultValue = "50") int size) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_moderation_events", ApiVersion.V1);
    Instant f = from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
    Instant t = to != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
    return ResponseEntity.ok(events.list(f, t, page, size));
  }
}
