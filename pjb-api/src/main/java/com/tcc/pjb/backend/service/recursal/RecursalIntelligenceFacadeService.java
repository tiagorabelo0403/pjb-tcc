package com.tcc.pjb.backend.service.recursal;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalPlanDto;

@Service
public class RecursalIntelligenceFacadeService {

    private final RecursalGraphIngestionService ingestionService;
    private final RecursalGraphQueryService queryService;
    private final RecursalTimelineIntegrationService timelineIntegration;
    private final RecursalWorkItemMaterializerService workItemMaterializer;
    private final RecursalOutboxEmitterService outboxEmitter;
    private final RecursalEffectiveSecrecyService secrecyService;

    public RecursalIntelligenceFacadeService(RecursalGraphIngestionService ingestionService,
                                            RecursalGraphQueryService queryService,
                                            RecursalTimelineIntegrationService timelineIntegration,
                                            RecursalWorkItemMaterializerService workItemMaterializer,
                                            RecursalOutboxEmitterService outboxEmitter,
                                            RecursalEffectiveSecrecyService secrecyService) {
        this.ingestionService = ingestionService;
        this.queryService = queryService;
        this.timelineIntegration = timelineIntegration;
        this.workItemMaterializer = workItemMaterializer;
        this.outboxEmitter = outboxEmitter;
        this.secrecyService = secrecyService;
    }

    @Transactional
    public RecursalFactIngestResponse ingest(Long processoId, CanonicalFact fact) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        Objects.requireNonNull(fact, "fact é obrigatório");

        RecursalPlan plan = ingestionService.ingest(processoId, fact);
        Long movId = timelineIntegration.appendTimelineEntry(processoId, fact, plan);
        workItemMaterializer.materialize(processoId, fact, plan);
        RecursalGraphResponse graph = queryService.readGraph(processoId);
        outboxEmitter.emitGraphChanged(processoId, fact, plan, graph);
        RecursalPlanDto planDto = RecursalDtoMapper.toPlanDto(plan);
        String dedupKey = fact.dedupKey();
        if (dedupKey == null || dedupKey.isBlank()) {
            dedupKey = fact.factId().toString();
        }
        return new RecursalFactIngestResponse(
                "PJB_2_0_RECURSAL",
                fact.factId(),
                dedupKey,
                processoId,
                movId,
                planDto,
                graph
        );
    }

    @Transactional(readOnly = true)
    public RecursalGraphResponse graph(Long processoId) {
        return queryService.readGraph(processoId);
    }

    @Transactional(readOnly = true)
    public RecursalEffectiveSecrecyService.SecrecySnapshot graphSecrecy(Long processoId) {
        return secrecyService.effectiveSecrecySnapshot(processoId);
    }
}
