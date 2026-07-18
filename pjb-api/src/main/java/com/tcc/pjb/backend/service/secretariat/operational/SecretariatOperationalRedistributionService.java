package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;


@Service
public class SecretariatOperationalRedistributionService {

    private final SecretariatOperationalAssignmentService assignmentService;
    private final WorkItemRepository workItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecretariatQueueProjectionService projectionService;

    public SecretariatOperationalRedistributionService(SecretariatOperationalAssignmentService assignmentService,
                                                       WorkItemRepository workItemRepository,
                                                       UsuarioRepository usuarioRepository,
                                                       SecretariatQueueProjectionService projectionService) {
        this.assignmentService = Objects.requireNonNull(assignmentService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
    }

    @Transactional(readOnly = true)
    public RedistributionSnapshot avaliar(Processo processo,
                                          SecretariatOperationalRoutingProfile routing,
                                          String stage) {
        SecretariatOperationalAssignmentService.AssignmentSnapshot assignment = assignmentService.avaliar(processo, routing, stage);
        List<WorkItem> items = assignment.targetWorkItemIds().isEmpty() ? List.of() : workItemRepository.findAllById(assignment.targetWorkItemIds());
        Usuario current = items.stream().map(WorkItem::getAssignedUser).filter(Objects::nonNull).findFirst().orElse(null);
        List<CandidateCapacity> capacities = assignment.candidates().stream()
                .map(candidate -> capacity(candidate, current))
                .sorted(Comparator.comparingInt(CandidateCapacity::redistributionScore).reversed().thenComparingInt(CandidateCapacity::activeItems).thenComparing(CandidateCapacity::nome))
                .toList();
        CandidateCapacity recommended = capacities.stream()
                .filter(candidate -> candidate.usuarioId() != null)
                .filter(candidate -> current == null || !candidate.usuarioId().equals(current.getId()))
                .findFirst()
                .orElse(capacities.isEmpty() ? null : capacities.getFirst());
        boolean redistributionSuggested = recommended != null && current != null && !Objects.equals(recommended.usuarioId(), current.getId())
                && recommended.redistributionScore() >= capacities.stream().filter(c -> c.usuarioId() != null && Objects.equals(c.usuarioId(), current.getId())).mapToInt(CandidateCapacity::redistributionScore).max().orElse(0) + 40;
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Redistribuição interna considera carga ativa, atrasos, throughput recente e afinidade de célula.");
        fundamentos.add("Stage analisado: " + assignment.stage() + ".");
        fundamentos.add("WorkItems alvo: " + assignment.targetWorkItemIds() + ".");
        if (current != null) {
            fundamentos.add("Responsável atual: " + current.getNome() + " (#" + current.getId() + ").");
        }
        if (recommended != null) {
            fundamentos.add("Melhor candidato para redistribuição: " + recommended.nome() + " (#" + recommended.usuarioId() + ").");
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("targetWorkItemCount", assignment.targetWorkItemIds().size());
        metrics.put("candidateCount", capacities.size());
        metrics.put("currentUserId", current == null ? null : current.getId());
        metrics.put("currentUserName", current == null ? null : current.getNome());
        metrics.put("redistributionSuggested", redistributionSuggested);
        metrics.put("stage", assignment.stage());
        metrics.entrySet().removeIf(entry -> entry.getValue() == null);
        return new RedistributionSnapshot(assignment.stage(), current, recommended, redistributionSuggested, List.copyOf(capacities), List.copyOf(fundamentos), Map.copyOf(metrics), List.copyOf(assignment.targetWorkItemIds()));
    }

    @PjbTransactionalBudget(operation = "secretariat.operational.redistribuir", maxMillis = 3000)
    @Transactional
    public RedistributionSnapshot redistribuir(Processo processo,
                                               Usuario actor,
                                               SecretariatOperationalRoutingProfile routing,
                                               String stage) {
        RedistributionSnapshot snapshot = avaliar(processo, routing, stage);
        if (!snapshot.redistributionSuggested() || snapshot.recommended() == null || snapshot.targetWorkItemIds().isEmpty()) {
            return snapshot;
        }
        Usuario selected = usuarioRepository.findById(snapshot.recommended().usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", snapshot.recommended().usuarioId()));
        List<WorkItem> items = workItemRepository.findAllById(snapshot.targetWorkItemIds());
        for (WorkItem item : items) {
            item.setAssignedUser(selected);
            item.setAssignedRole(selected.getTipoUsuario());
            String previous = snapshot.current() == null ? "SEM_RESPONSAVEL" : snapshot.current().getNome() + " (#" + snapshot.current().getId() + ')';
            String suffix = "\nRedistribuído por " + actor.getNome() + " (#" + actor.getId() + ") para " + selected.getNome() + " (#" + selected.getId() + ") a partir de " + previous;
            item.setDescricao((item.getDescricao() == null ? "" : item.getDescricao()) + suffix);
            projectionService.upsert(item, item.getPrioridade() == null ? 60 : 60 + Math.max(0, (6 - item.getPrioridade()) * 8),
                    List.of("REDISTRIBUICAO_INTERNA", snapshot.stage(), routing.secretariatCode(), selected.getNome()));
        }
        workItemRepository.saveAll(items);
        return avaliar(processo, routing, stage);
    }

    private CandidateCapacity capacity(SecretariatOperationalAssignmentService.CandidateLoad candidate, Usuario current) {
        long completed30d = candidate.usuarioId() == null ? 0L : workItemRepository.countCompletedByAssignedUserAfter(candidate.usuarioId(), Instant.now().minus(30, ChronoUnit.DAYS));
        int historicalThroughput = Math.toIntExact(Math.min(180, completed30d * 3));
        int continuityBonus = current != null && Objects.equals(candidate.usuarioId(), current.getId()) ? 40 : 0;
        int score = candidate.rankingScore() + historicalThroughput + continuityBonus;
        List<String> fundamentos = new ArrayList<>(candidate.reasons());
        fundamentos.add("Concluídos nos últimos 30 dias: " + completed30d);
        if (continuityBonus > 0) {
            fundamentos.add("Bônus de continuidade por já estar atribuído ao stage.");
        }
        return new CandidateCapacity(candidate.usuarioId(), candidate.nome(), candidate.cellCode(), candidate.usuario() == null ? null : candidate.usuario().getTipoUsuario(), candidate.activeItems(), candidate.overdueItems(), candidate.dueSoonItems(), Math.toIntExact(completed30d), score, List.copyOf(fundamentos));
    }

    public record RedistributionSnapshot(
            String stage,
            Usuario current,
            CandidateCapacity recommended,
            boolean redistributionSuggested,
            List<CandidateCapacity> candidates,
            List<String> fundamentos,
            Map<String, Object> metrics,
            List<Long> targetWorkItemIds
    ) {
    }

    public record CandidateCapacity(
            Long usuarioId,
            String nome,
            String cellCode,
            com.tcc.pjb.backend.model.entity.enums.TipoUsuario tipoUsuario,
            int activeItems,
            int overdueItems,
            int dueSoonItems,
            int completed30d,
            int redistributionScore,
            List<String> fundamentos
    ) {
    }
}
