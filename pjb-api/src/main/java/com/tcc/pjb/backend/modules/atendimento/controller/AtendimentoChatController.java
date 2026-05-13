package com.tcc.pjb.backend.modules.atendimento.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoCreateThreadRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMarkDeliveredRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMarkReadRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMessageDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoSendMessageRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAdvogadoDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoTypingRequest;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoChatService;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoInboxLiveHub;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.sse.TooManySseConnectionsException;

@RestController
@RequestMapping("/api/v1/atendimento")
public class AtendimentoChatController {

  private final AtendimentoChatService service;
  private final AtendimentoInboxLiveHub liveHub;
  private final CapabilityRateLimiter rateLimiter;
  private final int maxSubscribersPerTopic;

  public AtendimentoChatController(AtendimentoChatService service,
                                  AtendimentoInboxLiveHub liveHub,
                                  CapabilityRateLimiter rateLimiter,
                                  @Value("${pjb.atendimento.sse.maxSubscribersPerTopic:5}") int maxSubscribersPerTopic) {
    this.service = service;
    this.liveHub = liveHub;
    this.rateLimiter = rateLimiter;
    this.maxSubscribersPerTopic = Math.max(2, maxSubscribersPerTopic);
  }

  @GetMapping("/threads")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Page<AtendimentoThreadDto>> threads(Authentication authentication, Pageable pageable) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_threads", ApiVersion.V1);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(10)).cachePrivate())
        .body(service.listThreads(pageable));
  }

  @PostMapping("/threads")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<AtendimentoThreadDto> createThread(Authentication authentication, @Valid @RequestBody AtendimentoCreateThreadRequest req) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_thread_create", ApiVersion.V1);
    return ResponseEntity.ok(service.createThread(req));
  }

  @GetMapping("/threads/{threadId}/messages")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Page<AtendimentoMessageDto>> messages(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @RequestParam(value = "afterId", required = false) Long afterId,
      @RequestParam(value = "beforeId", required = false) Long beforeId,
      @RequestParam(value = "limit", defaultValue = "50") @Min(1) @Max(200) int limit
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_messages", ApiVersion.V1);
    return ResponseEntity.ok(service.listMessages(threadId, afterId, beforeId, limit));
  }

  

  @GetMapping("/threads/{threadId}/messages/around/{messageId}")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<java.util.List<AtendimentoMessageDto>> messagesAround(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @PathVariable("messageId") @Positive Long messageId,
      @RequestParam(value = "before", defaultValue = "30") @Min(0) @Max(200) int beforeCount,
      @RequestParam(value = "after", defaultValue = "30") @Min(0) @Max(200) int afterCount
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_messages_around", ApiVersion.V1);
    return ResponseEntity.ok(service.listMessagesAround(threadId, messageId, beforeCount, afterCount));
  }
@PostMapping("/threads/{threadId}/messages")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<AtendimentoMessageDto> send(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @Valid @RequestBody AtendimentoSendMessageRequest req
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_send", ApiVersion.V1);
    return ResponseEntity.ok(service.sendMessage(threadId, req));
  }

  @PostMapping("/threads/{threadId}/read")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Void> read(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @RequestBody AtendimentoMarkReadRequest req
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_read", ApiVersion.V1);
    service.markRead(threadId, req);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/threads/{threadId}/delivered")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Void> delivered(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @RequestBody AtendimentoMarkDeliveredRequest req
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_delivered", ApiVersion.V1);
    service.markDelivered(threadId, req);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/threads/{threadId}/typing")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<Void> typing(
      Authentication authentication,
      @PathVariable("threadId") @Positive Long threadId,
      @RequestBody AtendimentoTypingRequest req
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_typing", ApiVersion.V1);
    service.typing(threadId, req != null && req.typing());
    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/inbox/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public SseEmitter inboxStream(
      Authentication authentication,
      @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_sse", ApiVersion.V1);
    String topic = service.inboxTopicForCurrentUser();
    if (liveHub.activeSubscribers(topic) >= maxSubscribersPerTopic) {
      throw new TooManySseConnectionsException("too many sse");
    }
    return liveHub.register(topic, lastEventId);
  }

  @GetMapping("/processos/{processoId}/threads")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
  public ResponseEntity<java.util.List<AtendimentoThreadDto>> threadsForProcesso(
      Authentication authentication,
      @PathVariable("processoId") @Positive Long processoId
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_threads_processo", ApiVersion.V1);
    return ResponseEntity.ok(service.listThreadsForProcesso(processoId));
  }

  @GetMapping("/processos/{processoId}/advogados")
  @PreAuthorize("hasRole('CIDADAO')")
  public ResponseEntity<java.util.List<AtendimentoAdvogadoDto>> advogadosForProcesso(
      Authentication authentication,
      @PathVariable("processoId") @Positive Long processoId
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_advogados_processo", ApiVersion.V1);
    return ResponseEntity.ok(service.listAdvogadosForProcesso(processoId));
  }
}
