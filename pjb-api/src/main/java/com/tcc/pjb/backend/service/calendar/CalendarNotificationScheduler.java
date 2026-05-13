package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public CalendarNotificationScheduler(UsuarioRepository usuarioRepository,
                                         UserCalendarNotificationPreviewService previewService,
                                         CalendarNotificationEventPublisher eventPublisher) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.previewService = Objects.requireNonNull(previewService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
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
                        .forEach(eventPublisher::publish);
            }
            page++;
        } while (slice.hasNext());
    }
}
