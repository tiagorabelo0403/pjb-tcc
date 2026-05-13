package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCalendarPanelService {

    private final CurrentUserService currentUserService;
    private final UserCalendarWorkspaceService workspaceService;
    private final UserCalendarPreferenceService preferenceService;
    private final ProcessoRepository processoRepository;
    private final UserCalendarNotificationPreviewService notificationPreviewService;
    private final CalendarInstitutionalBridgeService institutionalBridgeService;
    private final CalendarInstitutionalScopeService scopeService;
    private final CalendarInstitutionalContextService contextService;
    private final CalendarEventAttentionPolicyService attentionPolicyService;

    public UserCalendarPanelService(CurrentUserService currentUserService,
                                    UserCalendarWorkspaceService workspaceService,
                                    UserCalendarPreferenceService preferenceService,
                                    ProcessoRepository processoRepository,
                                    UserCalendarNotificationPreviewService notificationPreviewService,
                                    CalendarInstitutionalBridgeService institutionalBridgeService,
                                    CalendarInstitutionalScopeService scopeService,
                                    CalendarInstitutionalContextService contextService,
                                    CalendarEventAttentionPolicyService attentionPolicyService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workspaceService = Objects.requireNonNull(workspaceService);
        this.preferenceService = Objects.requireNonNull(preferenceService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.notificationPreviewService = Objects.requireNonNull(notificationPreviewService);
        this.institutionalBridgeService = Objects.requireNonNull(institutionalBridgeService);
        this.scopeService = Objects.requireNonNull(scopeService);
        this.contextService = Objects.requireNonNull(contextService);
        this.attentionPolicyService = Objects.requireNonNull(attentionPolicyService);
    }

    @Transactional(readOnly = true)
    public CalendarPanelResponse panel(LocalDate from, LocalDate to, Long processoId) {
        return panelForUser(currentUserService.getRequired(), from, to, processoId);
    }

    @Transactional(readOnly = true)
    public CalendarPanelResponse panelForUser(Usuario usuario, LocalDate from, LocalDate to, Long processoId) {
        UserCalendarPreferenceResponse preference = preferenceService.currentOrDefault(usuario);
        CalendarWorkspaceResponse workspace = workspaceService.workspaceForUser(usuario, from, to, processoId);
        List<CalendarWorkspaceResponse.CalendarLaneDto> activeLanes = workspace.lanes().stream()
                .filter(CalendarWorkspaceResponse.CalendarLaneDto::visible)
                .toList();
        Map<LocalDate, DayAccumulator> dayMap = new LinkedHashMap<>();
        for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(1)) {
            dayMap.put(cursor, new DayAccumulator());
        }
        Map<Long, CalendarPanelResponse.ProcessSpotlightDto> processSpotlights = new LinkedHashMap<>();
        List<CalendarPanelResponse.HighlightedEventDto> highlighted = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (CalendarWorkspaceResponse.CalendarLaneDto lane : activeLanes) {
            for (CalendarWorkspaceResponse.CalendarDayDto day : lane.days()) {
                DayAccumulator accumulator = dayMap.computeIfAbsent(day.day(), ignored -> new DayAccumulator());
                for (CalendarWorkspaceEventDto event : day.events()) {
                    CalendarEventAttentionPolicyService.AttentionDescriptor descriptor = attentionPolicyService.describe(event, now);
                    accumulator.totalEvents++;
                    accumulator.criticalEvents += severity(descriptor.color()) >= 4 ? 1 : 0;
                    accumulator.attentionScore = Math.max(accumulator.attentionScore, descriptor.attentionScore());
                    accumulator.laneCodes.add(event.laneCode());
                    accumulator.detailCodes.add(descriptor.detailCode());
                    accumulator.labels.add(compactLabel(event.title()));
                    accumulator.dominantColor = dominantColor(accumulator.dominantColor, descriptor.color());
                    highlighted.add(new CalendarPanelResponse.HighlightedEventDto(
                            event.processoId(),
                            event.processoNumero(),
                            event.laneCode(),
                            event.segmentCode(),
                            descriptor.presentationCode(),
                            descriptor.presentationTitle(),
                            descriptor.iconCode(),
                            descriptor.attentionScore(),
                            event.title(),
                            event.subtitle(),
                            event.at(),
                            descriptor.color(),
                            event.detailsUrl(),
                            event.audienceCode()
                    ));
                    if (event.processoId() != null) {
                        CalendarPanelResponse.ProcessSpotlightDto current = processSpotlights.get(event.processoId());
                        CalendarPanelResponse.ProcessSpotlightDto candidate = new CalendarPanelResponse.ProcessSpotlightDto(
                                event.processoId(),
                                event.processoNumero(),
                                descriptor.presentationCode(),
                                descriptor.presentationTitle(),
                                event.title(),
                                descriptor.color(),
                                descriptor.attentionScore(),
                                event.at(),
                                event.detailsUrl()
                        );
                        if (current == null || compareSpotlight(candidate, current) < 0) {
                            processSpotlights.put(event.processoId(), candidate);
                        }
                    }
                }
            }
        }
        List<CalendarPanelResponse.PanelDayCellDto> dayCells = dayMap.entrySet().stream()
                .map(entry -> new CalendarPanelResponse.PanelDayCellDto(
                        entry.getKey(),
                        entry.getValue().dominantColor,
                        entry.getValue().totalEvents,
                        entry.getValue().criticalEvents,
                        entry.getValue().attentionScore,
                        List.copyOf(entry.getValue().laneCodes),
                        List.copyOf(entry.getValue().detailCodes),
                        entry.getValue().labels.stream().limit(4).toList()
                ))
                .toList();
        List<CalendarPanelResponse.HighlightedEventDto> highlightedEvents = highlighted.stream()
                .sorted(Comparator
                        .comparingInt((CalendarPanelResponse.HighlightedEventDto item) -> -item.attentionScore())
                        .thenComparing(item -> item.at() == null ? LocalDateTime.MAX : item.at())
                        .thenComparing(CalendarPanelResponse.HighlightedEventDto::title))
                .limit(24)
                .toList();
        List<CalendarInstitutionalScopeService.ScopeOption> availableScopes = scopeService.availableScopes(usuario, preference.includePersonalCalendar(), preference.includeInstitutionalCalendar(), processoId);
        String activeScopeCode = scopeService.normalizeActiveScope(preference.selectedScopeCode(), availableScopes, preference.includePersonalCalendar(), preference.includeInstitutionalCalendar());
        Long selectedTeamId = preference.selectedTeamId() != null ? preference.selectedTeamId() : scopeService.parseTeamId(activeScopeCode);
        List<CalendarInstitutionalContextService.InstitutionalContextOption> availableContexts = contextService.availableContexts(usuario, activeScopeCode, processoId, selectedTeamId);
        String activeInstitutionContextCode = contextService.normalizeActiveContext(preference.selectedInstitutionContextCode(), availableContexts, activeScopeCode);
        CalendarInstitutionalBridgeResponse institutionalBridge = institutionalBridgeService.bridgeForUser(usuario, from, to, processoId);
        CalendarInstitutionalFocusResponse institutionalFocus = institutionalBridgeService.focus(institutionalBridge);
        return new CalendarPanelResponse(
                Instant.now(),
                from,
                to,
                new CalendarPanelResponse.PanelProfileDto(
                        workspace.profile().profileCode(),
                        workspace.profile().profileTitle(),
                        preference.defaultView(),
                        workspace.profile().highlightLaneCode(),
                        preference.includePersonalCalendar(),
                        preference.includeInstitutionalCalendar(),
                        preference.highlightUrgentDays(),
                        activeScopeCode,
                        activeInstitutionContextCode,
                        preference.notificationCadenceMode()
                ),
                resolveScopes(availableScopes, activeScopeCode, processoId),
                resolveInstitutionalContexts(availableContexts, activeInstitutionContextCode),
                dayCells,
                highlightedEvents,
                processSpotlights.values().stream()
                        .sorted(Comparator
                                .comparingInt((CalendarPanelResponse.ProcessSpotlightDto item) -> -item.attentionScore())
                                .thenComparing(item -> item.at() == null ? LocalDateTime.MAX : item.at()))
                        .limit(12)
                        .toList(),
                workspace.colorLegend(),
                notificationPreviewService.previewForUser(usuario, from, to, processoId),
                institutionalBridge,
                institutionalFocus
        );
    }

    private List<CalendarPanelResponse.PanelScopeDto> resolveScopes(List<CalendarInstitutionalScopeService.ScopeOption> availableScopes,
                                                                    String activeScopeCode,
                                                                    Long processoId) {
        List<CalendarPanelResponse.PanelScopeDto> scopes = new ArrayList<>();
        for (CalendarInstitutionalScopeService.ScopeOption option : availableScopes) {
            String label = option.scopeCode().equals("PROCESSUAL") && processoId != null ? processLabel(processoId) : option.institutionLabel();
            String title = option.scopeCode().equals("PROCESSUAL") && processoId != null ? "Calendário do processo" : option.scopeTitle();
            scopes.add(new CalendarPanelResponse.PanelScopeDto(
                    option.scopeCode(),
                    title,
                    label,
                    option.scopeCode().equalsIgnoreCase(activeScopeCode)
            ));
        }
        return List.copyOf(scopes);
    }

    private List<CalendarPanelResponse.PanelInstitutionalContextDto> resolveInstitutionalContexts(List<CalendarInstitutionalContextService.InstitutionalContextOption> contexts,
                                                                                                  String activeInstitutionContextCode) {
        return contexts.stream()
                .map(item -> new CalendarPanelResponse.PanelInstitutionalContextDto(
                        item.contextCode(),
                        item.contextTitle(),
                        item.contextLabel(),
                        item.contextKind(),
                        item.contextCode().equalsIgnoreCase(activeInstitutionContextCode)
                ))
                .toList();
    }

    private String processLabel(Long processoId) {
        Processo processo = processoRepository.findById(processoId).orElse(null);
        if (processo == null) {
            return "Processo " + processoId;
        }
        String numero = processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()
                ? processo.getNumeroProcesso()
                : processo.getNumeroUnificado();
        return numero == null ? "Processo " + processoId : numero;
    }

    private static int compareSpotlight(CalendarPanelResponse.ProcessSpotlightDto a,
                                        CalendarPanelResponse.ProcessSpotlightDto b) {
        int attention = Integer.compare(b.attentionScore(), a.attentionScore());
        if (attention != 0) {
            return attention;
        }
        LocalDateTime aTime = a.at() == null ? LocalDateTime.MAX : a.at();
        LocalDateTime bTime = b.at() == null ? LocalDateTime.MAX : b.at();
        int cmpTime = aTime.compareTo(bTime);
        if (cmpTime != 0) {
            return cmpTime;
        }
        return Integer.compare(severity(b.color()), severity(a.color()));
    }

    private static String compactLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Evento";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 48 ? trimmed : trimmed.substring(0, 45) + "…";
    }

    private static String dominantColor(String current, String candidate) {
        if (current == null) {
            return candidate;
        }
        return severity(candidate) > severity(current) ? candidate : current;
    }

    static int severity(String color) {
        if (color == null) {
            return 0;
        }
        return switch (color.trim().toUpperCase(Locale.ROOT)) {
            case "RED" -> 5;
            case "AMBER", "ORANGE" -> 4;
            case "PURPLE" -> 3;
            case "BLUE" -> 2;
            case "GREEN" -> 1;
            default -> 0;
        };
    }

    private static final class DayAccumulator {
        private String dominantColor;
        private int totalEvents;
        private int criticalEvents;
        private int attentionScore;
        private final Set<String> laneCodes = new LinkedHashSet<>();
        private final Set<String> detailCodes = new LinkedHashSet<>();
        private final Set<String> labels = new LinkedHashSet<>();
    }
}
