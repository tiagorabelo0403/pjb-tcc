package com.tcc.pjb.backend.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.pjb.backend.model.dto.processo.ProcessoNoteCreateRequest;
import com.tcc.pjb.backend.model.dto.processo.ProcessoNoteDto;
import com.tcc.pjb.backend.model.dto.processo.ProcessoNoteUpdateRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processo.ProcessoNoteService;

@RestController
@RequestMapping("/api/v1/processos")
public class ProcessoNoteController {

  private final ProcessoNoteService service;
  private final CapabilityRateLimiter rateLimiter;

  public ProcessoNoteController(ProcessoNoteService service, CapabilityRateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/{processoId}/notes")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO','SERVIDOR','SERVIDOR_FORUM','JUIZ','DESEMBARGADOR','MINISTRO','ADMINISTRADOR')")
  public ResponseEntity<List<ProcessoNoteDto>> list(Authentication authentication, @PathVariable("processoId") Long processoId) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "process_notes_list", ApiVersion.V1);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(15)).cachePrivate())
        .body(service.list(processoId));
  }

  @PostMapping("/{processoId}/notes")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO','SERVIDOR','SERVIDOR_FORUM','JUIZ','DESEMBARGADOR','MINISTRO','ADMINISTRADOR')")
  public ResponseEntity<ProcessoNoteDto> create(
      Authentication authentication,
      @PathVariable("processoId") Long processoId,
      @RequestBody ProcessoNoteCreateRequest req) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "process_notes_create", ApiVersion.V1);
    return ResponseEntity.ok(service.create(processoId, req));
  }

  @PutMapping("/{processoId}/notes/{noteId}")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO','SERVIDOR','SERVIDOR_FORUM','JUIZ','DESEMBARGADOR','MINISTRO','ADMINISTRADOR')")
  public ResponseEntity<ProcessoNoteDto> update(
      Authentication authentication,
      @PathVariable("processoId") Long processoId,
      @PathVariable("noteId") Long noteId,
      @RequestBody ProcessoNoteUpdateRequest req) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "process_notes_update", ApiVersion.V1);
    return ResponseEntity.ok(service.update(processoId, noteId, req));
  }

  @DeleteMapping("/{processoId}/notes/{noteId}")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO','SERVIDOR','SERVIDOR_FORUM','JUIZ','DESEMBARGADOR','MINISTRO','ADMINISTRADOR')")
  public ResponseEntity<Void> delete(
      Authentication authentication,
      @PathVariable("processoId") Long processoId,
      @PathVariable("noteId") Long noteId) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "process_notes_delete", ApiVersion.V1);
    service.delete(processoId, noteId);
    return ResponseEntity.ok().build();
  }
}
