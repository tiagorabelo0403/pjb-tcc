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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoChecklistCreateItemRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoChecklistItemDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoChecklistUpdateItemRequest;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoChecklistService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RestController
@RequestMapping("/api/v1/atendimento")
public class AtendimentoChecklistController {

  private final AtendimentoChecklistService service;
  private final CapabilityRateLimiter rateLimiter;

  public AtendimentoChecklistController(AtendimentoChecklistService service, CapabilityRateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/threads/{threadId}/checklist/items")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Page<AtendimentoChecklistItemDto>> list(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_checklist_list", ApiVersion.V1);
    return ResponseEntity.ok(service.list(threadId, page, size));
  }

  @PostMapping("/threads/{threadId}/checklist/items")
  @PreAuthorize("hasRole('ADVOGADO')")
  public ResponseEntity<AtendimentoChecklistItemDto> create(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @Valid @RequestBody AtendimentoChecklistCreateItemRequest req
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_checklist_create", ApiVersion.V1);
    return ResponseEntity.ok(service.create(threadId, req));
  }

  @PutMapping("/threads/{threadId}/checklist/items/{itemId}")
  @PreAuthorize("hasRole('ADVOGADO')")
  public ResponseEntity<AtendimentoChecklistItemDto> update(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @PathVariable("itemId") @Positive Long itemId,
      @Valid @RequestBody AtendimentoChecklistUpdateItemRequest req
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_checklist_update", ApiVersion.V1);
    return ResponseEntity.ok(service.update(threadId, itemId, req));
  }

  @PostMapping("/threads/{threadId}/checklist/items/{itemId}/done")
  @PreAuthorize("hasRole('ADVOGADO')")
  public ResponseEntity<AtendimentoChecklistItemDto> done(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @PathVariable("itemId") @Positive Long itemId
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_checklist_done", ApiVersion.V1);
    return ResponseEntity.ok(service.markDone(threadId, itemId));
  }

  @PostMapping("/threads/{threadId}/checklist/items/{itemId}/reopen")
  @PreAuthorize("hasRole('ADVOGADO')")
  public ResponseEntity<AtendimentoChecklistItemDto> reopen(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @PathVariable("itemId") @Positive Long itemId
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_checklist_reopen", ApiVersion.V1);
    return ResponseEntity.ok(service.reopen(threadId, itemId));
  }

  @PostMapping("/threads/{threadId}/checklist/items/{itemId}/cancel")
  @PreAuthorize("hasRole('ADVOGADO')")
  public ResponseEntity<AtendimentoChecklistItemDto> cancel(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @PathVariable("itemId") @Positive Long itemId
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_checklist_cancel", ApiVersion.V1);
    return ResponseEntity.ok(service.cancel(threadId, itemId));
  }
}
