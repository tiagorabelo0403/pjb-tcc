package com.tcc.pjb.backend.service.workitem;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine;
import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine.AssignmentContext;
import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine.AssignmentResult;
import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine.WorkItemSpec;
import com.tcc.pjb.backend.core.util.SafeMaps;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationRequest;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;

@Service
public class ProcessWorkItemAutomationService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final RitoPackService ritoPackService;
    private final WorkItemMapper mapper;
    private final PjbTimeService timeService;
    private final ActorAssignmentEngine actorAssignmentEngine;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;

    public ProcessWorkItemAutomationService(ProcessoRepository processoRepository,
                                            WorkItemRepository workItemRepository,
                                            RitoPackService ritoPackService,
                                            WorkItemMapper mapper,
                                            PjbTimeService timeService,
                                            ActorAssignmentEngine actorAssignmentEngine,
                                            ProcessoRitoSnapshotService processoRitoSnapshotService) {
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.ritoPackService = ritoPackService;
        this.mapper = mapper;
        this.timeService = timeService;
        this.actorAssignmentEngine = actorAssignmentEngine;
        this.processoRitoSnapshotService = processoRitoSnapshotService;
    }

    @Transactional
    public WorkItemGenerationResponse generate(WorkItemGenerationRequest req) {
        String requestId = UUID.randomUUID().toString();
        Instant now = timeService.nowUtc();

        Processo processo = processoRepository.findById(req.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", req.processoId()));

        ProcessoRitoSnapshotService.ProcessoRitoSnapshot ritoSnapshot = processoRitoSnapshotService.resolve(processo, null);
        var ritoEfetivo = ritoSnapshot != null && ritoSnapshot.rito() != null ? ritoSnapshot.rito() : processo.getRito();
        if (ritoEfetivo == null) {
            return new WorkItemGenerationResponse(requestId, now, processo.getId(), 0, 0, List.of(),
                    SafeMaps.of(
                            "status", "NO_RITO",
                            "resolutionStatus", ritoSnapshot != null ? ritoSnapshot.status() : null,
                            "blocking", ritoSnapshot != null && ritoSnapshot.blocking(),
                            "message", "Processo não possui rito definido"
                    ));
        }

        Optional<RitoDefinition> defOpt = ritoPackService.get(ritoEfetivo);
        if (defOpt.isEmpty()) {
            return new WorkItemGenerationResponse(requestId, now, processo.getId(), 0, 0, List.of(),
                    SafeMaps.of(
                            "status", "NO_PACK",
                            "rito", ritoEfetivo.name(),
                            "resolutionStatus", ritoSnapshot != null ? ritoSnapshot.status() : null,
                            "blocking", ritoSnapshot != null && ritoSnapshot.blocking()
                    ));
        }

        FaseProcessual faseAlvo = resolveFase(req.fase(), processo.getFaseAtual());
        RitoDefinition def = defOpt.get();
        RitoStage stage = pickStage(def, faseAlvo);
        if (stage == null || stage.getWork() == null || stage.getWork().isEmpty()) {
            return new WorkItemGenerationResponse(requestId, now, processo.getId(), 0, 0, List.of(),
                    SafeMaps.of("status", "NO_TEMPLATES", "fase", (faseAlvo != null ? faseAlvo.name() : null)));
        }

        if (!req.force()) {
            boolean hasOpenInPhase = workItemRepository.findAllByProcesso(processo.getId()).stream()
                    .anyMatch(w -> w != null
                            && w.getFaseOrigem() == faseAlvo
                            && w.getStatus() != WorkItemStatus.CONCLUIDO
                            && w.getStatus() != WorkItemStatus.CANCELADO);
            if (hasOpenInPhase) {
                return new WorkItemGenerationResponse(requestId, now, processo.getId(), 0, stage.getWork().size(), List.of(),
                        SafeMaps.of("status", "ALREADY_OPEN", "fase", faseAlvo.name(), "message", "Há workitems em aberto nesta fase; use force=true para gerar faltantes"));
            }
        }

        int created = 0;
        int skipped = 0;
        List<WorkItemDto> createdItems = new ArrayList<>();
        Map<String, Object> dbg = new LinkedHashMap<>();
        dbg.put("rito", ritoEfetivo.name());
        dbg.put("ritoTitle", ritoSnapshot != null ? ritoSnapshot.ritoTitle() : null);
        dbg.put("ritoStatus", ritoSnapshot != null ? ritoSnapshot.status() : null);
        dbg.put("ritoNeedsReview", ritoSnapshot != null && ritoSnapshot.needsReview());
        dbg.put("ritoBlocking", ritoSnapshot != null && ritoSnapshot.blocking());
        dbg.put("ritoConfidence", ritoSnapshot != null ? ritoSnapshot.confidence() : null);
        dbg.put("fase", faseAlvo != null ? faseAlvo.name() : null);
        dbg.put("stageFase", stage.getFase());
        dbg.put("templates", stage.getWork().size());

        LinkedHashSet<String> reservedCodes = new LinkedHashSet<>();

        for (WorkTemplate t : stage.getWork()) {
            if (t == null || t.getCode() == null || t.getCode().isBlank()) {
                skipped++;
                continue;
            }

            
            String normalizedCode = normalizeCode(t.getCode());
            if (!reservedCodes.add(normalizedCode)) {
                skipped++;
                continue;
            }
            var exists = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(
                    processo.getId(), t.getCode(), WorkItemStatus.CANCELADO);
            if (exists.isPresent()) {
                skipped++;
                continue;
            }

            WorkItem wi = new WorkItem();
            wi.setProcesso(processo);
            wi.setStatus(WorkItemStatus.PENDENTE);
            wi.setTemplateCode(t.getCode());
            wi.setFaseOrigem(faseAlvo);
            wi.setTitulo(buildTitle(t));
            wi.setDescricao(buildDescription(t));
            wi.setType(resolveType(t.getType()));

            TipoUsuario role = TipoUsuario.fromPerfil(t.getActorRole());
            wi.setAssignedRole(role);

            if (t.getSlaDays() != null && t.getSlaDays() > 0) {
                wi.setDueAt(now.plus(t.getSlaDays(), ChronoUnit.DAYS));
            }

            wi.setBaseLegal(safeJoin(t.getLegalBases()));
            wi = workItemRepository.save(wi);
            created++;
            createdItems.add(mapper.toDto(wi));
        }

        AssignmentResult assignment = actorAssignmentEngine.assign(new AssignmentContext(
                processo.getId(),
                ritoEfetivo,
                faseAlvo,
                1,
                ritoEfetivo.isMilitar() || ritoEfetivo.isEleitoral() || ritoEfetivo.name().startsWith("PENAL"),
                false,
                false,
                false,
                req.force(),
                false,
                List.of(),
                Map.of()
        ));
        for (WorkItemSpec spec : assignment.workItems()) {
            if (spec == null || spec.code() == null || spec.code().isBlank()) {
                continue;
            }
            String normalizedCode = normalizeCode(spec.code());
            if (!reservedCodes.add(normalizedCode)) {
                skipped++;
                continue;
            }
            var exists = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(
                    processo.getId(), spec.code(), WorkItemStatus.CANCELADO);
            if (exists.isPresent()) {
                skipped++;
                continue;
            }
            WorkItem wi = new WorkItem();
            wi.setProcesso(processo);
            wi.setStatus(WorkItemStatus.PENDENTE);
            wi.setTemplateCode(spec.code());
            wi.setFaseOrigem(spec.fase());
            wi.setTitulo(spec.title());
            wi.setDescricao(spec.description());
            wi.setType(spec.type());
            wi.setAssignedRole(spec.actorRole());
            wi.setBlocking(spec.blocking());
            wi.setPrioridade(spec.required() ? 2 : 3);
            wi.setDueAt(spec.deadline());
            wi.setBaseLegal(safeJoin(spec.legalBases()));
            wi = workItemRepository.save(wi);
            created++;
            createdItems.add(mapper.toDto(wi));
        }

        dbg.put("created", created);
        dbg.put("skipped", skipped);
        dbg.put("actorDiagnostics", assignment.diagnostics());

        return new WorkItemGenerationResponse(requestId, now, processo.getId(), created, skipped,
                List.copyOf(createdItems), dbg);
    }

    private static FaseProcessual resolveFase(String faseRaw, FaseProcessual current) {
        if (faseRaw != null && !faseRaw.isBlank()) {
            try {
                return FaseProcessual.valueOf(faseRaw.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
            }
        }
        return current != null ? current : FaseProcessual.CONHECIMENTO;
    }

    private static RitoStage pickStage(RitoDefinition def, FaseProcessual fase) {
        if (def == null || def.getStages() == null || def.getStages().isEmpty()) return null;
        String f = fase != null ? fase.name() : null;
        for (RitoStage s : def.getStages()) {
            if (s == null) continue;
            if (f != null && f.equalsIgnoreCase(s.getFase())) return s;
        }
        
        return def.getStages().get(0);
    }

    private static String buildTitle(WorkTemplate t) {
        String label = t.getTitle() != null && !t.getTitle().isBlank() ? t.getTitle().trim() : "Tarefa";
        if (t.getActorRole() != null && !t.getActorRole().isBlank()) {
            return label + " (" + t.getActorRole().trim() + ")";
        }
        return label;
    }

    private static String buildDescription(WorkTemplate t) {
        StringBuilder sb = new StringBuilder(256);
        if (t.getChecklist() != null && !t.getChecklist().isEmpty()) {
            sb.append("Checklist:\n");
            for (String c : t.getChecklist()) {
                if (c == null || c.isBlank()) continue;
                sb.append("- ").append(c.trim()).append('\n');
            }
        }
        if (t.getLegalBases() != null && !t.getLegalBases().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Base legal: ").append(safeJoin(t.getLegalBases()));
        }
        return sb.toString();
    }

    private static String safeJoin(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        LinkedHashSet<String> s = new LinkedHashSet<>();
        for (String v : list) {
            if (v == null) continue;
            String t = v.trim();
            if (!t.isBlank()) s.add(t);
        }
        if (s.isEmpty()) return null;
        return String.join(" | ", s);
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static WorkItemType resolveType(String raw) {
        if (raw == null || raw.isBlank()) return WorkItemType.OUTRO;
        String t = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return WorkItemType.valueOf(t);
        } catch (Exception ignored) {
            
            return switch (t) {
                case "DESPACHAR" -> WorkItemType.DESPACHO;
                case "DECIDIR" -> WorkItemType.DECISAO;
                case "AUDIENCIA_CONCILIACAO", "AUDIENCIA_INSTRUCAO" -> WorkItemType.AUDIENCIA;
                case "INTIMAR" -> WorkItemType.INTIMACAO;
                case "CITAR" -> WorkItemType.CITACAO;
                case "PERICIAR" -> WorkItemType.PERICIA;
                case "CALCULAR" -> WorkItemType.CALCULO;
                case "RECORRER" -> WorkItemType.RECURSO;
                default -> WorkItemType.OUTRO;
            };
        }
    }
}
