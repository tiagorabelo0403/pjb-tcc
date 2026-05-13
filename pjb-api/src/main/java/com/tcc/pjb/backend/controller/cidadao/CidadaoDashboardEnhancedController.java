package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoEnhancedSnapshotResponse;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoOrientacaoAudienciaResponse;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoResumoProcessoResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarProcessEventMirrorResponse;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoTimelineVisualResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.calendar.UserCalendarPanelService;
import com.tcc.pjb.backend.service.calendar.UserCalendarProcessMirrorService;
import com.tcc.pjb.backend.service.cidadao.surface.CidadaoSurfaceFacadeService;

@RestController
@RequestMapping("/api/v1/cidadao/dashboard-enhanced")
@PreAuthorize("hasRole('CIDADAO')")
public class CidadaoDashboardEnhancedController {

    private final CidadaoSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final UserCalendarPanelService calendarPanelService;
    private final UserCalendarProcessMirrorService processMirrorService;

    public CidadaoDashboardEnhancedController(CidadaoSurfaceFacadeService facadeService,
                                              CapabilityRateLimiter rateLimiter,
                                              UserCalendarPanelService calendarPanelService,
                                              UserCalendarProcessMirrorService processMirrorService) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.calendarPanelService = calendarPanelService;
        this.processMirrorService = processMirrorService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<CidadaoEnhancedSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "cidadao_dashboard_enhanced_snapshot");
        return ResponseEntity.ok(facadeService.snapshot());
    }

    @GetMapping("/processos/{processoId}/timeline-visual")
    public ResponseEntity<CidadaoTimelineVisualResponse> timelineVisual(@PathVariable Long processoId,
                                                                        Authentication authentication) {
        enforce(authentication, "cidadao_dashboard_enhanced_timeline");
        return ResponseEntity.ok(facadeService.timelineVisual(processoId));
    }


    @GetMapping("/calendar-panel")
    public ResponseEntity<CalendarPanelResponse> calendarPanel(Authentication authentication) {
        enforce(authentication, "cidadao_dashboard_enhanced_calendar_panel");
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(31);
        return ResponseEntity.ok(calendarPanelService.panel(from, to, null));
    }

    @GetMapping("/processos/{processoId}/event-mirror")
    public ResponseEntity<CalendarProcessEventMirrorResponse> eventMirror(@PathVariable Long processoId,
                                                                          Authentication authentication) {
        enforce(authentication, "cidadao_dashboard_enhanced_event_mirror");
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now().plusDays(60);
        return ResponseEntity.ok(processMirrorService.mirror(processoId, from, to));
    }

    @GetMapping("/processos/{processoId}/orientacao-audiencia")
    public ResponseEntity<CidadaoOrientacaoAudienciaResponse> orientacaoAudiencia(@PathVariable Long processoId,
                                                                                   Authentication authentication) {
        enforce(authentication, "cidadao_dashboard_enhanced_orientacao_audiencia");
        return ResponseEntity.ok(facadeService.orientacaoAudiencia(processoId));
    }

    @GetMapping("/processos/{processoId}/resumo")
    public ResponseEntity<CidadaoResumoProcessoResponse> exportarResumo(@PathVariable Long processoId,
                                                                         Authentication authentication) {
        enforce(authentication, "cidadao_dashboard_enhanced_resumo");
        return ResponseEntity.ok(facadeService.resumoProcesso(processoId));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, capability, ApiVersion.V1);
    }
}
