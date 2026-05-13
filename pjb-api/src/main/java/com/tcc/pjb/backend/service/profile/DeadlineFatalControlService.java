package com.tcc.pjb.backend.service.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.profile.DeadlineFatalControlResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;

@Service
public class DeadlineFatalControlService {

    private final CurrentUserService currentUserService;
    private final WorkItemRepository workItemRepository;
    private final PjbTimeService pjbTimeService;

    public DeadlineFatalControlService(CurrentUserService currentUserService,
                                       WorkItemRepository workItemRepository,
                                       PjbTimeService pjbTimeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.pjbTimeService = Objects.requireNonNull(pjbTimeService);
    }

    @Transactional(readOnly = true)
    public DeadlineFatalControlResponse monitor(Integer horizonDays, Integer limit) {
        Usuario actor = currentUserService.getRequired();
        int safeHorizon = horizonDays == null || horizonDays < 1 ? 15 : Math.min(horizonDays, 90);
        int safeLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 200);
        Instant now = pjbTimeService.nowUtc();
        Instant max = now.plus(safeHorizon, ChronoUnit.DAYS);
        Map<Long, WorkItem> unique = new LinkedHashMap<>();
        if (actor.getId() != null) {
            workItemRepository.findDueByAssignedUser(actor.getId(), max, PageRequest.of(0, safeLimit)).forEach(item -> unique.putIfAbsent(item.getId(), item));
        }
        if (actor.getTipoUsuario() != null) {
            workItemRepository.findDueByRoleAndTerritory(actor.getTipoUsuario(), actor.getUf(), actor.getComarca(), max, PageRequest.of(0, safeLimit)).forEach(item -> unique.putIfAbsent(item.getId(), item));
        }
        List<DeadlineFatalControlResponse.DeadlineRiskItem> itens = unique.values().stream()
                .filter(item -> item.getDueAt() != null)
                .sorted(Comparator.comparing(WorkItem::getDueAt))
                .map(item -> toRiskItem(now, item, pjbTimeService.legalZone()))
                .toList();
        long fatais = itens.stream().filter(item -> "FATAL".equals(item.riskLevel())).count();
        long criticos = itens.stream().filter(item -> "CRITICO".equals(item.riskLevel())).count();
        long altos = itens.stream().filter(item -> "ALTO".equals(item.riskLevel())).count();
        String riskLevel = fatais > 0 ? "FATAL" : criticos > 0 ? "CRITICO" : altos > 0 ? "ALTO" : itens.isEmpty() ? "ESTAVEL" : "ATENCAO";
        return new DeadlineFatalControlResponse(
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                riskLevel,
                itens.size(),
                fatais,
                criticos,
                altos,
                buildActions(riskLevel, itens),
                itens,
                now
        );
    }

    private DeadlineFatalControlResponse.DeadlineRiskItem toRiskItem(Instant now, WorkItem item, ZoneId zoneId) {
        long dias = daysUntilDeadline(now, item.getDueAt(), zoneId);
        Processo processo = item.getProcesso();
        return new DeadlineFatalControlResponse.DeadlineRiskItem(
                item.getId(),
                processo != null ? processo.getId() : null,
                safeNumero(processo),
                item.getTitulo(),
                classify(dias),
                dias,
                item.getDueAt(),
                item.getPrioridade(),
                item.getQueueCode(),
                item.getBaseLegal()
        );
    }

    private List<String> buildActions(String level, List<DeadlineFatalControlResponse.DeadlineRiskItem> itens) {
        List<String> actions = new ArrayList<>();
        if ("FATAL".equals(level)) {
            actions.add("Acionar tratamento prioritário imediato para os itens com vencimento no dia ou já vencidos.");
            actions.add("Revalidar tempestividade, ciência e necessidade de protocolo emergencial.");
        }
        if ("CRITICO".equals(level) || "FATAL".equals(level)) {
            actions.add("Realocar capacidade operacional e travar novas filas não urgentes até estabilização.");
        }
        if (itens.stream().anyMatch(item -> hasText(item.baseLegal()))) {
            actions.add("Conferir a base legal do item antes da prática do ato para evitar nulidade operacional.");
        }
        if (actions.isEmpty()) {
            actions.add("Monitoramento estável; manter conferência diária do radar de prazos.");
        }
        return List.copyOf(actions.stream().distinct().toList());
    }

    private long daysUntilDeadline(Instant now, Instant dueAt, ZoneId zoneId) {
        LocalDate today = LocalDate.ofInstant(now, zoneId);
        LocalDate due = LocalDate.ofInstant(dueAt, zoneId);
        return ChronoUnit.DAYS.between(today, due);
    }

    private String classify(long dias) {
        if (dias <= 0) {
            return "FATAL";
        }
        if (dias <= 2) {
            return "CRITICO";
        }
        if (dias <= 5) {
            return "ALTO";
        }
        if (dias <= 10) {
            return "ATENCAO";
        }
        return "ESTAVEL";
    }

    private String safeNumero(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (hasText(processo.getNumeroUnificado())) {
            return processo.getNumeroUnificado();
        }
        if (hasText(processo.getNumeroProcesso())) {
            return processo.getNumeroProcesso();
        }
        return processo.getId() != null ? String.valueOf(processo.getId()) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
