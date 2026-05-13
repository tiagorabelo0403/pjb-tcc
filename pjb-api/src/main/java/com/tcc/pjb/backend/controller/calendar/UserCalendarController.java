package com.tcc.pjb.backend.controller.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.pjb.backend.model.dto.calendar.CalendarCustomEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarCustomEventRequest;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventsResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarMarkerRequest;
import com.tcc.pjb.backend.model.dto.calendar.CalendarMonthResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationPreviewResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarProcessEventMirrorResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceRequest;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.calendar.UserCalendarNotificationPreviewService;
import com.tcc.pjb.backend.service.calendar.UserCalendarPanelService;
import com.tcc.pjb.backend.service.calendar.UserCalendarPreferenceService;
import com.tcc.pjb.backend.service.calendar.UserCalendarProcessMirrorService;
import com.tcc.pjb.backend.service.calendar.UserCalendarService;
import com.tcc.pjb.backend.service.calendar.UserCalendarWorkspaceService;

@RestController
@RequestMapping("/api/v1/calendar")
public class UserCalendarController {

  private static final String CALENDAR_ROLES = "hasAnyRole('CIDADAO','ADVOGADO','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','PROCURADORIA_MUNICIPAL','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR','DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL','PERITO','PERITO_CRIMINAL','PERITO_AMBIENTAL','PERITO_CONTABIL','PERITO_ENGENHARIA','PERITO_DIGITAL','PERITO_INSS','PERITO_MEDICO','PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL','ASSISTENTE_TECNICO','CONCILIADOR_CEJUSC','MEDIADOR','ARBITRO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')";

  private final UserCalendarService service;
  private final CapabilityRateLimiter rateLimiter;
  private final UserCalendarWorkspaceService workspaceService;
  private final UserCalendarPanelService panelService;
  private final UserCalendarProcessMirrorService processMirrorService;
  private final UserCalendarPreferenceService preferenceService;
  private final UserCalendarNotificationPreviewService notificationPreviewService;
  private final CalendarInstitutionalBridgeService institutionalBridgeService;

  public UserCalendarController(UserCalendarService service,
                                CapabilityRateLimiter rateLimiter,
                                UserCalendarWorkspaceService workspaceService,
                                UserCalendarPanelService panelService,
                                UserCalendarProcessMirrorService processMirrorService,
                                UserCalendarPreferenceService preferenceService,
                                UserCalendarNotificationPreviewService notificationPreviewService,
                                CalendarInstitutionalBridgeService institutionalBridgeService) {
    this.service = service;
    this.rateLimiter = rateLimiter;
    this.workspaceService = workspaceService;
    this.panelService = panelService;
    this.processMirrorService = processMirrorService;
    this.preferenceService = preferenceService;
    this.notificationPreviewService = notificationPreviewService;
    this.institutionalBridgeService = institutionalBridgeService;
  }

