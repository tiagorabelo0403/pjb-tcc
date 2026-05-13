package com.tcc.pjb.backend.core.frontend.virtualcounter;

import java.util.List;
import java.util.Objects;

public record PjbVirtualCounterRoutingDecision(
        PjbVirtualCounterIntent intent,
        String destination,
        boolean humanAssistanceRecommended,
        List<String> nextQuestions
) {
    public PjbVirtualCounterRoutingDecision {
        intent = intent == null ? PjbVirtualCounterIntent.UNKNOWN : intent;
        destination = Objects.toString(destination, "public.portal").trim();
        nextQuestions = nextQuestions == null ? List.of() : List.copyOf(nextQuestions);
    }
}
