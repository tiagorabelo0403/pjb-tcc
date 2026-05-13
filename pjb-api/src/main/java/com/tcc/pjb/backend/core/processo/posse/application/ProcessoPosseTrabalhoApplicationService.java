package com.tcc.pjb.backend.core.processo.posse.application;

import com.tcc.pjb.backend.core.processo.posse.domain.ProcessoPosseAggregate;
import com.tcc.pjb.backend.core.processo.posse.domain.ProcessoPosseItem;
import com.tcc.pjb.backend.core.processo.posse.domain.ProcessoPosseTransicao;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPosseTrabalhoApplicationService {

    private final WorkItemRepository workItemRepository;
    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;

    public ProcessoPosseTrabalhoApplicationService(WorkItemRepository workItemRepository,
                                                   ProcessoTrabalhoApplicationService processoTrabalhoApplicationService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoTrabalhoApplicationService = Objects.requireNonNull(processoTrabalhoApplicationService);
    }

    public ProcessoPosseAggregate detalhar(Long processoId) {
        ProcessoTrabalhoAggregate trabalho = processoTrabalhoApplicationService.detalhar(processoId);
        List<WorkItem> workItems = workItemRepository.findAllByProcesso(processoId);
        List<ProcessoPosseItem> items = workItems.stream()
                .sorted(Comparator.comparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toPosseItem)
                .toList();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        if (items.stream().anyMatch(item -> item.currentOwnership().equals("SEM_POSSE"))) {
            alerts.add("EXISTE_WORK_ITEM_SEM_POSSE_RESOLVIDA");
        }
        if (items.stream().anyMatch(item -> item.claimable() && item.transitions().size() < 2)) {
            alerts.add("EXISTE_ITEM_CLAIMABLE_SEM_TRILHA_TRANSITORIA_MINIMA");
        }
        if (items.stream().anyMatch(item -> item.guards().contains("BLOQUEANTE_EM_ABERTO"))) {
            alerts.add("EXISTE_ITEM_BLOQUEANTE_COM_POSSE_SENSIVEL");
        }
        return new ProcessoPosseAggregate(
                trabalho.identity(),
                workItems.size(),
                workItems.stream().filter(this::open).count(),
                items.stream().filter(ProcessoPosseItem::claimable).count(),
                alerts.size(),
                items,
                List.copyOf(alerts),
                Instant.now()
        );
    }

    private ProcessoPosseItem toPosseItem(WorkItem workItem) {
        List<ProcessoPosseTransicao> transitions = buildTransitions(workItem);
        String ownership = resolveOwnership(workItem);
        boolean claimable = open(workItem) && workItem.getAssignedUser() == null && workItem.getAssignedRole() != null;
        return new ProcessoPosseItem(
                workItem.getId(),
                workItem.getTitulo(),
                ownership,
                trailHash(workItem, transitions),
                claimable,
                transitions,
                guards(workItem)
        );
    }

    private List<ProcessoPosseTransicao> buildTransitions(WorkItem workItem) {
        ArrayList<ProcessoPosseTransicao> transitions = new ArrayList<>();
        long seq = 1L;
        transitions.add(new ProcessoPosseTransicao(
                workItem.getId(),
                seq++,
                "CRIADO",
                "SEM_POSSE",
                stateOfQueue(workItem),
                defaultInstant(workItem.getCreatedAt(), workItem.getUpdatedAt()),
                "SISTEMA",
                lane(workItem),
                workItem.isBlocking()
        ));
        if (workItem.getAssignedRole() != null) {
            transitions.add(new ProcessoPosseTransicao(
                    workItem.getId(),
                    seq++,
                    "POSSE_POR_PAPEL",
                    stateOfQueue(workItem),
                    "PAPEL:" + workItem.getAssignedRole().name(),
                    defaultInstant(workItem.getCreatedAt(), workItem.getUpdatedAt()),
                    "REGRA_DE_FILA",
                    lane(workItem),
                    workItem.isBlocking()
            ));
        }
        if (workItem.getAssignedUser() != null) {
            transitions.add(new ProcessoPosseTransicao(
                    workItem.getId(),
                    seq++,
                    "POSSE_NOMINAL",
                    workItem.getAssignedRole() == null ? stateOfQueue(workItem) : "PAPEL:" + workItem.getAssignedRole().name(),
                    "USUARIO:" + workItem.getAssignedUser().getId(),
                    defaultInstant(workItem.getUpdatedAt(), workItem.getCreatedAt()),
                    "ALOCAÇÃO_NOMINAL",
                    lane(workItem),
                    workItem.isBlocking()
            ));
        }
        if (workItem.getStatus() == WorkItemStatus.EM_EXECUCAO) {
            transitions.add(new ProcessoPosseTransicao(
                    workItem.getId(),
                    seq++,
                    "EXECUCAO_ATIVA",
                    resolveOwnership(workItem),
                    resolveOwnership(workItem) + ":EM_EXECUCAO",
                    defaultInstant(workItem.getUpdatedAt(), workItem.getCreatedAt()),
                    "WORKSTREAM",
                    lane(workItem),
                    workItem.isBlocking()
            ));
        }
        if (workItem.getStatus() == WorkItemStatus.CONCLUIDO || workItem.getStatus() == WorkItemStatus.CANCELADO) {
            transitions.add(new ProcessoPosseTransicao(
                    workItem.getId(),
                    seq,
                    "ENCERRADO",
                    resolveOwnership(workItem),
                    "SEM_POSSE_FINAL",
                    defaultInstant(workItem.getUpdatedAt(), workItem.getCreatedAt()),
                    "WORKSTREAM",
                    lane(workItem),
                    false
            ));
        }
        return List.copyOf(transitions);
    }

    private List<String> guards(WorkItem workItem) {
        LinkedHashSet<String> guards = new LinkedHashSet<>();
        guards.add("POSSE_TRANSITORIA_GERA_NOVA_TRILHA_E_NAO_SOBRESCRITA_CEGA");
        if (open(workItem)) {
            guards.add("ITEM_ABERTO_EXIGE_POSSE_RESOLVIDA_OU_CLAIM_FORMAL");
        }
        if (workItem.isBlocking()) {
            guards.add("BLOQUEANTE_EM_ABERTO");
        }
        if (workItem.getAssignedUser() == null && workItem.getAssignedRole() != null) {
            guards.add("CLAIM_FORMAL_OBRIGATORIO");
        }
        if (workItem.getAssignedRole() == null && workItem.getAssignedUser() == null && open(workItem)) {
            guards.add("SEM_POSSE_FORMAL_ATIVA");
        }
        return List.copyOf(guards);
    }

    private String trailHash(WorkItem workItem, List<ProcessoPosseTransicao> transitions) {
        String seed = workItem.getId() + "|" + workItem.getTemplateCode() + "|" + transitions.stream()
                .map(transition -> transition.sequence() + ":" + transition.eventCode() + ":" + transition.toState() + ":" + transition.occurredAt())
                .reduce("", String::concat);
        return Hashes.sha256HexPrefix(seed, 24);
    }

    private String resolveOwnership(WorkItem workItem) {
        if (workItem.getAssignedUser() != null) {
            return "USUARIO:" + workItem.getAssignedUser().getId();
        }
        if (workItem.getAssignedRole() != null) {
            return "PAPEL:" + workItem.getAssignedRole().name();
        }
        return open(workItem) ? "SEM_POSSE" : "FINALIZADO";
    }

    private String stateOfQueue(WorkItem workItem) {
        return "FILA:" + (workItem.getQueueCode() == null ? "NAO_INFORMADA" : workItem.getQueueCode());
    }

    private String lane(WorkItem workItem) {
        return workItem.getInboxKey() == null ? "INBOX_NAO_INFORMADA" : workItem.getInboxKey();
    }

    private Instant defaultInstant(Instant preferred, Instant fallback) {
        return preferred != null ? preferred : fallback != null ? fallback : Instant.now();
    }

    private boolean open(WorkItem workItem) {
        return workItem.getStatus() == null || (workItem.getStatus() != WorkItemStatus.CONCLUIDO && workItem.getStatus() != WorkItemStatus.CANCELADO);
    }
}
