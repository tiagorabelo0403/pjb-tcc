package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewLocationRequirementResolver {

    NationalProceduralReviewInputSlice assess(String cidadeSugerida, String ufSugerida) {
        LinkedHashSet<String> missingInputs = new LinkedHashSet<>();
        if (NationalProceduralRoutingSupport.isBlank(cidadeSugerida)) {
            missingInputs.add("cidadeComarcaBase");
        }
        if (NationalProceduralRoutingSupport.isBlank(ufSugerida)) {
            missingInputs.add("ufBase");
        }
        return new NationalProceduralReviewInputSlice(List.copyOf(missingInputs), List.of());
    }
}
