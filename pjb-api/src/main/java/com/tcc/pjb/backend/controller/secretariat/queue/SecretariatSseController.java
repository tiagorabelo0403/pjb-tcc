package com.tcc.pjb.backend.controller.secretariat.queue;


import org.springframework.http.MediaType;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.live.SecretariatLiveHub;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_BASE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
public class SecretariatSseController {

  private final SecretariatLiveHub hub;
  private final SecretariatInstitutionalVisibilityService visibilityService;

  public SecretariatSseController(SecretariatLiveHub hub, SecretariatInstitutionalVisibilityService visibilityService) {
    this.hub = hub;
    this.visibilityService = visibilityService;
  }

  @GetMapping(value = OperationalApiRoutes.PATH_SECRETARIAT_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(
      @RequestParam("inboxKey") String inboxKey,
      @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    return hub.register(normalized, lastEventId);
  }
}
