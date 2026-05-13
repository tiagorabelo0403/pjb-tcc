package com.tcc.pjb.backend.controller.ui;

import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationBundleDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingPreferenceDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingPreferenceUpdateRequestDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.ui.presentation.UiPresentationService;
import com.tcc.pjb.backend.service.ui.presentation.live.UiPresentationLiveHub;

@RestController
@RequestMapping("/api/v1/ui/presentation")
@PreAuthorize("isAuthenticated()")
public class UiPresentationController {

  private final UiPresentationService service;
  private final UiPresentationLiveHub hub;
  private final CurrentUserService currentUser;

  public UiPresentationController(UiPresentationService service, UiPresentationLiveHub hub, CurrentUserService currentUser) {
    this.service = Objects.requireNonNull(service);
    this.hub = Objects.requireNonNull(hub);
    this.currentUser = Objects.requireNonNull(currentUser);
  }

  @GetMapping(value = "/bundle", produces = MediaType.APPLICATION_JSON_VALUE)
  public UiPresentationBundleDto bundle() {
    return service.bundleForCurrentUser();
  }

  @GetMapping(value = "/reading-preference", produces = MediaType.APPLICATION_JSON_VALUE)
  public UiReadingPreferenceDto readingPreference() {
    return service.readingPreference();
  }

  @PutMapping(value = "/reading-preference", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public UiReadingPreferenceDto updateReadingPreference(@RequestBody UiReadingPreferenceUpdateRequestDto req) {
    return service.updateReadingPreference(req);
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(
      @RequestParam(name = "usuarioId", required = false) Long usuarioId,
      @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId
  ) {
    long uid;
    if (usuarioId == null) {
      uid = currentUser.currentUserIdOrZero();
    } else {
      Usuario u = currentUser.getOrNull();
      TipoUsuario tipo = u == null ? null : u.getTipoUsuario();
      if (tipo == null || !tipo.isAdmin()) {
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN,
            "perfil nao autorizado para stream de terceiros"
        );
      }
      uid = usuarioId;
    }

    if (uid <= 0) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "usuarioId required"
      );
    }

    return hub.register("UIP:" + uid, lastEventId);
  }
}
