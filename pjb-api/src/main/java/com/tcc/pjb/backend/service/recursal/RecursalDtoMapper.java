package com.tcc.pjb.backend.service.recursal;

import java.util.ArrayList;
import java.util.List;
import com.tcc.pjb.backend.core.kernel.recursal.plan.*;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalPlanDto;

final class RecursalDtoMapper {

    private RecursalDtoMapper() {}

    static RecursalPlanDto toPlanDto(RecursalPlan plan) {
        if (plan == null) return new RecursalPlanDto(List.of(), List.of(), List.of(), List.of(), List.of());

        List<RecursalPlanDto.ProceedingUpsertDto> proceedings = new ArrayList<>();
        for (ProceedingUpsert p : plan.proceedings()) {
            proceedings.add(new RecursalPlanDto.ProceedingUpsertDto(
                    p.proceedingKey(),
                    p.shadow(),
                    p.status() != null ? p.status().name() : null,
                    p.instanceLevel(),
                    p.court(),
                    p.numeroUnificado(),
                    p.linkedProcessoId(),
                    p.secrecy() != null ? p.secrecy().name() : null,
                    p.sourceSystem() != null ? p.sourceSystem().name() : null
            ));
        }

        List<RecursalPlanDto.EdgeUpsertDto> edges = new ArrayList<>();
        for (EdgeUpsert e : plan.edges()) {
            edges.add(new RecursalPlanDto.EdgeUpsertDto(
                    e.fromProceedingKey(),
                    e.toProceedingKey(),
                    e.relationType() != null ? e.relationType().name() : null,
                    e.appealType() != null ? e.appealType().name() : null
            ));
        }

        List<RecursalPlanDto.SyncDirectiveDto> sync = new ArrayList<>();
        for (SyncDirective s : plan.sync()) {
            sync.add(new RecursalPlanDto.SyncDirectiveDto(
                    s.system() != null ? s.system().name() : null,
                    s.proceedingKey(),
                    s.numeroOrHint(),
                    s.targetInstance(),
                    s.targetCourt(),
                    s.priority()
            ));
        }

        List<RecursalPlanDto.WorkItemDirectiveDto> work = new ArrayList<>();
        for (WorkItemDirective w : plan.workItems()) {
            work.add(new RecursalPlanDto.WorkItemDirectiveDto(
                    w.queue(),
                    w.title(),
                    w.description(),
                    w.dueDate()
            ));
        }

        return new RecursalPlanDto(proceedings, edges, sync, work, plan.notes());
    }
}
