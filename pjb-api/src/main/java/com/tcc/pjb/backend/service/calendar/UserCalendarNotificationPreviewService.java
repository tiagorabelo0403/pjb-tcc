package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationPreviewResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCalendarNotificationPreviewService {

    private final CurrentUserService currentUserService;
    private final UserCalendarWorkspaceService workspaceService;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final UserCalendarPreferenceService preferenceService;
    private final ProcessoRepository processoRepository;
    private final CalendarNotificationCadencePolicyService cadencePolicyService;
    private final CalendarEventAttentionPolicyService attentionPolicyService;

    public UserCalendarNotificationPreviewService(CurrentUserService currentUserService,
                                                  UserCalendarWorkspaceService workspaceService,
                                                  NotificationHistoryRepository notificationHistoryRepository,
                                                  UserCalendarPreferenceService preferenceService,
                                                  ProcessoRepository processoRepository,
                                                  CalendarNotificationCadencePolicyService cadencePolicyService,
                                                  CalendarEventAttentionPolicyService attentionPolicyService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workspaceService = Objects.requireNonNull(workspaceService);
        this.notificationHistoryRepository = Objects.requireNonNull(notificationHistoryRepository);
        this.preferenceService = Objects.requireNonNull(preferenceService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.cadencePolicyService = Objects.requireNonNull(cadencePolicyService);
        this.attentionPolicyService = Objects.requireNonNull(attentionPolicyService);
    }

    @Transactional(readOnly = true)
    public CalendarNotificationPreviewResponse preview(LocalDate from, LocalDate to, Long processoId) {
        return previewForUser(currentUserService.getRequired(), from, to, processoId);
    }

    @Transactional(readOnly = true)
    public CalendarNotificationPreviewResponse previewForUser(Usuario usuario, LocalDate from, LocalDate to, Long processoId) {
        CalendarWorkspaceResponse workspace = workspaceService.workspaceForUser(usuario, from, to, processoId);
        UserCalendarPreferenceResponse preference = preferenceService.currentOrDefault(usuario);
        Map<Long, Processo> processos = indexProcessos(workspace);
        LocalDateTime now = LocalDateTime.now();
        List<CalendarNotificationPreviewResponse.CalendarNotificationItemDto> items = workspace.lanes().stream()
                .filter(CalendarWorkspaceResponse.CalendarLaneDto::visible)
                .flatMap(lane -> lane.days().stream())
                .flatMap(day -> day.events().stream())
                .map(event -> toCandidate(usuario, preference, workspace.profile().profileCode(), event, processos.get(event.processoId()), now))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt((CalendarNotificationPreviewResponse.CalendarNotificationItemDto item) -> -item.attentionScore())
                        .thenComparing(CalendarNotificationPreviewResponse.CalendarNotificationItemDto::notifyAt)
                        .thenComparing(CalendarNotificationPreviewResponse.CalendarNotificationItemDto::eventAt)
                        .thenComparing(CalendarNotificationPreviewResponse.CalendarNotificationItemDto::title))
                .limit(24)
                .toList();
        int critical = (int) items.stream().filter(item -> "CRITICA".equals(item.urgency()) || "ALTA".equals(item.urgency())).count();
        long unreadInbox = usuario != null && usuario.getId() != null ? notificationHistoryRepository.countByUsuarioIdAndLidoEmIsNull(usuario.getId()) : 0L;
        return new CalendarNotificationPreviewResponse(
                Instant.now(),
                usuario != null ? usuario.getId() : null,
                workspace.profile().profileCode(),
                preference.selectedInstitutionContextCode(),
                items.size(),
                critical,
                unreadInbox,
                items
        );
    }

    @Transactional(readOnly = true)
    public List<CalendarNotificationEnvelope> dueNotificationsForUser(Usuario usuario, LocalDate from, LocalDate to, Long processoId, LocalDateTime now) {
        CalendarWorkspaceResponse workspace = workspaceService.workspaceForUser(usuario, from, to, processoId);
        UserCalendarPreferenceResponse preference = preferenceService.currentOrDefault(usuario);
        Map<Long, Processo> processos = indexProcessos(workspace);
        return workspace.lanes().stream()
                .filter(CalendarWorkspaceResponse.CalendarLaneDto::visible)
                .flatMap(lane -> lane.days().stream())
                .flatMap(day -> day.events().stream())
                .map(event -> toEnvelope(usuario, preference, workspace.profile().profileCode(), event, processos.get(event.processoId()), now))
                .filter(Objects::nonNull)
                .filter(item -> item.notifyAt() != null && !item.notifyAt().isAfter(now))
                .filter(item -> item.eventAt() != null && !item.eventAt().isBefore(now.minusDays(1)))
                .toList();
    }

    private CalendarNotificationPreviewResponse.CalendarNotificationItemDto toCandidate(Usuario usuario,
                                                                                         UserCalendarPreferenceResponse preference,
                                                                                         String profileCode,
                                                                                         CalendarWorkspaceEventDto event,
                                                                                         Processo processo,
                                                                                         LocalDateTime now) {
        NotificationPlan plan = plan(usuario, preference, profileCode, event, processo, now);
        if (plan == null) {
            return null;
        }
        return new CalendarNotificationPreviewResponse.CalendarNotificationItemDto(
                plan.notificationKey(),
                plan.stageCode(),
                plan.urgency(),
                event.laneCode(),
                event.segmentCode(),
                plan.presentationCode(),
                plan.iconCode(),
                plan.attentionScore(),
                event.processoId(),
                event.processoNumero(),
                plan.title(),
                plan.body(),
                event.at(),
                plan.notifyAt(),
                plan.color(),
                event.detailsUrl(),
                event.audienceCode(),
                plan.windowLabel(),
                plan.cadenceMode()
        );
    }

    private CalendarNotificationEnvelope toEnvelope(Usuario usuario,
                                                    UserCalendarPreferenceResponse preference,
                                                    String profileCode,
                                                    CalendarWorkspaceEventDto event,
                                                    Processo processo,
                                                    LocalDateTime now) {
        NotificationPlan plan = plan(usuario, preference, profileCode, event, processo, now);
        if (plan == null) {
            return null;
        }
        return new CalendarNotificationEnvelope(
                UUID.randomUUID(),
                usuario != null ? usuario.getId() : null,
                event.processoId(),
                event.processoNumero(),
                profileCode,
                event.laneCode(),
                event.segmentCode(),
                plan.stageCode(),
                plan.urgency(),
                plan.color(),
                plan.title(),
                plan.body(),
                event.detailsUrl(),
                event.audienceCode(),
                event.at(),
                plan.notifyAt(),
                Instant.now(),
                plan.notificationKey()
        );
    }

    private NotificationPlan plan(Usuario usuario,
                                  UserCalendarPreferenceResponse preference,
                                  String profileCode,
                                  CalendarWorkspaceEventDto event,
                                  Processo processo,
                                  LocalDateTime now) {
        if (event == null || event.at() == null) {
            return null;
        }
        CalendarNotificationCadencePolicyService.CadenceDecision cadence = cadencePolicyService.resolve(usuario, profileCode, preference, event, processo, now);
        if (cadence == null) {
            return null;
        }
        CalendarEventAttentionPolicyService.AttentionDescriptor descriptor = attentionPolicyService.describe(event, now);
        String processoNumero = event.processoNumero() == null || event.processoNumero().isBlank() ? "sem número" : event.processoNumero();
        String eventLabel = event.title() == null || event.title().isBlank() ? "evento" : event.title().trim();
        String title = descriptor.presentationTitle() + " • " + processoNumero + " • " + cadence.windowLabel() + " • " + eventLabel;
        String body = event.title() + (event.subtitle() == null || event.subtitle().isBlank() ? "" : " · " + event.subtitle());
        String key = String.join(":",
                "CAL",
                String.valueOf(event.processoId() == null ? 0L : event.processoId()),
                String.valueOf(event.eventId() == null ? 0L : event.eventId()),
                descriptor.presentationCode(),
                cadence.stageCode(),
                event.at().toLocalDate().toString(),
                cadence.cadenceMode());
        return new NotificationPlan(
                key,
                cadence.stageCode(),
                cadence.urgency(),
                title,
                body,
                cadence.notifyAt(),
                cadence.windowLabel(),
                cadence.cadenceMode(),
                descriptor.presentationCode(),
                descriptor.iconCode(),
                descriptor.attentionScore(),
                descriptor.color()
        );
    }

    private Map<Long, Processo> indexProcessos(CalendarWorkspaceResponse workspace) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (CalendarWorkspaceResponse.CalendarLaneDto lane : workspace.lanes()) {
            for (CalendarWorkspaceResponse.CalendarDayDto day : lane.days()) {
                for (CalendarWorkspaceEventDto event : day.events()) {
                    if (event.processoId() != null) {
                        ids.add(event.processoId());
                    }
                }
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Processo> index = new LinkedHashMap<>();
        for (Processo processo : processoRepository.findAllById(ids)) {
            index.put(processo.getId(), processo);
        }
        return index;
    }

    private record NotificationPlan(
            String notificationKey,
            String stageCode,
            String urgency,
            String title,
            String body,
            LocalDateTime notifyAt,
            String windowLabel,
            String cadenceMode,
            String presentationCode,
            String iconCode,
            int attentionScore,
            String color
    ) {
    }
}
