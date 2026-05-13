package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/cidadao")
public class CidadaoLiveController {

    private final UiHistoryLiveHub hub;
    private final CurrentUserService currentUser;
    private final CapabilityRateLimiter rateLimiter;

    public CidadaoLiveController(UiHistoryLiveHub hub, CurrentUserService currentUser, CapabilityRateLimiter rateLimiter) {
        this.hub = hub;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(value = "/live/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('CIDADAO')")
    public SseEmitter stream(
            Authentication authentication,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_sse", ApiVersion.V1);
        Usuario u = currentUser.getRequired();
        String cpf = u.getCpf();
        if (cpf == null || cpf.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "CPF não encontrado no perfil");
        }
        String topic = "HIST:INBOX:CIDCPF:" + cpf;
        if (hub.activeSubscribers(topic) >= 2) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Limite de conexões SSE atingido");
        }
        return hub.register(topic, lastEventId);
    }
}
