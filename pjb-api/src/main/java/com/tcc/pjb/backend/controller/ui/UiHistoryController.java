package com.tcc.pjb.backend.controller.ui;

import java.time.Duration;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiHistoryEntryDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.ui.UiHistoryAccessApplicationService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@RestController
@RequestMapping("/api/v1/ui")
@PreAuthorize("isAuthenticated()")
public class UiHistoryController {

  private final UiHistoryService history;
  private final UiHistoryAccessApplicationService access;
  private final CurrentUserService currentUser;

  public UiHistoryController(UiHistoryService history,
                             UiHistoryAccessApplicationService access,
                             CurrentUserService currentUser) {
    this.history = Objects.requireNonNull(history);
    this.access = Objects.requireNonNull(access);
    this.currentUser = Objects.requireNonNull(currentUser);
  }

  @GetMapping("/history")
  public ResponseEntity<Page<UiHistoryEntryDto>> history(
      @RequestParam(name = "processoId", required = false) Long processoId,
      @RequestParam(name = "workItemId", required = false) Long workItemId,
      @RequestParam(name = "inboxKey", required = false) String inboxKey,
      @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch,
      @RequestParam(name = "page", required = false, defaultValue = "0") int page,
      @RequestParam(name = "size", required = false, defaultValue = "50") int size
  ) {
    int p = Math.max(0, page);
    int s = Math.min(200, Math.max(1, size));
    Pageable pageable = PageRequest.of(p, s);

    if (workItemId != null) {
      access.authorizeWorkItemHistoryIfPresent(workItemId);
      String etag = history.etagForWorkItemHistory(workItemId, pageable);
      if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(304)
            .eTag(etag)
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate())
            .build();
      }
      return ResponseEntity.ok()
          .eTag(etag)
          .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate())
          .body(history.historyByWorkItemId(workItemId, pageable));
    }

    if (processoId != null) {
      access.authorizeProcessHistoryIfPresent(processoId);
      String etag = history.etagForProcessoHistory(processoId, pageable);
      if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(304)
            .eTag(etag)
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate())
            .build();
      }
      return ResponseEntity.ok()
          .eTag(etag)
          .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate())
          .body(history.historyByProcessoId(processoId, pageable));
    }

    if (inboxKey != null && !inboxKey.isBlank()) {
      String key = inboxKey.trim();
      String up = key.toUpperCase();

      if (up.startsWith("SEC:")) {
        Usuario u = currentUser.getOrNull();
        TipoUsuario tipo = u == null ? null : u.getTipoUsuario();
        if (tipo == null || (!tipo.isServidorJudiciario() && !tipo.isAdmin())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "perfil nao autorizado");
        }
      }

      if (up.startsWith("CIDCPF:")) {
        String targetCpf = key.substring("CIDCPF:".length());
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
        String raw = key.substring("USR:".length());
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

      String etag = history.etagForInboxHistory(key, pageable);
      if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(304)
            .eTag(etag)
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate())
            .build();
      }
      return ResponseEntity.ok()
          .eTag(etag)
          .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate())
          .body(history.historyByInboxKey(key, pageable));
    }

    return ResponseEntity.badRequest().build();
  }
}
