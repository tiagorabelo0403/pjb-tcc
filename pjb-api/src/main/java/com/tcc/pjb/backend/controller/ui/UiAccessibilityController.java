package com.tcc.pjb.backend.controller.ui;

import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityLoginContextDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreferenceDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreferenceUpdateRequestDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiUsageMetricsDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiReadabilityProfilePreviewRequestDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiReadabilityProfilePreviewResponseDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiPlainLanguagePreviewRequestDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiPlainLanguagePreviewResponseDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.ui.accessibility.UiAccessibilityService;
import com.tcc.pjb.backend.service.ui.accessibility.UiPlainLanguageService;
import com.tcc.pjb.backend.service.ui.accessibility.UiReadabilityProfileService;
import com.tcc.pjb.backend.service.ui.accessibility.live.UiAccessibilityLiveHub;

@RestController
@RequestMapping("/api/v1/ui/accessibility")
@PreAuthorize("isAuthenticated()")
public class UiAccessibilityController {

  private final UiAccessibilityService service;
  private final UiAccessibilityLiveHub hub;
  private final CurrentUserService currentUser;
  private final UiAccessibilityRateLimiter rateLimiter;
  private final UiPlainLanguageService plainLanguageService;
  private final UiReadabilityProfileService readabilityProfileService;

  public UiAccessibilityController(
      UiAccessibilityService service,
      UiAccessibilityLiveHub hub,
      CurrentUserService currentUser,
      UiAccessibilityRateLimiter rateLimiter,
      UiPlainLanguageService plainLanguageService,
      UiReadabilityProfileService readabilityProfileService
  ) {
    this.service = Objects.requireNonNull(service);
    this.hub = Objects.requireNonNull(hub);
    this.currentUser = Objects.requireNonNull(currentUser);
    this.rateLimiter = Objects.requireNonNull(rateLimiter);
    this.plainLanguageService = Objects.requireNonNull(plainLanguageService);
    this.readabilityProfileService = Objects.requireNonNull(readabilityProfileService);
  }

  @PostMapping(value = "/login-context", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public UiAccessibilityLoginContextDto loginContext(@Valid @RequestBody UiUsageMetricsDto metrics, HttpServletRequest request) {
    rateLimiter.assertAllowed(currentUser.currentUserIdOrZero(), clientIp(request));
    return service.evaluateOnLogin(metrics);
  }

  @GetMapping(value = "/preference", produces = MediaType.APPLICATION_JSON_VALUE)
  public UiAccessibilityPreferenceDto preference() {
    return service.getPreference();
  }

  @PutMapping(value = "/preference", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public UiAccessibilityPreferenceDto update(@Valid @RequestBody UiAccessibilityPreferenceUpdateRequestDto req) {
    return service.updatePreference(req);
  }


  @PostMapping(value = "/plain-language/preview", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public UiPlainLanguagePreviewResponseDto plainLanguagePreview(@Valid @RequestBody UiPlainLanguagePreviewRequestDto request) {
    return plainLanguageService.preview(request.text());
  }

  @PostMapping(value = "/readability-profile/preview", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public UiReadabilityProfilePreviewResponseDto readabilityProfilePreview(@Valid @RequestBody UiReadabilityProfilePreviewRequestDto request) {
    return readabilityProfileService.preview(request);
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
        throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "perfil nao autorizado para stream de terceiros");
      }
      uid = usuarioId;
    }

    if (uid <= 0) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "usuarioId required");
    }

    return hub.register("A11Y:" + uid, lastEventId);
  }

  private static String clientIp(HttpServletRequest req) {
    if (req == null) return "";
    String fwd = req.getHeader("X-Forwarded-For");
    if (fwd != null && !fwd.isBlank()) {
      int i = fwd.indexOf(',');
      return (i > 0 ? fwd.substring(0, i) : fwd).trim();
    }
    String real = req.getHeader("X-Real-IP");
    if (real != null && !real.isBlank()) return real.trim();
    String xri = req.getHeader("X-Request-IP");
    if (xri != null && !xri.isBlank()) return xri.trim();
    return req.getRemoteAddr();
  }
}
