package com.tcc.pjb.backend.service.recursal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.plan.EdgeView;
import com.tcc.pjb.backend.core.kernel.recursal.plan.GraphSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.plan.ProceedingView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;

@Service
public class RecursalGraphQueryService {

    private final RecursalGraphIngestionService ingestionService;

    public RecursalGraphQueryService(RecursalGraphIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public RecursalGraphResponse readGraph(Long processoId) {
        GraphSnapshot snap = ingestionService.readGraph(processoId);

        List<RecursalGraphResponse.NodeDto> nodes = new ArrayList<>(snap.proceedings().size());
        int predicted = 0, active = 0, reconciled = 0;
        InstanceLevel maxInstance = InstanceLevel.FIRST_INSTANCE;

        
        List<ProceedingView> ordered = snap.proceedings().stream()
                .sorted(Comparator
                        .comparing((ProceedingView p) -> p.instanceLevel() == null ? InstanceLevel.FIRST_INSTANCE : p.instanceLevel())
                        .thenComparing(ProceedingView::shadow)
                        .thenComparing(ProceedingView::proceedingKey))
                .toList();

        for (ProceedingView p : ordered) {
            String status = p.status() != null ? p.status().name() : "UNKNOWN";
            switch (status) {
                case "PREDICTED" -> predicted++;
                case "ACTIVE" -> active++;
                case "RECONCILED" -> reconciled++;
            }

            InstanceLevel lvl = p.instanceLevel() == null ? InstanceLevel.FIRST_INSTANCE : p.instanceLevel();
            if (lvl.ordinal() > maxInstance.ordinal()) maxInstance = lvl;

            String label = buildDisplayLabel(p);

            nodes.add(new RecursalGraphResponse.NodeDto(
                    p.proceedingKey(),
                    p.shadow(),
                    status,
                    p.instanceLevel(),
                    p.court(),
                    p.numeroUnificado(),
                    p.linkedProcessoId(),
                    p.secrecy() != null ? p.secrecy().name() : null,
                    p.sourceSystem() != null ? p.sourceSystem().name() : null,
                    label
            ));
        }

        List<RecursalGraphResponse.EdgeDto> edges = new ArrayList<>(snap.edges().size());
        for (EdgeView e : snap.edges()) {
            edges.add(new RecursalGraphResponse.EdgeDto(
                    e.fromProceedingKey(),
                    e.toProceedingKey(),
                    e.relationType() != null ? e.relationType().name() : null,
                    e.appealType() != null ? e.appealType().name() : null
            ));
        }

        RecursalGraphResponse.SummaryDto summary = new RecursalGraphResponse.SummaryDto(
                nodes.size(),
                edges.size(),
                predicted,
                active,
                reconciled,
                maxInstance
        );

        return new RecursalGraphResponse(snap.caseFileId(), snap.anchorProceedingKey(), summary, nodes, edges);
    }

    private static String buildDisplayLabel(ProceedingView p) {
        String num = p.numeroUnificado();
        if (num != null && !num.isBlank()) return num;
        String court = p.court();
        if (court != null && !court.isBlank()) return court + " • " + p.proceedingKey();
        return p.proceedingKey();
    }
}
