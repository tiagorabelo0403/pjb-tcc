package com.tcc.pjb.backend.service.processual.pendencia;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.pendencia.OperationalPendingDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.pendencia.OperationalPendingItemResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;

@Service
public class OperationalPendingDashboardService {

    private final CurrentUserService currentUserService;
    private final WorkItemRepository workItemRepository;

    public OperationalPendingDashboardService(CurrentUserService currentUserService,
                                              WorkItemRepository workItemRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
    }

    public OperationalPendingDashboardResponse dashboard(Integer limite) {
        Usuario usuario = currentUserService.getRequired();
        int size = limite == null ? 40 : Math.min(Math.max(limite, 1), 100);
        var userPage = workItemRepository.inboxByUser(usuario.getId(), PageRequest.of(0, size));
        var rolePage = workItemRepository.inboxByRoleAndTerritory(usuario.getTipoUsuario(), usuario.getUf(), usuario.getComarca(), PageRequest.of(0, size));
        Map<Long, OperationalPendingItemResponse> merged = new LinkedHashMap<>();
        userPage.getContent().stream().map(item -> toItem(item, true)).forEach(item -> merged.put(item.workItemId(), item));
        rolePage.getContent().stream().map(item -> toItem(item, false)).forEach(item -> merged.putIfAbsent(item.workItemId(), item));
        List<OperationalPendingItemResponse> itens = merged.values().stream()
                .sorted(Comparator.comparing(OperationalPendingItemResponse::dueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OperationalPendingItemResponse::prioridade, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(OperationalPendingItemResponse::workItemId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        long vencidas = itens.stream().filter(item -> item.dueAt() != null && item.dueAt().isBefore(Instant.now())).count();
        long criticas24h = itens.stream().filter(item -> item.dueAt() != null && item.dueAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS))).count();
        long bloqueantes = itens.stream().filter(OperationalPendingItemResponse::bloqueante).count();
        int processosAfetados = new LinkedHashSet<>(itens.stream().map(OperationalPendingItemResponse::processoId).filter(Objects::nonNull).toList()).size();
        return new OperationalPendingDashboardResponse(
                usuario.getId(),
                usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name(),
                usuario.getUf(),
                usuario.getComarca(),
                userPage.getTotalElements(),
                rolePage.getTotalElements(),
                itens.size(),
                vencidas,
                criticas24h,
                bloqueantes,
                processosAfetados,
                aggregate(itens, OperationalPendingItemResponse::type),
                aggregate(itens, OperationalPendingItemResponse::status),
                aggregate(itens, OperationalPendingItemResponse::fila),
                itens,
                Instant.now()
        );
    }

    private OperationalPendingItemResponse toItem(WorkItem item, boolean atribuidoAoUsuario) {
        return new OperationalPendingItemResponse(
                item.getId(),
                item.getProcesso() == null ? null : item.getProcesso().getId(),
                item.getProcesso() == null ? null : item.getProcesso().getNumeroProcesso(),
                item.getTitulo(),
                item.getType() == null ? null : item.getType().name(),
                item.getStatus() == null ? null : item.getStatus().name(),
                item.getQueueCode(),
                item.getInboxKey(),
                item.getPrioridade(),
                item.getDueAt(),
                item.isBlocking(),
                atribuidoAoUsuario
        );
    }

    private Map<String, Long> aggregate(List<OperationalPendingItemResponse> itens,
                                        java.util.function.Function<OperationalPendingItemResponse, String> extractor) {
        LinkedHashMap<String, Long> map = new LinkedHashMap<>();
        itens.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .forEach(value -> map.merge(value, 1L, Long::sum));
        return map;
    }
}
