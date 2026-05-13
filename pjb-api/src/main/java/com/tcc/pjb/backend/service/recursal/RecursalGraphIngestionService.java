package com.tcc.pjb.backend.service.recursal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventStore;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventType;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.*;
import com.tcc.pjb.backend.core.kernel.recursal.template.RecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.RecursalTemplateResolver;
import com.tcc.pjb.backend.model.entity.casefile.CaseEdge;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.CaseEdgeRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.service.casefile.CaseFileResolution;
import com.tcc.pjb.backend.service.casefile.CaseFileResolverService;

@Service
public class RecursalGraphIngestionService {

    private final CaseFileResolverService caseFileResolverService;
    private final CaseProceedingRepository proceedingRepository;
    private final CaseEdgeRepository edgeRepository;
    private final RecursalTemplateResolver templateResolver;
    private final CaseFileEventStore eventStore;

    public RecursalGraphIngestionService(CaseFileResolverService caseFileResolverService,
                                        CaseProceedingRepository proceedingRepository,
                                        CaseEdgeRepository edgeRepository,
                                        RecursalTemplateResolver templateResolver,
                                        CaseFileEventStore eventStore) {
        this.caseFileResolverService = caseFileResolverService;
        this.proceedingRepository = proceedingRepository;
        this.edgeRepository = edgeRepository;
        this.templateResolver = templateResolver;
        this.eventStore = eventStore;
    }

    
    @Transactional
    public RecursalPlan ingest(Long processoId, CanonicalFact fact) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        Objects.requireNonNull(fact, "fact é obrigatório");

        CaseFileResolution resolution = caseFileResolverService.resolveForProcesso(processoId, fact.sourceSystem());
        Long caseFileId = resolution.caseFile().getId();

        eventStore.append(caseFileId, CaseFileEventType.FACT_INGESTED, new FactIngestedPayload(fact.dedupKey(), fact.type().name()));

        GraphSnapshot snapshot = buildSnapshot(caseFileId, resolution.anchorProceeding().getProceedingKey());
        ProceduralContext ctx = resolution.context();

        RecursalTemplate template = templateResolver.resolve(ctx);
        RecursalPlan plan = template.plan(fact, snapshot, ctx);

        applyPlan(caseFileId, plan);

