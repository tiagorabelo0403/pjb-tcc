package com.tcc.pjb.backend.controller.ui;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.ui.UiHistoryAccessApplicationService;
import com.tcc.pjb.backend.service.ui.live.UiHistoryLiveHub;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ui")
@PreAuthorize("isAuthenticated()")
public class UiHistorySseController {

  private final UiHistoryLiveHub hub;
  private final UiHistoryAccessApplicationService access;
  private final CurrentUserService currentUser;

  public UiHistorySseController(UiHistoryLiveHub hub,
                                UiHistoryAccessApplicationService access,
                                CurrentUserService currentUser) {
    this.hub = Objects.requireNonNull(hub);
    this.access = Objects.requireNonNull(access);
    this.currentUser = Objects.requireNonNull(currentUser);
  }

  @GetMapping(value = "/history/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(
      @RequestParam(name = "processoId", required = false) Long processoId,
      @RequestParam(name = "inboxKey", required = false) String inboxKey,
      @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId
  ) {
    if (processoId != null) {
      access.authorizeProcessHistoryIfPresent(processoId);
    }

    String rawKey = inboxKey == null ? null : inboxKey.trim();
    if (rawKey != null && !rawKey.isBlank()) {
      String up = rawKey.toUpperCase();

      if (up.startsWith("SEC:")) {
        Usuario u = currentUser.getOrNull();
        TipoUsuario tipo = u == null ? null : u.getTipoUsuario();
        if (tipo == null || (!tipo.isServidorJudiciario() && !tipo.isAdmin())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "perfil nao autorizado");
        }
      }

      if (up.startsWith("CIDCPF:")) {
        String targetCpf = rawKey.substring("CIDCPF:".length());
        Usuario u = currentUser.getOrNull();
        TipoUsuario tipo = u == null ? null : u.getTipoUsuario();
        String myCpf = u == null ? null : u.getCpf();
        boolean ok = myCpf != null && !myCpf.isBlank() && myCpf.equals(targetCpf);
        if (!ok) {
          if (tipo == null || (!tipo.isAdmin() && !tipo.isServidorJudiciario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "perfil nao autorizado");
          }
        }
      }

      if (up.startsWith("USR:")) {
        String raw = rawKey.substring("USR:".length());
        long targetId;
        try {
          targetId = Long.parseLong(raw.trim());
        } catch (Exception ex) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inboxKey invalid");
        }

        Usuario u = currentUser.getOrNull();
        TipoUsuario tipo = u == null ? null : u.getTipoUsuario();
        Long myId = u == null ? null : u.getId();
        boolean ok = myId != null && myId.longValue() == targetId;
        if (!ok) {
          if (tipo == null || (!tipo.isAdmin() && !tipo.isServidorJudiciario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "perfil nao autorizado");
          }
        }
      }
    }

    String topic = topic(processoId, rawKey);
    if (topic == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "processoId or inboxKey required");
    }

    return hub.register(topic, lastEventId);
  }

  private static String topic(Long processoId, String inboxKey) {
    if (processoId != null) {
      return "HIST:" + processoId;
    }
    if (inboxKey != null && !inboxKey.isBlank()) {
      return "HIST:INBOX:" + inboxKey.trim();
    }
    return null;
  }
}
