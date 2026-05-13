package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.dto.calendar.CalendarProcessEventMirrorResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCalendarProcessMirrorService {

    private final UserCalendarWorkspaceService workspaceService;
    private final TimelineSurfaceFacadeService timelineSurfaceFacadeService;
    private final ProcessoRepository processoRepository;
    private final CalendarEventAttentionPolicyService attentionPolicyService;

    public UserCalendarProcessMirrorService(UserCalendarWorkspaceService workspaceService,
                                            TimelineSurfaceFacadeService timelineSurfaceFacadeService,
                                            ProcessoRepository processoRepository,
                                            CalendarEventAttentionPolicyService attentionPolicyService) {
        this.workspaceService = Objects.requireNonNull(workspaceService);
        this.timelineSurfaceFacadeService = Objects.requireNonNull(timelineSurfaceFacadeService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.attentionPolicyService = Objects.requireNonNull(attentionPolicyService);
    }

    @Transactional(readOnly = true)
    public CalendarProcessEventMirrorResponse mirror(Long processoId, LocalDate from, LocalDate to) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        CalendarWorkspaceResponse workspace = workspaceService.workspace(from, to, processoId);
        List<CalendarWorkspaceEventDto> calendarEvents = workspace.lanes().stream()
                .flatMap(lane -> lane.days().stream())
                .flatMap(day -> day.events().stream())
                .sorted(Comparator.comparing(CalendarWorkspaceEventDto::at).thenComparing(CalendarWorkspaceEventDto::title))
                .toList();
        List<TimelineItemResponse> timeline = timelineSurfaceFacadeService.timeline(processoId);
        List<CalendarProcessEventMirrorResponse.LinkedTimelineItemDto> linkedTimeline = timeline.stream()
                .map(item -> linkTimelineItem(item, calendarEvents))
                .toList();
        Map<LocalDate, DayMirrorAcc> dayMap = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (CalendarWorkspaceEventDto event : calendarEvents) {
            LocalDate day = event.at() != null ? event.at().toLocalDate() : null;
            if (day == null) {
                continue;
            }
            CalendarEventAttentionPolicyService.AttentionDescriptor descriptor = attentionPolicyService.describe(event, now);
            DayMirrorAcc acc = dayMap.computeIfAbsent(day, ignored -> new DayMirrorAcc());
            acc.totalEvents++;
            acc.dominantColor = dominantColor(acc.dominantColor, descriptor.color());
            acc.titles.add(event.title());
        }
        return new CalendarProcessEventMirrorResponse(
                Instant.now(),
                processoId,
                numeroProcesso(processo),
                processo.getTribunal(),
                processo.getComarca(),
                processo.getRito() != null ? processo.getRito().name() : null,
                calendarEvents.stream()
                        .map(event -> {
                            CalendarEventAttentionPolicyService.AttentionDescriptor descriptor = attentionPolicyService.describe(event, now);
                            return new CalendarProcessEventMirrorResponse.LinkedCalendarEventDto(
                                    event.eventId(),
                                    event.laneCode(),
                                    event.segmentCode(),
                                    descriptor.presentationCode(),
                                    descriptor.detailCode(),
                                    descriptor.iconCode(),
                                    descriptor.attentionScore(),
                                    event.title(),
                                    event.subtitle(),
                                    event.at(),
                                    descriptor.color(),
                                    event.detailsUrl(),
                                    event.deadlineRuleSummary()
                            );
                        }).toList(),
                linkedTimeline,
                dayMap.entrySet().stream()
                        .map(entry -> new CalendarProcessEventMirrorResponse.DayMirrorDto(
                                entry.getKey(),
                                entry.getValue().dominantColor,
                                entry.getValue().totalEvents,
                                entry.getValue().titles.stream().limit(4).toList()
                        )).toList()
        );
    }

    private CalendarProcessEventMirrorResponse.LinkedTimelineItemDto linkTimelineItem(TimelineItemResponse item,
                                                                                       List<CalendarWorkspaceEventDto> events) {
        CalendarWorkspaceEventDto matched = events.stream()
                .filter(event -> isMatch(item, event))
                .min(Comparator.comparing(event -> event.at() == null ? LocalDateTime.MAX : event.at()))
                .orElse(null);
        CalendarEventAttentionPolicyService.AttentionDescriptor descriptor = matched == null ? null : attentionPolicyService.describe(matched, LocalDateTime.now(ZoneOffset.UTC));
        return new CalendarProcessEventMirrorResponse.LinkedTimelineItemDto(
                item.id(),
                item.data(),
                item.descricao(),
                item.faseDe(),
                item.fasePara(),
                descriptor != null ? descriptor.color() : null,
                matched != null ? matched.title() : null,
                descriptor != null ? descriptor.presentationCode() : null,
                descriptor != null ? descriptor.iconCode() : null,
                descriptor != null ? descriptor.attentionScore() : 0,
                matched != null ? matchReason(item, matched, descriptor) : null
        );
    }

    private boolean isMatch(TimelineItemResponse item, CalendarWorkspaceEventDto event) {
        if (item == null || event == null || item.data() == null || event.at() == null) {
            return false;
        }
        LocalDate timelineDay = LocalDateTime.ofInstant(item.data(), ZoneOffset.UTC).toLocalDate();
        if (!timelineDay.equals(event.at().toLocalDate())) {
            return false;
        }
        String descricao = normalize(item.descricao());
        String title = normalize(event.title());
        if (descricao == null || title == null) {
            return false;
        }
        if (descricao.contains("AUDI") && title.contains("AUDI")) {
            return true;
        }
        if (descricao.contains("JULG") && (title.contains("SESS") || title.contains("PAUTA") || title.contains("VOTO"))) {
            return true;
        }
        if (descricao.contains("PRAZO") && event.laneCode().equals("PRAZOS")) {
            return true;
        }
        return containsTokenIntersection(descricao, title);
    }

    private String matchReason(TimelineItemResponse item,
                               CalendarWorkspaceEventDto event,
                               CalendarEventAttentionPolicyService.AttentionDescriptor descriptor) {
        String descricao = normalize(item.descricao());
        if (descricao != null && descricao.contains("AUDI") && event.title() != null) {
            return "evento processual sincronizado por audiência";
        }
        if (descricao != null && descricao.contains("JULG")) {
            return descriptor == null ? "evento processual sincronizado por julgamento" : "evento processual sincronizado por " + descriptor.presentationTitle().toLowerCase(Locale.ROOT);
        }
        if (event.laneCode().equals("PRAZOS")) {
            return descriptor == null ? "evento processual sincronizado por prazo" : "evento processual sincronizado por " + descriptor.presentationTitle().toLowerCase(Locale.ROOT);
        }
        return "evento processual sincronizado por data e semântica";
    }

    private static boolean containsTokenIntersection(String left, String right) {
        for (String token : left.split("\\s+")) {
            if (token.length() >= 5 && right.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Â', 'A')
                .replace('Ã', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private static String dominantColor(String current, String candidate) {
        if (current == null) {
            return candidate;
        }
        return UserCalendarPanelService.severity(candidate) > UserCalendarPanelService.severity(current) ? candidate : current;
    }

    private static String numeroProcesso(Processo processo) {
        if (processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()) {
            return processo.getNumeroProcesso();
        }
        return processo.getNumeroUnificado();
    }

    private static final class DayMirrorAcc {
        private String dominantColor;
        private int totalEvents;
        private final List<String> titles = new ArrayList<>();
    }
}
