package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralReviewSignalCollectorOrchestrationTest {

    @Test
    void mustOrchestrateReasonsAndPolicySignals() {
        NationalProceduralReviewReasonCollector reasonCollector = mock(NationalProceduralReviewReasonCollector.class);
        NationalProceduralReviewPolicySignalResolver policySignalResolver = mock(NationalProceduralReviewPolicySignalResolver.class);
        NationalProceduralReviewSignalCollector collector = new NationalProceduralReviewSignalCollector(reasonCollector, policySignalResolver);
        NationalProceduralReviewSynthesisContext context = NationalProceduralReviewSignalCollectorTest.baseContext(Map.of("classe", "teste"));
        when(reasonCollector.collect(context)).thenReturn(new NationalProceduralReviewDraft(List.of("r"), List.of("l"), List.of(), List.of(), List.of(), List.of("m")));
        when(policySignalResolver.collect(context)).thenReturn(new NationalProceduralReviewSignalSet(List.of("a"), List.of("c"), List.of("b")));

        NationalProceduralReviewDraft result = collector.collect(context);

        assertEquals(List.of("r"), result.reasons());
        assertEquals(List.of("a"), result.alerts());
        assertEquals(List.of("c"), result.reviewChecklist());
        assertEquals(List.of("b"), result.blockingIssues());
        verify(reasonCollector).collect(context);
        verify(policySignalResolver).collect(context);
    }
}