        return plan;
    }

    @Transactional(readOnly = true)
    public GraphSnapshot readGraph(Long processoId) {
        CaseFileResolution resolution = caseFileResolverService.resolveForProcesso(processoId, null);
        return buildSnapshot(resolution.caseFile().getId(), resolution.anchorProceeding().getProceedingKey());
    }

    private GraphSnapshot buildSnapshot(Long caseFileId, String anchorProceedingKey) {
        List<CaseProceeding> nodes = proceedingRepository.findAllByCaseFileId(caseFileId);
        List<ProceedingView> pv = new ArrayList<>(nodes.size());
        for (CaseProceeding n : nodes) {
            pv.add(new ProceedingView(
                    n.getProceedingKey(),
                    n.isShadow(),
                    n.getStatus(),
                    n.getInstanceLevel(),
                    n.getCourt(),
                    n.getNumeroUnificado(),
                    n.getLinkedProcessoId(),
                    n.getSecrecy(),
                    n.getSourceSystem()
            ));
        }

        List<CaseEdge> edges = edgeRepository.findAllByCaseFileId(caseFileId);
        List<EdgeView> ev = new ArrayList<>(edges.size());
        for (CaseEdge e : edges) {
            ev.add(new EdgeView(
                    e.getFromProceedingKey(),
                    e.getToProceedingKey(),
                    e.getRelationType(),
                    e.getAppealType()
            ));
        }

        return new GraphSnapshot(caseFileId, anchorProceedingKey, pv, ev);
    }

    private void applyPlan(Long caseFileId, RecursalPlan plan) {
        if (plan == null) return;

        for (ProceedingUpsert p : plan.proceedings()) {
            upsertProceeding(caseFileId, p);
            eventStore.append(caseFileId, CaseFileEventType.PROCEEDING_UPSERTED, new ProceedingUpsertedPayload(p.proceedingKey(), p.status().name()));
        }

        for (EdgeUpsert e : plan.edges()) {
            upsertEdge(caseFileId, e);
            eventStore.append(caseFileId, CaseFileEventType.EDGE_UPSERTED,
                    new EdgeUpsertedPayload(e.fromProceedingKey(), e.toProceedingKey(), e.relationType().name(), e.appealType().name()));
        }

        for (SyncDirective d : plan.sync()) {
            eventStore.append(caseFileId, CaseFileEventType.SYNC_DIRECTIVE_EMITTED,
                    new SyncDirectivePayload(d.system().name(), d.proceedingKey(), d.targetInstance().name(), d.targetCourt(), d.priority()));
        }

        for (WorkItemDirective w : plan.workItems()) {
            eventStore.append(caseFileId, CaseFileEventType.WORK_ITEM_EMITTED,
                    new WorkItemPayload(w.queue(), w.title(), w.description(), w.dueDate() != null ? w.dueDate().toString() : null));
        }
    }

    private void upsertProceeding(Long caseFileId, ProceedingUpsert upsert) {
        CaseProceeding current = proceedingRepository.findByProceedingKey(upsert.proceedingKey()).orElse(null);
        if (current == null) {
            CaseProceeding created = CaseProceeding.builder()
                    .caseFileId(caseFileId)
                    .proceedingKey(upsert.proceedingKey())
                    .shadow(upsert.shadow())
                    .status(upsert.status())
                    .instanceLevel(upsert.instanceLevel())
                    .court(upsert.court())
                    .numeroUnificado(upsert.numeroUnificado())
                    .linkedProcessoId(upsert.linkedProcessoId())
                    .secrecy(upsert.secrecy())
                    .sourceSystem(upsert.sourceSystem())
                    .build();
            try {
                proceedingRepository.save(created);
                return;
            } catch (DataIntegrityViolationException race) {
                current = proceedingRepository.findByProceedingKey(upsert.proceedingKey()).orElse(null);
                if (current == null) throw race;
            }
        }

        boolean dirty = false;

        
        if (!upsert.shadow() && current.isShadow()) {
            current.setShadow(false);
            dirty = true;
        }

        if (current.getStatus() == null || statusRank(upsert.status()) > statusRank(current.getStatus())) {
            current.setStatus(upsert.status());
            dirty = true;
        }

        if (current.getInstanceLevel() == null) {
            current.setInstanceLevel(upsert.instanceLevel());
            dirty = true;
        }

        if (isBlank(current.getCourt()) && !isBlank(upsert.court())) {
            current.setCourt(upsert.court());
            dirty = true;
        }

        if (isBlank(current.getNumeroUnificado()) && !isBlank(upsert.numeroUnificado())) {
            current.setNumeroUnificado(upsert.numeroUnificado());
            dirty = true;
        }

        if (current.getLinkedProcessoId() == null && upsert.linkedProcessoId() != null) {
            current.setLinkedProcessoId(upsert.linkedProcessoId());
            dirty = true;
        }

        NivelSigilo nextSigilo = maxSigilo(current.getSecrecy(), upsert.secrecy());
        if (nextSigilo != current.getSecrecy()) {
            current.setSecrecy(nextSigilo);
            dirty = true;
        }

        if (current.getSourceSystem() == null) {
            current.setSourceSystem(upsert.sourceSystem());
            dirty = true;
        }

        if (dirty) {
            proceedingRepository.save(current);
        }
    }

    private void upsertEdge(Long caseFileId, EdgeUpsert upsert) {
        CaseEdge edge = CaseEdge.builder()
                .caseFileId(caseFileId)
                .fromProceedingKey(upsert.fromProceedingKey())
                .toProceedingKey(upsert.toProceedingKey())
                .relationType(upsert.relationType())
                .appealType(upsert.appealType())
                .build();
        try {
            edgeRepository.save(edge);
        } catch (DataIntegrityViolationException ignored) {
            
        }
    }

    private static int statusRank(CaseProceedingStatus s) {
        if (s == null) return 0;
        return switch (s) {
            case PREDICTED -> 10;
            case ACTIVE -> 30;
            case RECONCILED -> 40;
            case MERGED -> 45;
            case CLOSED -> 50;
        };
    }

    private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
        if (a == null) return b == null ? NivelSigilo.PUBLICO : b;
        if (b == null) return a;
        return a.getNivel() >= b.getNivel() ? a : b;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    
    private record FactIngestedPayload(String dedupKey, String factType) {}

    private record ProceedingUpsertedPayload(String proceedingKey, String status) {}

    private record EdgeUpsertedPayload(String fromKey, String toKey, String relationType, String appealType) {}

    private record SyncDirectivePayload(String system, String proceedingKey, String targetInstance, String targetCourt, int priority) {}

    private record WorkItemPayload(String queue, String title, String description, String dueDate) {}
}
