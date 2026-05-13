package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingServicePhaseOrchestrationTest {

    @Test
    void mustDelegateCoreAnalysisAndFinalizationInSequence() {
        NationalProceduralRoutingPayloadFactory payloadFactory = Mockito.mock(NationalProceduralRoutingPayloadFactory.class);
        NationalProceduralRoutingCoreAnalyzer coreAnalyzer = Mockito.mock(NationalProceduralRoutingCoreAnalyzer.class);
        NationalProceduralRoutingFinalizationResolver finalizationResolver = Mockito.mock(NationalProceduralRoutingFinalizationResolver.class);
        NationalProceduralRoutingService service = new NationalProceduralRoutingService(payloadFactory, coreAnalyzer, finalizationResolver);

        Map<String, Object> input = Map.of("classe", "cobranca");
        LinkedHashMap<String, Object> copiedPayload = new LinkedHashMap<>(Map.of("classe", "cobranca", "valorCausa", 1000));
        ProceduralRoutingReport expected = new ProceduralRoutingReport(
                Instant.now(),
                "COBRANCA",
                "CIVEL",
                "COMUM",
                "COMUM",
                "ESTADUAL",
                "COMUM_ORDINARIO",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "MEDIA",
                "DOCUMENTAL",
                false,
                false,
                false,
                0.9d,
                "BAIXO",
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        when(payloadFactory.copyOf(input)).thenReturn(copiedPayload);
        when(coreAnalyzer.analyze(anyMap(), eq("context"))).thenReturn(null);
        when(finalizationResolver.finalize(null)).thenReturn(expected);

        ProceduralRoutingReport result = service.analyzeContext(input);

        assertSame(expected, result);
        verify(payloadFactory).copyOf(input);
        verify(coreAnalyzer).analyze(copiedPayload, "context");
        verify(finalizationResolver).finalize(null);
    }
}
