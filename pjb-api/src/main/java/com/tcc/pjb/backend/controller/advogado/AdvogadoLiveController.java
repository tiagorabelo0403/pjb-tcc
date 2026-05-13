package com.tcc.pjb.backend.controller.advogado;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.ui.live.UiHistoryLiveHub;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RestController
@RequestMapping("/api/v1/advogado")
public class AdvogadoLiveController {

  private final UiHistoryLiveHub hub;
  private final CurrentUserService currentUser;
  private final CapabilityRateLimiter rateLimiter;

  public AdvogadoLiveController(UiHistoryLiveHub hub, CurrentUserService currentUser, CapabilityRateLimiter rateLimiter) {
    this.hub = hub;
    this.currentUser = currentUser;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping(value = "/live/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
  public SseEmitter stream(
      Authentication authentication,
      @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId
  ) {
    if (rateLimiter != null) {
      rateLimiter.enforce(CapabilityRateLimitDomain.LAWYER, authentication, "advogado_sse", ApiVersion.V1);
    }
    Usuario u = currentUser.getRequired();
    Long uid = u.getId();
    if (uid == null || uid <= 0) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "usuario invalido");
    }
    String topic = "HIST:INBOX:USR:" + uid;
    if (hub.activeSubscribers(topic) >= 2) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Limite de conexões SSE atingido");
    }
    return hub.register(topic, lastEventId);
  }
}
