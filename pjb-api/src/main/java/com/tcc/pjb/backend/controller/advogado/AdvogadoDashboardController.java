package com.tcc.pjb.backend.controller.advogado;

import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoDashboardSummaryResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarProcessEventMirrorResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;
import com.tcc.pjb.backend.service.calendar.UserCalendarPanelService;
import com.tcc.pjb.backend.service.calendar.UserCalendarProcessMirrorService;

@RestController
@RequestMapping("/api/v1/advogado/dashboard")
@Validated
@PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
public class AdvogadoDashboardController {

    private final AdvogadoSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final UserCalendarPanelService calendarPanelService;
    private final UserCalendarProcessMirrorService processMirrorService;

    public AdvogadoDashboardController(AdvogadoSurfaceFacadeService facadeService,
                                       CapabilityRateLimiter rateLimiter,
                                       UserCalendarPanelService calendarPanelService,
                                       UserCalendarProcessMirrorService processMirrorService) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.calendarPanelService = calendarPanelService;
        this.processMirrorService = processMirrorService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdvogadoDashboardSummaryResponse> summary(Authentication authentication,
                                                                    @RequestParam(defaultValue = "7") int horizonDays,
                                                                    @RequestParam(defaultValue = "20") int maxItems) {
        enforce(authentication, "advogado_dashboard_summary");
        return ResponseEntity.ok(facadeService.dashboardSummary(horizonDays, maxItems));
    }


    @GetMapping("/calendar-panel")
    public ResponseEntity<CalendarPanelResponse> calendarPanel(Authentication authentication,
                                                               @RequestParam(defaultValue = "31") int horizonDays) {
        enforce(authentication, "advogado_dashboard_calendar_panel");
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(Math.max(7, Math.min(horizonDays, 90)));
        return ResponseEntity.ok(calendarPanelService.panel(from, to, null));
    }

    @GetMapping("/processos/{processoId}/event-mirror")
    public ResponseEntity<CalendarProcessEventMirrorResponse> eventMirror(Authentication authentication,
                                                                          @PathVariable Long processoId) {
        enforce(authentication, "advogado_dashboard_event_mirror");
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now().plusDays(90);
        return ResponseEntity.ok(processMirrorService.mirror(processoId, from, to));
    }

    private void enforce(Authentication authentication, String capability) {
        if (rateLimiter != null) {
            rateLimiter.enforce(CapabilityRateLimitDomain.LAWYER, authentication, capability, ApiVersion.V1);
        }
    }
}