  @GetMapping("/events")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarEventsResponse> events(
      Authentication authentication,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "processoId", required = false) Long processoId) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_events", ApiVersion.V1);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(20)).cachePrivate())
        .body(processoId == null ? service.list(from, to) : service.listForProcesso(from, to, processoId));
  }

  @GetMapping("/workspace")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarWorkspaceResponse> workspace(
      Authentication authentication,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "processoId", required = false) Long processoId,
      @RequestParam(value = "lane", required = false) String lane) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_workspace", ApiVersion.V1);
    CalendarWorkspaceResponse response = workspaceService.workspace(from, to, processoId);
    if (lane != null && !lane.isBlank()) {
      String normalizedLane = lane.trim().toUpperCase(Locale.ROOT);
      response = new CalendarWorkspaceResponse(
          response.from(),
          response.to(),
          response.profile(),
          response.colorLegend(),
          response.lanes().stream().filter(item -> item.laneCode().equalsIgnoreCase(normalizedLane)).toList()
      );
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(20)).cachePrivate())
        .body(response);
  }


  @GetMapping("/panel")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarPanelResponse> panel(
      Authentication authentication,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "processoId", required = false) Long processoId) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_panel", ApiVersion.V1);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(20)).cachePrivate())
        .body(panelService.panel(from, to, processoId));
  }

  @GetMapping("/processos/{processoId}/event-mirror")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarProcessEventMirrorResponse> eventMirror(
      Authentication authentication,
      @PathVariable("processoId") Long processoId,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_process_event_mirror", ApiVersion.V1);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(20)).cachePrivate())
        .body(processMirrorService.mirror(processoId, from, to));
  }


  @GetMapping("/institutional-bridge")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarInstitutionalBridgeResponse> institutionalBridge(
      Authentication authentication,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "processoId", required = false) Long processoId) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_institutional_bridge", ApiVersion.V1);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(20)).cachePrivate())
        .body(institutionalBridgeService.bridge(from, to, processoId));
  }


  @GetMapping("/institutional-focus")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarInstitutionalFocusResponse> institutionalFocus(
      Authentication authentication,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "processoId", required = false) Long processoId) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_institutional_focus", ApiVersion.V1);
    CalendarInstitutionalBridgeResponse bridge = institutionalBridgeService.bridge(from, to, processoId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(20)).cachePrivate())
        .body(institutionalBridgeService.focus(bridge));
  }

  @GetMapping("/notification-preview")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarNotificationPreviewResponse> notificationPreview(
      Authentication authentication,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "processoId", required = false) Long processoId) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_notification_preview", ApiVersion.V1);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(15)).cachePrivate())
        .body(notificationPreviewService.preview(from, to, processoId));
  }

  @GetMapping("/preferences")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<UserCalendarPreferenceResponse> preferences(Authentication authentication) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_preferences_get", ApiVersion.V1);
    return ResponseEntity.ok(preferenceService.current());
  }

  @PutMapping("/preferences")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<UserCalendarPreferenceResponse> preferences(
      Authentication authentication,
      @RequestBody UserCalendarPreferenceRequest request) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_preferences_put", ApiVersion.V1);
    return ResponseEntity.ok(preferenceService.save(request));
  }

  @GetMapping("/month")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarMonthResponse> month(
      Authentication authentication,
      @RequestParam("year") int year,
      @RequestParam("month") int month) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_month", ApiVersion.V1);
    YearMonth ym = YearMonth.of(year, month);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePrivate())
        .body(service.month(ym));
  }

  @PostMapping("/markers")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<Void> mark(Authentication authentication, @RequestBody CalendarMarkerRequest req) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_mark", ApiVersion.V1);
    service.mark(req);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/markers/{eventType}/{eventId}")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<Void> unmark(
      Authentication authentication,
      @PathVariable("eventType") String eventType,
      @PathVariable("eventId") Long eventId) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_unmark", ApiVersion.V1);
    service.unmark(eventType, eventId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/custom-events")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarCustomEventDto> createCustom(
      Authentication authentication,
      @RequestBody CalendarCustomEventRequest req) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_custom_create", ApiVersion.V1);
    return ResponseEntity.ok(service.createCustomEvent(req));
  }

  @PutMapping("/custom-events/{id}")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<CalendarCustomEventDto> updateCustom(
      Authentication authentication,
      @PathVariable("id") Long id,
      @RequestBody CalendarCustomEventRequest req) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_custom_update", ApiVersion.V1);
    return ResponseEntity.ok(service.updateCustomEvent(id, req));
  }

  @DeleteMapping("/custom-events/{id}")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<Void> deleteCustom(
      Authentication authentication,
      @PathVariable("id") Long id) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_custom_delete", ApiVersion.V1);
    service.deleteCustomEvent(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/ics", produces = "text/calendar")
  @PreAuthorize(CALENDAR_ROLES)
  public ResponseEntity<String> ics(
      Authentication authentication,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "processoId", required = false) Long processoId) {
    rateLimiter.enforce(resolveDomain(authentication), authentication, "calendar_ics", ApiVersion.V1);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/calendar"))
        .body(service.ics(from, to, processoId));
  }

  private CapabilityRateLimitDomain resolveDomain(Authentication authentication) {
    if (authentication == null || authentication.getAuthorities() == null) {
      return CapabilityRateLimitDomain.CITIZEN;
    }
    boolean lawyer = authentication.getAuthorities().stream().anyMatch(item -> "ROLE_ADVOGADO".equalsIgnoreCase(item.getAuthority()) || "ROLE_ADVOCACIA".equalsIgnoreCase(item.getAuthority()));
    if (lawyer) {
      return CapabilityRateLimitDomain.LAWYER;
    }
    boolean citizen = authentication.getAuthorities().stream().anyMatch(item -> "ROLE_CIDADAO".equalsIgnoreCase(item.getAuthority()) || "ROLE_USER".equalsIgnoreCase(item.getAuthority()));
    if (citizen) {
      return CapabilityRateLimitDomain.CITIZEN;
    }
    return CapabilityRateLimitDomain.INSTITUCIONAL;
  }
}
