package com.tcc.pjb.backend.service.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.calendar.UserCalendarService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.profile.PerfilRealtimeTopicService;

@Component
public class PainelServiceCommons {

    private final WorkItemRepository workItemRepository;
    private final UserCalendarService userCalendarService;
    private final PerfilPainelSupportService supportService;
    private final OutboxPublisher outboxPublisher;
    private final PerfilRealtimeTopicService realtimeTopicService;

    public PainelServiceCommons(WorkItemRepository workItemRepository,
                                UserCalendarService userCalendarService,
                                PerfilPainelSupportService supportService,
                                OutboxPublisher outboxPublisher,
                                PerfilRealtimeTopicService realtimeTopicService) {
        this.workItemRepository = workItemRepository;
        this.userCalendarService = userCalendarService;
        this.supportService = supportService;
        this.outboxPublisher = outboxPublisher;
        this.realtimeTopicService = realtimeTopicService;
    }

    public List<WorkItem> inboxUsuario(Usuario usuario, int limit) {
        if (usuario == null || usuario.getId() == null) {
            return List.of();
        }
        return workItemRepository.inboxByUser(usuario.getId(), PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    public List<WorkItem> inboxRole(Usuario usuario, int limit) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return List.of();
        }
        return workItemRepository.inboxByRoleAndTerritory(usuario.getTipoUsuario(), usuario.getUf(), usuario.getComarca(), PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    public List<WorkItem> inboxHibrido(Usuario usuario, int limit) {
        List<WorkItem> own = inboxUsuario(usuario, limit);
        return own.isEmpty() ? inboxRole(usuario, limit) : own;
    }

    public List<CalendarEventDto> agenda(LocalDate from, LocalDate to) {
        try {
            return userCalendarService.list(from, to).days().stream().flatMap(day -> day.events().stream()).sorted(java.util.Comparator.comparing(CalendarEventDto::at)).toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public Map<String, Object> mapWorkItem(WorkItem item) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("titulo", item.getTitulo());
        map.put("descricao", item.getDescricao());
        map.put("processoId", item.getProcesso() != null ? item.getProcesso().getId() : null);
        map.put("processoNumero", item.getProcesso() != null ? item.getProcesso().getNumeroProcesso() : null);
        map.put("dueAt", item.getDueAt());
        map.put("prioridade", item.getPrioridade());
        map.put("status", item.getStatus());
        map.put("queueCode", item.getQueueCode());
        return map;
    }

    public String resumo(WorkItem item) {
        String processo = item.getProcesso() != null ? item.getProcesso().getNumeroProcesso() : "sem-processo";
        return item.getTitulo() + " · " + processo;
    }

    public String normalized(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT);
    }

    public boolean titleContains(WorkItem item, String... terms) {
        String n = normalized(item.getTitulo()) + ' ' + normalized(item.getDescricao());
        for (String term : terms) {
            if (n.contains(normalized(term))) {
                return true;
            }
        }
        return false;
    }

    public String etag(String prefix, Object... values) {
        return supportService.etagFor(prefix, values);
    }

    public void publishUserHistory(Usuario usuario, String channel, String type, String message, Processo processo, Object refId) {
        String topic = realtimeTopicService.inboxTopic(usuario, channel);
        publish(topic, type, message, processo, refId, channel);
    }

    public void publishTerritoryHistory(Usuario usuario, String channel, String type, String message, Processo processo, Object refId) {
        String topic = realtimeTopicService.territoryTopic(usuario, channel);
        publish(topic, type, message, processo, refId, channel);
    }

    private void publish(String topic, String type, String message, Processo processo, Object refId, String channel) {
        try {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("at", Instant.now().toString());
            payload.put("message", message);
            payload.put("topic", topic);
            payload.put("channel", channel);
            if (processo != null) {
                payload.put("processoId", processo.getId());
                payload.put("processoNumero", processo.getNumeroProcesso());
            }
            if (refId != null) {
                payload.put("referenceId", String.valueOf(refId));
            }
            outboxPublisher.enqueue(
                    topic,
                    OutboxPublisher.EVT_UI_HISTORY_LIVE,
                    payload,
                    Map.of("topic", topic, "channel", channel),
                    "role-live:" + type + ':' + String.valueOf(refId),
                    "ROLE_PANEL",
                    refId == null ? null : String.valueOf(refId)
            );
        } catch (Exception ignored) {
        }
    }
}
