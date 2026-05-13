package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.profile.PerfilRealtimeTopicService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarNotificationDispatchService {

    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final NotificationService notificationService;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final OutboxPublisher outboxPublisher;
    private final PerfilRealtimeTopicService realtimeTopicService;

    public CalendarNotificationDispatchService(UsuarioRepository usuarioRepository,
                                               ProcessoRepository processoRepository,
                                               NotificationService notificationService,
                                               NotificationHistoryRepository notificationHistoryRepository,
                                               OutboxPublisher outboxPublisher,
                                               PerfilRealtimeTopicService realtimeTopicService) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.notificationHistoryRepository = Objects.requireNonNull(notificationHistoryRepository);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.realtimeTopicService = Objects.requireNonNull(realtimeTopicService);
    }

    @Transactional
    public void dispatch(CalendarNotificationEnvelope envelope) {
        if (envelope == null || envelope.usuarioId() == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findById(envelope.usuarioId()).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return;
        }
        if (notificationHistoryRepository.existsByUsuarioIdAndProcessoIdAndTitulo(envelope.usuarioId(), envelope.processoId(), envelope.title())) {
            return;
        }
        Processo processo = envelope.processoId() == null ? null : processoRepository.findById(envelope.processoId()).orElse(null);
        boolean highPriority = "CRITICA".equalsIgnoreCase(envelope.urgency()) || "ALTA".equalsIgnoreCase(envelope.urgency());
        NotificationService.DispatchReport report = notificationService.notifyUserAdvanced(
                usuario,
                processo,
                envelope.title(),
                envelope.body(),
                envelope.detailsUrl(),
                highPriority
        );
        String topic = realtimeTopicService.inboxTopic(usuario, "CALENDAR");
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "CALENDAR_NOTIFICATION");
        payload.put("at", Instant.now().toString());
        payload.put("usuarioId", usuario.getId());
        payload.put("processoId", envelope.processoId());
        payload.put("processoNumero", envelope.processoNumero());
        payload.put("laneCode", envelope.laneCode());
        payload.put("segmentCode", envelope.segmentCode());
        payload.put("stageCode", envelope.stageCode());
        payload.put("urgency", envelope.urgency());
        payload.put("color", envelope.color());
        payload.put("title", envelope.title());
        payload.put("body", envelope.body());
        payload.put("detailsUrl", envelope.detailsUrl());
        payload.put("topic", topic);
        payload.put("notificationKey", envelope.notificationKey());
        payload.put("status", report.status());
        outboxPublisher.enqueue(
                topic,
                OutboxPublisher.EVT_UI_HISTORY_LIVE,
                payload,
                Map.of("topic", topic, "channel", "CALENDAR"),
                "calendar-live:" + envelope.notificationKey(),
                "CALENDAR_NOTIFICATION",
                envelope.notificationKey()
        );
        if (usuario.getTipoUsuario() != null && usuario.getTipoUsuario().name().equals("CIDADAO")) {
            outboxPublisher.enqueue(
                    "CIDADAO:" + usuario.getId(),
                    OutboxPublisher.EVT_CIDADAO_DASHBOARD_REFRESH,
                    Map.of("usuarioId", usuario.getId(), "reason", "CALENDAR_NOTIFICATION", "notificationKey", envelope.notificationKey()),
                    Map.of("topic", "CIDADAO:" + usuario.getId()),
                    "cidadao-calendar-refresh:" + envelope.notificationKey(),
                    "CIDADAO",
                    String.valueOf(usuario.getId())
            );
        }
    }
}
