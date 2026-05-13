package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewPartyRequirementResolver {

    NationalProceduralReviewInputSlice assess(Map<String, Object> payload) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        LinkedHashSet<String> missingInputs = new LinkedHashSet<>();
        if (NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("parteAutoraNome")))
                && NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("parteAutoraCpf")))) {
            missingInputs.add("parteAutora");
        }
        if (NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("parteReuNome")))
                && NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("parteReuCpf")))) {
            missingInputs.add("parteRe");
        }
        return new NationalProceduralReviewInputSlice(List.copyOf(missingInputs), List.of());
    }
}
