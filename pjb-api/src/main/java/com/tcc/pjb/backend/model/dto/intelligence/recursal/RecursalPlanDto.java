package com.tcc.pjb.backend.model.dto.intelligence.recursal;

import java.time.LocalDate;
import java.util.List;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;

public record RecursalPlanDto(
        List<ProceedingUpsertDto> proceedings,
        List<EdgeUpsertDto> edges,
        List<SyncDirectiveDto> sync,
        List<WorkItemDirectiveDto> workItems,
        List<String> notes
) {
    public RecursalPlanDto {
        proceedings = proceedings == null ? List.of() : List.copyOf(proceedings);
        edges = edges == null ? List.of() : List.copyOf(edges);
        sync = sync == null ? List.of() : List.copyOf(sync);
        workItems = workItems == null ? List.of() : List.copyOf(workItems);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public record ProceedingUpsertDto(
            String proceedingKey,
            boolean shadow,
            String status,
            InstanceLevel instanceLevel,
            String court,
            String numeroUnificado,
            Long linkedProcessoId,
            String secrecy,
            String sourceSystem
    ) {}

    public record EdgeUpsertDto(
            String fromProceedingKey,
            String toProceedingKey,
            String relationType,
            String appealType
    ) {}

    public record SyncDirectiveDto(
            String system,
            String proceedingKey,
            String numeroOrHint,
            InstanceLevel targetInstance,
            String targetCourt,
            int priority
    ) {}

    public record WorkItemDirectiveDto(
            String queue,
            String title,
            String description,
            LocalDate dueDate
    ) {}
}
