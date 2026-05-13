package com.tcc.pjb.backend.service.juiz.topology;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.juiz.handoff.JuizGabineteHandoffService;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JuizGabineteTopologyOrchestrationService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final JuizProcessoGuardRailService guardRailService;
    private final JuizGabineteRoutingResolver routingResolver;
    private final JuizGabineteHandoffService handoffService;

    public JuizGabineteTopologyOrchestrationService(CurrentUserService currentUserService,
                                                    ProcessoRepository processoRepository,
                                                    WorkItemRepository workItemRepository,
                                                    JuizProcessoGuardRailService guardRailService,
                                                    JuizGabineteRoutingResolver routingResolver,
                                                    JuizGabineteHandoffService handoffService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.guardRailService = Objects.requireNonNull(guardRailService);
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.handoffService = Objects.requireNonNull(handoffService);
    }

    @Transactional(readOnly = true)
    public GabineteTopologySnapshot topologia(Long processoId) {
        Usuario actor = requireJudgeActor();
        Processo processo = loadProcesso(processoId);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.avaliar(processoId, "TOPOLOGIA_GABINETE");
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        List<WorkItem> ativos = activeJudgeItems(processoId);
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Mesa judicial resolvida por topologia nacional, cobertura territorial e lane processual.");
        fundamentos.add("Gabinete de captura: " + routing.gabineteDesk() + '.');
        fundamentos.add("Inbox do gabinete: " + routing.gabineteInboxKey() + '.');
        fundamentos.add("Secretaria de retorno: " + routing.secretariatRouting().secretariatCode() + '.');
        fundamentos.add("Canal de sessão: " + routing.sessionChannel() + '.');
        if (!guard.allowed()) {
            fundamentos.add("Guard rails ativos detectaram restrição de captura para este magistrado.");
        }
        return new GabineteTopologySnapshot(
                actor.getId(),
                actor.getNome(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                routing,
                guard,
                ativos.stream().map(this::toWorkItemSnapshot).toList(),
                List.copyOf(fundamentos)
        );
    }

    @Transactional
    public GabineteCaptureSnapshot capturar(Long processoId) {
        Usuario actor = requireJudgeActor();
        Processo processo = loadProcesso(processoId);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.requireAtuacaoPermitida(processo, actor, "CAPTURAR_PROCESSO_GABINETE");
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem conflicting = activeJudgeItems(processoId).stream()
                .filter(item -> item.getAssignedUser() != null)
                .filter(item -> item.getAssignedUser().isMagistrado())
                .filter(item -> !Objects.equals(item.getAssignedUser().getId(), actor.getId()))
                .findFirst()
                .orElse(null);
        if (conflicting != null) {
            throw new AccessDeniedPjbException("Processo já está capturado por outro magistrado em mesa incompatível.");
        }
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, routing.captureTemplateCode(processoId))
                .orElseGet(() -> WorkItem.builder()
                        .processo(processo)
                        .faseOrigem(processo.getFaseAtual())
                        .templateCode(routing.captureTemplateCode(processoId))
                        .type(WorkItemType.DECISAO)
                        .titulo("Captura topológica do gabinete — " + firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), "PROCESSO"))
                        .descricao("Captura do processo na mesa judicial topológica com isolamento por justiça, instância, regime e lane.")
                        .build());
        Instant now = Instant.now();
        item.setQueueCode(routing.gabineteDesk());
        item.setInboxKey(routing.gabineteInboxKey());
        item.setAssignedRole(resolveJudgeRole(actor));
        item.setAssignedUser(actor);
        item.setStatus(WorkItemStatus.EM_EXECUCAO);
        item.setPrioridade(resolvePriority(processo, routing));
        item.setBlocking(true);
        item.setUf(firstNonBlank(processo.getUf(), actor.getUf()));
        item.setComarca(firstNonBlank(processo.getComarca(), actor.getComarca()));
        item.setBaseLegal("Captura judicial topológica vinculada ao guard rail e à mesa correta do gabinete.");
        item.setDueAt(now.plus(routing.captureSla()));
        WorkItem saved = workItemRepository.save(item);
        return new GabineteCaptureSnapshot(
                saved.getId(),
                processoId,
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                actor.getId(),
                actor.getNome(),
                routing,
                guard,
                "CAPTURADO",
                saved.getDueAt(),
                List.of(
                        "Processo capturado na mesa judicial correta.",
                        "Inbox topológico do gabinete aplicado.",
                        "Guard rails validados antes da captura."
                )
        );
    }

    @Transactional
    public GabineteReleaseSnapshot liberar(Long processoId, String destino) {
        JuizGabineteHandoffService.HandoffAction handoff = handoffService.encaminharParaSecretaria(processoId, destino, null);
        Processo processo = loadProcesso(processoId);
        Usuario actor = requireJudgeActor();
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.avaliar(processoId, "LIBERAR_PROCESSO_GABINETE");
        String stage = normalizeStage(destino);
        return new GabineteReleaseSnapshot(
                handoff.workItemId(),
                processoId,
                handoff.numeroProcesso(),
                actor.getId(),
                actor.getNome(),
                stage,
                routing,
                guard,
                handoff.queueCode(),
                handoff.inboxKey(),
                handoff.dueAt(),
                handoff.effects()
        );
    }

    private int resolvePriority(Processo processo, JuizGabineteRoutingProfile routing) {
        int priority = 2;
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != com.tcc.pjb.backend.model.entity.enums.NivelSigilo.PUBLICO) {
            priority = 0;
        }
        if (routing.topology() != null && routing.topology().instanceAxis() != null && routing.topology().instanceAxis().contains("SEGUNDO")) {
            priority = Math.min(priority, 1);
        }
        return priority;
    }

    private String resolveSecretariatQueue(String stage, SecretariatOperationalRoutingProfile routing) {
        return switch (stage) {
            case "SANEAMENTO" -> firstNonBlank(routing.saneamentoQueueCode(), routing.executionQueueCode(), routing.receiptQueueCode());
            case "AUDIENCIA" -> firstNonBlank(routing.audienceQueueCode(), routing.executionQueueCode(), routing.receiptQueueCode());
            default -> firstNonBlank(routing.executionQueueCode(), routing.receiptQueueCode(), routing.saneamentoQueueCode());
        };
    }

    private String resolveSecretariatInbox(String stage, SecretariatOperationalRoutingProfile routing) {
        return switch (stage) {
            case "SANEAMENTO" -> firstNonBlank(routing.saneamentoInboxKey(), routing.executionInboxKey(), routing.receiptInboxKey());
            case "AUDIENCIA" -> firstNonBlank(routing.audienceInboxKey(), routing.executionInboxKey(), routing.receiptInboxKey());
            default -> firstNonBlank(routing.executionInboxKey(), routing.receiptInboxKey(), routing.saneamentoInboxKey());
        };
    }

    private Usuario requireJudgeActor() {
        Usuario actor = currentUserService.getRequired();
        if (actor.getTipoUsuario() == null || !actor.getTipoUsuario().isMagistratura()) {
            throw new AccessDeniedPjbException("Apenas magistratura pode operar a topologia do gabinete.");
        }
        return actor;
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private String normalizeStage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "EXECUCAO";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return switch (normalized) {
            case "SANEAMENTO", "RECEBIMENTO", "TRIAGEM" -> "SANEAMENTO";
            case "PAUTA", "AUDIENCIA", "AUDIENCIAS" -> "AUDIENCIA";
            default -> "EXECUCAO";
        };
    }

    private List<WorkItem> activeJudgeItems(Long processoId) {
        return workItemRepository.findAllByProcesso(processoId).stream()
                .filter(item -> item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO)
                .filter(item -> item.getAssignedRole() != null && item.getAssignedRole().isMagistratura())
                .toList();
    }

    private TipoUsuario resolveJudgeRole(Usuario actor) {
        if (actor == null || actor.getTipoUsuario() == null) {
            return TipoUsuario.MAGISTRADO;
        }
        return actor.getTipoUsuario();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private WorkItemSnapshot toWorkItemSnapshot(WorkItem item) {
        return new WorkItemSnapshot(
                item.getId(),
                item.getTemplateCode(),
                item.getTitulo(),
                item.getQueueCode(),
                item.getInboxKey(),
                item.getAssignedUser() == null ? null : item.getAssignedUser().getId(),
                item.getAssignedUser() == null ? null : item.getAssignedUser().getNome(),
                item.getStatus() == null ? null : item.getStatus().name(),
                item.getDueAt(),
                item.isBlocking()
        );
    }

    public record GabineteTopologySnapshot(
            Long actorId,
            String actorNome,
            Long processoId,
            String numeroProcesso,
            JuizGabineteRoutingProfile routing,
            JuizProcessoGuardRailService.GuardRailSnapshot guardRail,
            List<WorkItemSnapshot> activeItems,
            List<String> fundamentos
    ) {
    }

    public record GabineteCaptureSnapshot(
            Long workItemId,
            Long processoId,
            String numeroProcesso,
            Long actorId,
            String actorNome,
            JuizGabineteRoutingProfile routing,
            JuizProcessoGuardRailService.GuardRailSnapshot guardRail,
            String status,
            Instant dueAt,
            List<String> efeitos
    ) {
    }

    public record GabineteReleaseSnapshot(
            Long workItemId,
            Long processoId,
            String numeroProcesso,
            Long actorId,
            String actorNome,
            String stage,
            JuizGabineteRoutingProfile routing,
            JuizProcessoGuardRailService.GuardRailSnapshot guardRail,
            String queueCode,
            String inboxKey,
            Instant dueAt,
            List<String> efeitos
    ) {
    }

    public record WorkItemSnapshot(
            Long id,
            String templateCode,
            String titulo,
            String queueCode,
            String inboxKey,
            Long assignedUserId,
            String assignedUserNome,
            String status,
            Instant dueAt,
            boolean blocking
    ) {
    }
}
