package com.tcc.pjb.backend.service.rito;

import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine;
import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine.AssignmentContext;
import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine.AssignmentResult;
import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine.WorkItemSpec;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.kernel.process.payload.PhaseChangedPayload;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.validator.FaseValidatorService;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.rito.dto.AdvanceRitoRequest;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RitoWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(RitoWorkflowService.class);

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final RitoPackService ritoPackService;
    private final FaseValidatorService faseValidatorService;
    private final CurrentUserService currentUserService;
    private final ProcessEventStore processEventStore;
    private final PjbTimeService timeService;
    private final ActorAssignmentEngine actorAssignmentEngine;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;

    public RitoWorkflowService(ProcessoRepository processoRepository,
                               WorkItemRepository workItemRepository,
                               MovimentacaoProcessualRepository movimentacaoRepository,
                               RitoPackService ritoPackService,
                               FaseValidatorService faseValidatorService,
                               CurrentUserService currentUserService,
                               ProcessEventStore processEventStore,
                               PjbTimeService timeService,
                               ActorAssignmentEngine actorAssignmentEngine,
                               ProceduralCanonicalResolver proceduralCanonicalResolver) {
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.ritoPackService = ritoPackService;
        this.faseValidatorService = faseValidatorService;
        this.currentUserService = currentUserService;
        this.processEventStore = processEventStore;
        this.timeService = timeService;
        this.actorAssignmentEngine = actorAssignmentEngine;
        this.proceduralCanonicalResolver = proceduralCanonicalResolver;
    }

    @Transactional
    public RitoPlanDto seedIfNeeded(Long processoId) {
        Processo processo = getProcesso(processoId);
        seedWorkItemsForCurrentStage(processo);
        return plan(processoId);
    }

    @Transactional(readOnly = true)
    public RitoPlanDto plan(Long processoId) {
        Processo processo = getProcesso(processoId);
        RitoProcessual rito = resolveEffectiveRito(processo);
        if (rito != null && processo.getRito() != rito) {
            processo.setRito(rito);
        }
        List<WorkItem> all = workItemRepository.findAllByProcesso(processoId);
        List<WorkItemDto> current = all.stream()
                .filter(item -> Objects.equals(item.getFaseOrigem(), processo.getFaseAtual()))
                .filter(item -> item.getStatus() != WorkItemStatus.CANCELADO)
                .map(this::toDto)
                .toList();
        List<WorkItemDto> blockingOpen = all.stream()
                .filter(item -> Objects.equals(item.getFaseOrigem(), processo.getFaseAtual()))
                .filter(WorkItem::isBlocking)
                .filter(item -> item.getStatus() != WorkItemStatus.CONCLUIDO && item.getStatus() != WorkItemStatus.CANCELADO)
                .map(this::toDto)
                .toList();
        return RitoPlanDto.builder()
                .processoId(processo.getId())
                .numeroProcesso(processo.getNumeroUnificado())
                .rito(rito)
                .faseAtual(processo.getFaseAtual())
                .allowedNext(allowedNext(processo).stream().toList())
                .currentStageWork(current)
                .blockingOpen(blockingOpen)
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = "timeline_processo", key = "#processoId")
    public RitoPlanDto advance(Long processoId, AdvanceRitoRequest request) {
        Objects.requireNonNull(request, "request é obrigatório");
        Objects.requireNonNull(request.nextFase(), "nextFase é obrigatória");
        Processo processo = getProcesso(processoId);
        faseValidatorService.validarMudancaFase(processo, request.nextFase());
        Set<FaseProcessual> allowed = allowedNext(processo);
        if (!allowed.isEmpty() && !allowed.contains(request.nextFase())) {
            throw new IllegalStateException("Transição não permitida pelo rito pack. Fase atual: " + processo.getFaseAtual() + " -> " + request.nextFase());
        }
        List<WorkItem> blocking = workItemRepository.findBlockingOpen(processoId, processo.getFaseAtual());
        if (!blocking.isEmpty()) {
            String titles = blocking.stream().map(WorkItem::getTitulo).limit(5).collect(Collectors.joining("; "));
            throw new IllegalStateException("Não é possível avançar. Há tarefas de checklist em aberto: " + titles);
        }
        Usuario ator = currentUserService.getOptional().orElse(null);
        FaseProcessual de = processo.getFaseAtual();
        FaseProcessual para = request.nextFase();
        processo.setFaseAtual(para);
        processo.setDataUltimaMovimentacao(LocalDateTime.ofInstant(timeService.nowUtc(), ZoneOffset.UTC));
        processoRepository.save(processo);
        processEventStore.append(processo.getId(), ProcessEventType.PHASE_CHANGED,
                new PhaseChangedPayload(processo.getId(), de != null ? de.name() : null, para.name(), request.motivo(),
                        LocalDateTime.ofInstant(timeService.nowUtc(), ZoneOffset.UTC)));
        movimentacaoRepository.save(MovimentacaoProcessual.builder()
                .processo(processo)
                .faseDe(de)
                .fasePara(para)
                .descricao(request.motivo() != null ? request.motivo() : "Avanço de fase via motor de ritos")
                .ator(ator)
                .build());
        seedWorkItemsForCurrentStage(processo);
        return plan(processoId);
    }

    private Processo getProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
    }

    private void seedWorkItemsForCurrentStage(Processo processo) {
        RitoProcessual rito = resolveEffectiveRito(processo);
        if (rito == null || processo.getFaseAtual() == null) {
            return;
        }
        if (processo.getRito() != rito) {
            processo.setRito(rito);
            processoRepository.save(processo);
        }
        if (seedUsingActorAssignmentEngine(processo, rito)) {
            return;
        }
        log.warn("[RitoWorkflowService] WORKFLOW_BLUEPRINT_INCOMPLETE para processo {} rito {} fase {}. Seed legado bloqueado para evitar fallback silencioso.",
                processo.getId(), rito, processo.getFaseAtual());
    }

    private boolean seedUsingActorAssignmentEngine(Processo processo, RitoProcessual rito) {
        try {
            AssignmentResult result = actorAssignmentEngine.assign(buildAssignmentContext(processo, rito));
            if (!result.diagnostics().isEmpty()) {
                log.warn("[RitoWorkflowService] Diagnósticos de atribuição para processo {} rito {} fase {}: {}", processo.getId(), processo.getRito(), processo.getFaseAtual(), result.diagnostics());
            }
            if (result.workItems().isEmpty()) {
                return false;
            }
            for (WorkItemSpec spec : result.workItems()) {
                saveSpecIfAbsent(processo, spec);
            }
            return true;
        } catch (RuntimeException ex) {
            log.error("[RitoWorkflowService] Falha ao gerar work items canônicos para processo {} rito {} fase {}", processo.getId(), processo.getRito(), processo.getFaseAtual(), ex);
            return false;
        }
    }

    private AssignmentContext buildAssignmentContext(Processo processo, RitoProcessual rito) {
        return new AssignmentContext(
                processo.getId(),
                rito,
                processo.getFaseAtual(),
                1,
                false,
                false,
                false,
                false,
                processo.getFaseAtual() == FaseProcessual.RECURSAL,
                false,
                List.of(),
                buildExtras(processo)
        );
    }


    private java.util.Map<String, Object> buildExtras(Processo processo) {
        java.util.LinkedHashMap<String, Object> extras = new java.util.LinkedHashMap<>();
        CanonicalContext canonical = resolveCanonicalContext(processo);
        extras.put("canonicalContext", canonical.toMap());
        extras.put("ramoDireito", firstNonBlank(canonical.ramoDireito(), processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null));
        extras.put("classeTpu", firstNonBlank(canonical.classeTpuCodigo(), processo.getClasseProcessual()));
        if (processo.getJurisdicao() != null) {
            if (processo.getJurisdicao().getUf() != null) {
                extras.put("uf", processo.getJurisdicao().getUf());
            }
            if (processo.getJurisdicao().getCidade() != null) {
                extras.put("comarca", processo.getJurisdicao().getCidade());
            }
        }
        return java.util.Map.copyOf(extras);
    }

    private void seedLegacy(Processo processo, RitoProcessual rito) {
        Optional<RitoDefinition> definition = ritoPackService.get(rito);
        if (definition.isEmpty() || definition.get().getStages() == null) {
            return;
        }
        RitoStage stage = definition.get().getStages().stream()
                .filter(item -> Objects.equals(item.getFase(), processo.getFaseAtual().name()))
                .findFirst()
                .orElse(null);
        if (stage == null || stage.getWork() == null) {
            return;
        }
        for (WorkTemplate template : stage.getWork()) {
            saveTemplateIfAbsent(processo, template);
        }
    }

    private void saveTemplateIfAbsent(Processo processo, WorkTemplate template) {
        if (template == null || template.getCode() == null || template.getCode().isBlank()) {
            return;
        }
        if (exists(processo.getId(), template.getCode())) {
            return;
        }
        Instant dueAt = template.getSlaDays() != null && template.getSlaDays() > 0
                ? timeService.nowUtc().plus(template.getSlaDays(), ChronoUnit.DAYS)
                : null;
        WorkItem workItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(template.getCode().trim())
                .type(parseEnum(WorkItemType.class, template.getType(), WorkItemType.OUTRO))
                .titulo(safe(template.getTitle()))
                .descricao(safe(template.getDescription()))
                .assignedRole(parseEnum(TipoUsuario.class, template.getActorRole(), null))
                .prioridade(normalizePriority(template.getPriority() != null ? template.getPriority() : 3))
                .blocking(Boolean.TRUE.equals(template.getBlocking()))
                .dueAt(dueAt)
                .uf(processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : null)
                .comarca(processo.getJurisdicao() != null ? processo.getJurisdicao().getCidade() : null)
                .comarcaEntidade(processo.getJurisdicao() != null ? processo.getJurisdicao().getComarcaEntidade() : null)
                .baseLegal(template.getLegalBases() == null || template.getLegalBases().isEmpty() ? null : String.join("\n", template.getLegalBases()))
                .build();
        workItemRepository.save(workItem);
    }

    private void saveSpecIfAbsent(Processo processo, WorkItemSpec spec) {
        if (spec == null || spec.code() == null || spec.code().isBlank()) {
            return;
        }
        if (exists(processo.getId(), spec.code())) {
            return;
        }
        WorkItem workItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(spec.fase())
                .templateCode(spec.code().trim())
                .type(spec.type())
                .titulo(safe(spec.title()))
                .descricao(safe(spec.description()))
                .assignedRole(spec.actorRole())
                .prioridade(normalizePriority(spec.required() ? 2 : 3))
                .blocking(spec.blocking())
                .dueAt(spec.deadline())
                .uf(processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : null)
                .comarca(processo.getJurisdicao() != null ? processo.getJurisdicao().getCidade() : null)
                .comarcaEntidade(processo.getJurisdicao() != null ? processo.getJurisdicao().getComarcaEntidade() : null)
                .baseLegal(spec.legalBases() == null || spec.legalBases().isEmpty() ? null : String.join("\n", spec.legalBases()))
                .build();
        workItemRepository.save(workItem);
    }

    private boolean exists(Long processoId, String templateCode) {
        return workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, templateCode.trim(), WorkItemStatus.CANCELADO).isPresent();
    }

    private Set<FaseProcessual> allowedNext(Processo processo) {
        RitoProcessual rito = resolveEffectiveRito(processo);
        if (rito == null) {
            return Set.of();
        }
        Optional<RitoDefinition> definition = ritoPackService.get(rito);
        if (definition.isEmpty() || definition.get().getStages() == null) {
            return Set.of();
        }
        RitoStage stage = definition.get().getStages().stream()
                .filter(item -> Objects.equals(item.getFase(), processo.getFaseAtual().name()))
                .findFirst()
                .orElse(null);
        if (stage == null || stage.getAllowedNext() == null || stage.getAllowedNext().isEmpty()) {
            return Set.of();
        }
        Set<FaseProcessual> out = new LinkedHashSet<>();
        for (String raw : stage.getAllowedNext()) {
            FaseProcessual value = parseEnum(FaseProcessual.class, raw, null);
            if (value != null) {
                out.add(value);
            }
        }
        return out;
    }

    private RitoProcessual resolveEffectiveRito(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (processo.getRito() != null) {
            return processo.getRito();
        }
        CanonicalContext canonical = resolveCanonicalContext(processo);
        if (canonical.rito() != null) {
            return canonical.rito();
        }
        if (processo.getClasseProcessual() != null || processo.getRamoDireito() != null) {
            return RitoProcessual.fromString(firstNonBlank(processo.getClasseProcessual(), processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null));
        }
        return null;
    }

    private CanonicalContext resolveCanonicalContext(Processo processo) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
        payload.put("classeTpu", processo.getClasseProcessual());
        payload.put("classe", processo.getClasseProcessual());
        payload.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        payload.put("tipoJustica", processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null);
        payload.put("materia", processo.getAssunto());
        if (processo.getJurisdicao() != null) {
            payload.put("uf", processo.getJurisdicao().getUf());
        }
        return proceduralCanonicalResolver.resolve(payload);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? null : value.trim();
    }

    private Integer normalizePriority(Integer value) {
        if (value == null) {
            return 3;
        }
        if (value < 1) {
            return 1;
        }
        if (value > 5) {
            return 5;
        }
        return value;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String raw, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private WorkItemDto toDto(WorkItem item) {
        return WorkItemDto.builder()
                .id(item.getId())
                .processoId(item.getProcesso() != null ? item.getProcesso().getId() : null)
                .processoNumero(item.getProcesso() != null ? item.getProcesso().getNumeroUnificado() : null)
                .faseOrigem(item.getFaseOrigem())
                .type(item.getType())
                .titulo(item.getTitulo())
                .descricao(item.getDescricao())
                .assignedRole(item.getAssignedRole())
                .assignedUserId(item.getAssignedUser() != null ? item.getAssignedUser().getId() : null)
                .status(item.getStatus())
                .prioridade(item.getPrioridade())
                .blocking(item.isBlocking())
                .dueAt(item.getDueAt())
                .uf(item.getUf())
                .comarca(item.getComarca())
                .build();
    }
}
