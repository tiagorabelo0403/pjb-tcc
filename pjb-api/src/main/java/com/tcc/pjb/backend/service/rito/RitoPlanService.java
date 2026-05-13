package com.tcc.pjb.backend.service.rito;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.intelligence.RitoPlanResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.rito.diagnostics.RitoPackStatus;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.util.Locale;

@Service
public class RitoPlanService {

    private final RitoPackService packService;
    private final RitoPackStatus status;
    private final ProceduralCatalogService proceduralCatalogService;

    public RitoPlanService(RitoPackService packService, RitoPackStatus status, ProceduralCatalogService proceduralCatalogService) {
        this.packService = packService;
        this.status = status;
        this.proceduralCatalogService = proceduralCatalogService;
    }

    public RitoPlanResponse plan(String ritoRaw) {
        RitoProcessual rito = proceduralCatalogService.resolveRito(ritoRaw, null, null);

        Optional<RitoDefinition> defOpt = packService.get(rito);
        List<RitoPlanResponse.RitoStageDto> stages = new ArrayList<>();

        if (defOpt.isPresent() && defOpt.get().getStages() != null) {
            for (RitoStage s : defOpt.get().getStages()) {
                if (s == null) continue;
                List<RitoPlanResponse.WorkDto> work = new ArrayList<>();
                if (s.getWork() != null) {
                    for (WorkTemplate wt : s.getWork()) {
                        if (wt == null) continue;
                        work.add(new RitoPlanResponse.WorkDto(
                                wt.getCode(),
                                wt.getType(),
                                wt.getTitle(),
                                wt.getDescription(),
                                wt.getActorRole(),
                                wt.getPriority(),
                                wt.getSlaDays(),
                                wt.getBlocking(),
                                wt.getLegalBases() == null ? List.of() : wt.getLegalBases()
                        ));
                    }
                }
                stages.add(new RitoPlanResponse.RitoStageDto(
                        safeUpper(s.getFase()),
                        s.getAllowedNext() == null ? List.of() : s.getAllowedNext(),
                        Collections.unmodifiableList(work)
                ));
            }
        }

        return new RitoPlanResponse(
                UUID.randomUUID().toString(),
                Instant.now(),
                rito.name(),
                status.getVersion(),
                status.getChecksum(),
                status.isLoaded(),
                status.getIssues(),
                Collections.unmodifiableList(stages)
        );
    }

    private static String safeUpper(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isBlank() ? null : s.toUpperCase(Locale.ROOT);
    }
}
