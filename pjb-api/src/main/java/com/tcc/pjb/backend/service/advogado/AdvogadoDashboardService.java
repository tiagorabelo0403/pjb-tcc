package com.tcc.pjb.backend.service.advogado;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.advogado.AdvogadoDashboardDto;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusEvento;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.service.PrazoService;

@Service
public class AdvogadoDashboardService {

    private final CurrentUserService currentUser;
    private final ClienteRepository clienteRepository;
    private final WorkItemRepository workItemRepository;
    private final PrazoService prazoService;

    public AdvogadoDashboardService(CurrentUserService currentUser,
                                   ClienteRepository clienteRepository,
                                   WorkItemRepository workItemRepository,
                                   PrazoService prazoService) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.prazoService = Objects.requireNonNull(prazoService);
    }

    @Transactional(readOnly = true)
    public AdvogadoDashboardDto.SummaryResponse summary(int horizonDays, int maxItems) {
        Usuario u = currentUser.getRequired();
        Long uid = u.getId();
        if (uid == null) {
            throw new IllegalStateException("usuario_sem_id");
        }

        Instant now = Instant.now();
        int safeDays = Math.min(Math.max(horizonDays, 1), 90);
        int safeMax = Math.min(Math.max(maxItems, 1), 50);
        Instant limit = now.plusSeconds((long) safeDays * 24L * 3600L);

        long ativos = clienteRepository.countByAdvogado_IdAndStatus(uid, StatusCliente.ATIVO);
        long arquivados = clienteRepository.countByAdvogado_IdAndStatus(uid, StatusCliente.ARQUIVADO);

        long overdue = workItemRepository.countOverdueByAssignedUser(uid, now);
        long dueSoon = workItemRepository.countDueByAssignedUser(uid, limit);

        List<WorkItem> dueItems = workItemRepository.findDueByAssignedUser(uid, limit, PageRequest.of(0, safeMax));

        List<AdvogadoDashboardDto.WorkItemLite> prazosCriticos = dueItems.stream()
                .sorted(Comparator.comparing(WorkItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(safeMax)
                .map(this::toWorkItemLite)
                .toList();

        List<EventoProcessual> agendaAll = prazoService.getAgendaDoUsuario(uid);
        LocalDateTime agoraLocal = LocalDateTime.now();
        LocalDateTime ate = agoraLocal.plusDays(safeDays);

        List<AdvogadoDashboardDto.AgendaEventLite> agendaProxima = new ArrayList<>();
        for (EventoProcessual e : agendaAll) {
            if (e == null) continue;
            if (e.getStatus() != null && e.getStatus() != StatusEvento.PENDENTE) continue;
            LocalDateTime fim = e.getDataFim();
            if (fim == null) continue;
            if (fim.isBefore(agoraLocal)) continue;
            if (fim.isAfter(ate)) continue;
            agendaProxima.add(toAgendaLite(e));
            if (agendaProxima.size() >= safeMax) break;
        }

        return AdvogadoDashboardDto.SummaryResponse.builder()
                .generatedAt(now)
                .clientesAtivos(ativos)
                .clientesArquivados(arquivados)
                .workItemsOverdue(overdue)
                .workItemsDueSoon(dueSoon)
                .agendaProxima(agendaProxima)
                .prazosCriticos(prazosCriticos)
                .build();
    }

    private AdvogadoDashboardDto.WorkItemLite toWorkItemLite(WorkItem w) {
        if (w == null) return null;
        return AdvogadoDashboardDto.WorkItemLite.builder()
                .id(w.getId())
                .processoId(w.getProcesso() != null ? w.getProcesso().getId() : null)
                .processoNumero(w.getProcesso() != null ? w.getProcesso().getNumeroUnificado() : null)
                .titulo(w.getTitulo())
                .dueAt(w.getDueAt())
                .status(w.getStatus())
                .prioridade(w.getPrioridade())
                .build();
    }

    private AdvogadoDashboardDto.AgendaEventLite toAgendaLite(EventoProcessual e) {
        return AdvogadoDashboardDto.AgendaEventLite.builder()
                .id(e.getId())
                .tipo(e.getTipo() != null ? e.getTipo().name() : null)
                .titulo(e.getTitulo())
                .dataInicio(e.getDataInicio())
                .dataFim(e.getDataFim())
                .processoId(e.getProcesso() != null ? e.getProcesso().getId() : null)
                .processoNumero(e.getProcesso() != null ? e.getProcesso().getNumeroUnificado() : null)
                .build();
    }
}
