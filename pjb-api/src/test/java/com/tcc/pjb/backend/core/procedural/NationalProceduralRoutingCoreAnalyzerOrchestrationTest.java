package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingCoreAnalyzerOrchestrationTest {

    @Test
    void mustOrchestrateFoundationAndClassificationSubphases() {
        NationalProceduralRoutingFoundationAnalyzer foundationAnalyzer = Mockito.mock(NationalProceduralRoutingFoundationAnalyzer.class);
        NationalProceduralRoutingClassificationAnalyzer classificationAnalyzer = Mockito.mock(NationalProceduralRoutingClassificationAnalyzer.class);
        NationalProceduralRoutingCoreAnalyzer analyzer = new NationalProceduralRoutingCoreAnalyzer(foundationAnalyzer, classificationAnalyzer);

        NationalProceduralRoutingCoreResolution expected = NationalProceduralRoutingTestFixtures.sampleResolution();
        NationalProceduralRoutingFoundationResolution foundation = new NationalProceduralRoutingFoundationResolution(
                expected.payload(),
                expected.corpus(),
                expected.sourceLabel(),
                expected.partyProfile(),
                expected.selectedRito(),
                expected.canonical(),
                expected.competence(),
                expected.actionProfile(),
                expected.probatoryProfile(),
                expected.teto(),
                expected.juizadoDecision()
        );

        when(foundationAnalyzer.analyze(Map.of("classe", "indenizacao"), "context")).thenReturn(foundation);
        when(classificationAnalyzer.analyze(foundation)).thenReturn(expected);

        NationalProceduralRoutingCoreResolution result = analyzer.analyze(Map.of("classe", "indenizacao"), "context");

        assertSame(expected, result);
        verify(foundationAnalyzer).analyze(Map.of("classe", "indenizacao"), "context");
        verify(classificationAnalyzer).analyze(foundation);
    }
}
