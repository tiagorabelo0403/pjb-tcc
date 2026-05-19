package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoCommand;
import com.tcc.pjb.backend.modules.notificacoes.application.NotificacaoPrazoApplicationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CalendarNotificationScheduler {

    private final UsuarioRepository usuarioRepository;
    private final UserCalendarNotificationPreviewService previewService;
    private final CalendarNotificationEventPublisher eventPublisher;
    private final NotificacaoPrazoApplicationService notificacaoPrazoApplicationService;

    public CalendarNotificationScheduler(UsuarioRepository usuarioRepository,
                                         UserCalendarNotificationPreviewService previewService,
                                         CalendarNotificationEventPublisher eventPublisher,
                                         NotificacaoPrazoApplicationService notificacaoPrazoApplicationService) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.previewService = Objects.requireNonNull(previewService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.notificacaoPrazoApplicationService = Objects.requireNonNull(notificacaoPrazoApplicationService);
    }

    @Scheduled(fixedDelayString = "${pjb.calendar.notifications.scan-delay-ms:1800000}")
    public void scanAndDispatch() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        int page = 0;
        Page<Usuario> slice;
        do {
            slice = usuarioRepository.findByAtivoTrue(PageRequest.of(page, 100));
            for (Usuario usuario : slice.getContent()) {
                if (usuario == null || usuario.getId() == null || !usuario.isAtivo()) {
                    continue;
                }
                previewService.dueNotificationsForUser(usuario, today, today.plusDays(15), null, now)
                        .forEach(this::dispatch);
            }
            page++;
        } while (slice.hasNext());
    }

    private void dispatch(CalendarNotificationEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        if (dispatchPrazoModular(envelope)) {
            return;
        }
        eventPublisher.publish(envelope);
    }

    private boolean dispatchPrazoModular(CalendarNotificationEnvelope envelope) {
        if (!isPrazo(envelope)) {
            return false;
        }
        if (envelope.eventAt() == null || envelope.usuarioId() == null || envelope.processoId() == null) {
            return false;
        }
        try {
            var result = notificacaoPrazoApplicationService.publicarAlertaPrazo(new NotificacaoPrazoCommand(
                    envelope.usuarioId(),
                    envelope.processoId(),
                    envelope.processoNumero(),
                    envelope.eventAt().toLocalDate(),
                    normalizarNotificarEm(envelope.notifyAt()),
                    envelope.title(),
                    envelope.body(),
                    envelope.detailsUrl(),
                    envelope.urgency(),
                    "CALENDAR",
                    envelope.notificationKey()
            ));
            return result != null && result.aceita();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isPrazo(CalendarNotificationEnvelope envelope) {
        String lane = token(envelope.laneCode());
        String segment = token(envelope.segmentCode());
        return lane.startsWith("PRAZO") || segment.startsWith("PRAZO");
    }

    private String token(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDateTime normalizarNotificarEm(LocalDateTime notifyAt) {
        if (notifyAt == null || notifyAt.toLocalDate().isBefore(LocalDate.now())) {
            return null;
        }
        return notifyAt;
    }
}
