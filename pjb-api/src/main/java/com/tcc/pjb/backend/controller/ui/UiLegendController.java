package com.tcc.pjb.backend.controller.ui;

import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiLegendDto;
import com.tcc.pjb.backend.model.dto.ui.UiPersona;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.ui.UiHintService;

@RestController
@RequestMapping("/api/v1/ui")
@PreAuthorize("permitAll()")
public class UiLegendController {

  private final UiHintService ui;
  private final CurrentUserService currentUser;

  public UiLegendController(UiHintService ui, CurrentUserService currentUser) {
    this.ui = Objects.requireNonNull(ui);
    this.currentUser = Objects.requireNonNull(currentUser);
  }

  
  @GetMapping("/legend")
  public ResponseEntity<UiLegendDto> legend(
      @RequestParam(name = "theme", required = false) String theme,
      @RequestParam(name = "persona", required = false) String persona,
      @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch
  ) {
    UiTheme t = UiTheme.fromString(theme);

    Usuario u = currentUser.getOrNull();
    UiPersona p = u != null ? UiPersona.fromTipoUsuario(u.getTipoUsuario()) : UiPersona.OUTRO;

    if (persona != null && !persona.isBlank() && u != null && u.getTipoUsuario() != null && u.getTipoUsuario().isAdmin()) {
      try {
        p = UiPersona.valueOf(persona.trim().toUpperCase(java.util.Locale.ROOT));
      } catch (Exception ignored) {
      }
    }

    UiLegendDto dto = ui.legend(t, p);
    String etag = "W/\"" + dto.version() + "-" + dto.theme() + "-" + dto.persona() + "\"";
    if (etag.equals(ifNoneMatch)) {
      return ResponseEntity.status(304)
          .eTag(etag)
          .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate().mustRevalidate())
          .build();
    }
    return ResponseEntity.ok()
        .eTag(etag)
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate().mustRevalidate())
        .body(dto);
  }
}
