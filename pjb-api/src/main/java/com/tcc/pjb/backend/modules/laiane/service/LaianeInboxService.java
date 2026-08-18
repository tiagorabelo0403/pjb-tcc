package com.tcc.pjb.backend.modules.laiane.service;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.entity.NotificationHistory;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.modules.laiane.dto.inbox.LaianeInboxResponse;
import com.tcc.pjb.backend.modules.laiane.dto.inbox.LaianeNotificationDto;
import com.tcc.pjb.backend.service.workitem.WorkItemService;

@Service
public class LaianeInboxService {

    private final CurrentUserService currentUserService;
    private final WorkItemService workItemService;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final PjbTimeService timeService;

    public LaianeInboxService(CurrentUserService currentUserService,
                             WorkItemService workItemService,
                             NotificationHistoryRepository notificationHistoryRepository,
                             PjbTimeService timeService) {
        this.currentUserService = currentUserService;
        this.workItemService = workItemService;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.timeService = timeService;
    }

    public LaianeInboxResponse inbox() {
        Usuario u = currentUserService.get();

        
        List<WorkItemDto> work = workItemService.inbox(0, 30).getContent();

        
        List<NotificationHistory> last = notificationHistoryRepository.findTop50ByUsuarioIdOrderByEnviadoEmDesc(u.getId());
        List<LaianeNotificationDto> mapped = last.stream().map(this::map).toList();

        return LaianeInboxResponse.builder()
                .workItems(work)
                .notifications(mapped)
                .build();
    }

    private LaianeNotificationDto map(NotificationHistory h) {
        return LaianeNotificationDto.builder()
                .id(h.getId())
                .processoId(h.getProcessoId())
                .canal(h.getCanal())
                .titulo(h.getTitulo())
                .mensagem(h.getMensagem())
                .enviadoEm(toOffset(h.getEnviadoEm()))
                .status(h.getStatus())
                .build();
    }

    private OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(timeService.legalZone()).toOffsetDateTime();
    }
}
