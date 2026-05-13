package com.tcc.pjb.backend.service.intelligence;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.SafeMaps;
import com.tcc.pjb.backend.model.dto.intelligence.*;
import com.tcc.pjb.backend.service.material.MaterialPackService;
import com.tcc.pjb.backend.service.material.model.MaterialProfile;
import com.tcc.pjb.backend.service.rito.RitoPlanService;

@Service
public class CaseKitService {

    private final CaseTriageService triageService;
    private final RitoPlanService ritoPlanService;
    private final MaterialPackService materialPackService;

    public CaseKitService(CaseTriageService triageService,
                          RitoPlanService ritoPlanService,
                          MaterialPackService materialPackService) {
        this.triageService = triageService;
        this.ritoPlanService = ritoPlanService;
        this.materialPackService = materialPackService;
    }

    public CaseKitResponse build(CaseTriageRequest req) {
        CaseTriageResponse triage = triageService.triage(req);

        RitoPlanResponse ritoPlan = ritoPlanService.plan(triage.ritoSugerido() != null ? triage.ritoSugerido().name() : null);

        MaterialProfile profile = materialPackService.resolve(triage.ramoSugerido(), triage.ritoSugerido());
        CaseKitResponse.MaterialKitDto material = new CaseKitResponse.MaterialKitDto(
                profile.getRequiredDocuments(),
                profile.getProofChecklist(),
                profile.getLegalBases(),
                profile.getWarnings()
        );

        Map<String, Object> dbg = new LinkedHashMap<>();
        dbg.put("triageId", triage.triageId());
        dbg.put("materialBy", SafeMaps.of(
                "ramo", triage.ramoSugerido() != null ? triage.ramoSugerido().name() : null,
                "rito", triage.ritoSugerido() != null ? triage.ritoSugerido().name() : null
        ));
        dbg.put("ritoPackLoaded", ritoPlan.packLoaded());

        return new CaseKitResponse(
                UUID.randomUUID().toString(),
                Instant.now(),
                triage,
                ritoPlan,
                material,
                dbg
        );
    }
}
